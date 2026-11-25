package com.violinassistant.app.tuner

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.ArrayDeque
import kotlin.math.abs

/**
 * Coordinates pitch smoothing, NOISY confusion gating, and history state matching
 * Soundcorset's TunerEngine + PitchDetector.
 */
class SoundcorsetTunerEngine(
    private val audioInput: SoundcorsetAudioInput = SoundcorsetAudioInput(),
    initialSampleRate: Int = 44100
) {
    companion object {
        private const val CONFUSION_TTL_MILLIS = 3000L
        private const val CONFUSION_DECAY = 0.95
        private const val MAX_ENVELOPE_BARS = 140
    }

    private val _state = MutableStateFlow(TunerUiState())
    val state: StateFlow<TunerUiState> = _state.asStateFlow()

    private var a4Hz: Float = SoundcorsetFrequencyPool.DEFAULT_A4_HZ
    private var useFlat: Boolean = false
    private var useSolfege: Boolean = false
    private var sampleRate: Int = initialSampleRate
    private var noteFrequencies = SoundcorsetFrequencyPool.buildNoteFrequencies(a4Hz)
    private var noteLags = SoundcorsetFrequencyPool.buildNoteLagTable(noteFrequencies, sampleRate)

    private val volumeTracker = FloatingMaxVolumeTracker()
    private val pitchQueue = ArrayDeque<Int>(4)
    private val pitchMajorQueue = ArrayDeque<Int>(8)
    private val historyQueue = ArrayDeque<PitchHistoryPoint>(SoundcorsetFrequencyPool.HISTORY_SIZE + 2)
    private val envelopeQueue = ArrayDeque<Int>(MAX_ENVELOPE_BARS + 2)

    private var latestPitchHz: Double = 0.0
    private var lastMajorHz: Double = 0.0
    private var latestPitchUpdateMs: Long = 0L
    private var confused: Boolean = false
    private var confusionLevel: Double = 0.0
    private var lastStableTimeMs: Long = 0L
    private var inTuneScore: Int = 0

    fun updateSettings(a4Hz: Float, useFlat: Boolean, useSolfege: Boolean) {
        synchronized(this) {
            this.a4Hz = a4Hz.coerceIn(415f, 465f)
            this.useFlat = useFlat
            this.useSolfege = useSolfege
            noteFrequencies = SoundcorsetFrequencyPool.buildNoteFrequencies(this.a4Hz)
            noteLags = SoundcorsetFrequencyPool.buildNoteLagTable(noteFrequencies, sampleRate)
            emitState(isLiveFrame = false, nowMs = System.currentTimeMillis())
        }
    }

    fun start() {
        audioInput.start(
            onStarted = { resolvedRate ->
                synchronized(this) {
                    sampleRate = resolvedRate
                    noteLags = SoundcorsetFrequencyPool.buildNoteLagTable(noteFrequencies, sampleRate)
                    resetSessionState()
                    emitState(isLiveFrame = false, nowMs = System.currentTimeMillis())
                }
            },
            onFrame = { samples ->
                processAudioFrame(samples, System.currentTimeMillis())
            }
        )
    }

    fun stop() {
        audioInput.stop()
        synchronized(this) {
            _state.value = _state.value.copy(isListening = false, isLive = false)
        }
    }

    internal fun processAudioFrame(samples: DoubleArray, nowMs: Long = System.currentTimeMillis()) {
        synchronized(this) {
            val volume = SoundcorsetDSP.getVolume(samples)
            volumeTracker.update(volume)
            val envAmp = ((volume / 100.0) * 32767.0).toInt().coerceIn(0, 32767)
            if (envelopeQueue.size >= MAX_ENVELOPE_BARS) envelopeQueue.removeFirst()
            envelopeQueue.addLast(envAmp)

            val prevUpdate = latestPitchUpdateMs
            if (volumeTracker.isAboveGate(volume)) {
                val freq = SoundcorsetDSP.detectPitchHz(samples, sampleRate, noteFrequencies, noteLags)
                if (freq > 0.0 && freq <= noteFrequencies.last()) {
                    latestPitchHz = if (latestPitchHz > 0.0 && abs(freq - latestPitchHz) < 0.05 * freq) {
                        0.75 * latestPitchHz + 0.25 * freq
                    } else {
                        freq
                    }
                    updateConfusionAndHistory(latestPitchHz, nowMs)
                    latestPitchUpdateMs = nowMs
                } else {
                    appendSilentHistory()
                }
            } else {
                confusionLevel *= CONFUSION_DECAY
                appendSilentHistory()
            }
            emitState(isLiveFrame = latestPitchUpdateMs != prevUpdate, nowMs = nowMs)
        }
    }

    private fun updateConfusionAndHistory(freq: Double, nowMs: Long) {
        val noteIdx = SoundcorsetFrequencyPool.closestNoteIndex(freq, noteFrequencies)
        if (pitchQueue.size >= 3) pitchQueue.removeFirst()
        pitchQueue.addLast(noteIdx)
        if (pitchMajorQueue.size >= 7) pitchMajorQueue.removeFirst()
        pitchMajorQueue.addLast(noteIdx)

        if (pitchMajorQueue.size == 7 && pitchMajorQueue.distinct().size == 1) {
            lastMajorHz = freq
        }
        val distinctRecent = pitchQueue.distinct().size
        if (distinctRecent > 2) confused = true else if (distinctRecent == 1) confused = false
        if (!confused) lastStableTimeMs = nowMs
        confusionLevel = CONFUSION_DECAY * confusionLevel +
            (1.0 - CONFUSION_DECAY) * if (confused) 1.0 else 0.0

        val effectiveFreq = resolveEffectiveFreq(freq, nowMs)
        val effIdx = SoundcorsetFrequencyPool.closestNoteIndex(effectiveFreq, noteFrequencies)
        val centsVal = SoundcorsetFrequencyPool.diffInCents(noteFrequencies[effIdx], effectiveFreq).toFloat()
        inTuneScore = if (abs(centsVal) < 10f) (inTuneScore + 1).coerceAtMost(15)
        else (if (inTuneScore >= 10) 5 else inTuneScore - 5).coerceAtLeast(0)
        if (historyQueue.size >= SoundcorsetFrequencyPool.HISTORY_SIZE) historyQueue.removeFirst()
        historyQueue.addLast(
            PitchHistoryPoint(
                centsOffset = centsVal,
                noteIndex = effIdx,
                noteShortLabel = SoundcorsetFrequencyPool.noteName(effIdx, useFlat, useSolfege, includeOctave = false)
            )
        )
    }

    private fun appendSilentHistory() {
        if (historyQueue.size >= SoundcorsetFrequencyPool.HISTORY_SIZE) historyQueue.removeFirst()
        historyQueue.addLast(PitchHistoryPoint(null, SoundcorsetFrequencyPool.A4_INDEX, ""))
    }

    private fun resolveEffectiveFreq(rawFreq: Double, nowMs: Long): Double =
        if (rawFreq > 0.0 && confused && (nowMs - lastStableTimeMs) < CONFUSION_TTL_MILLIS && lastMajorHz > 0.0) {
            lastMajorHz
        } else rawFreq

    private fun emitState(isLiveFrame: Boolean, nowMs: Long) {
        val hasFreq = latestPitchHz > 0.0
        val effFreq = resolveEffectiveFreq(latestPitchHz, nowMs)
        val noteIdx = if (effFreq > 0.0) SoundcorsetFrequencyPool.closestNoteIndex(effFreq, noteFrequencies) else SoundcorsetFrequencyPool.A4_INDEX
        val targetHz = noteFrequencies[noteIdx]
        val cents = if (effFreq > 0.0) SoundcorsetFrequencyPool.diffInCents(targetHz, effFreq).toFloat() else -999f

        _state.value = TunerUiState(
            a4Hz = a4Hz,
            useFlat = useFlat,
            useSolfege = useSolfege,
            isListening = audioInput.isRunning,
            hasActiveNote = hasFreq,
            isLive = isLiveFrame,
            detectedFreqHz = effFreq,
            targetFreqHz = targetHz,
            currentNoteIndex = noteIdx,
            cents = cents,
            score = inTuneScore,
            confusionLevel = confusionLevel,
            pitchHistory = historyQueue.toList(),
            volumeEnvelope = envelopeQueue.toList()
        )
    }

    private fun resetSessionState() {
        volumeTracker.reset()
        pitchQueue.clear()
        pitchMajorQueue.clear()
        historyQueue.clear()
        envelopeQueue.clear()
        latestPitchHz = 0.0
        lastMajorHz = 0.0
        latestPitchUpdateMs = 0L
        confused = false
        confusionLevel = 0.0
        lastStableTimeMs = 0L
        inTuneScore = 0
    }
}
