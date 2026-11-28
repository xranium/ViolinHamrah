package com.violinassistant.app.practice

import com.violinassistant.app.audio.FrequencyStackDebouncer
import com.violinassistant.app.model.NoteEvent
import com.violinassistant.app.model.NoteStatus
import com.violinassistant.app.model.PitchResult
import com.violinassistant.app.model.PracticeMode

class PracticeEngine(
    private val notes: List<NoteEvent>,
    private val tolerancePercent: Int = 20,
    private val practiceMode: PracticeMode = PracticeMode.GUIDED,
    private val debouncer: FrequencyStackDebouncer? = null,
    private val glitchGraceMs: Long = 0L,
    private val minWrongNoteVolume: Float = 0f,
    private val syncToBeatOnAutoAdvance: Boolean = false,
    private val pitchToleranceCents: Int = 50,
    private val acceptedPitchBandCents: Int = 50,
    private val onStateChange: ((PracticeEngineState) -> Unit)? = null
) {
    companion object {
        const val TRANSIENT_THRESHOLD_SEC = 0.020f
    }

    private var currentIndex = 0
    private val noteStatuses = mutableMapOf<Int, NoteStatus>()
    private val evaluations = mutableListOf<EvaluationRecord>()
    private var totalWrongAttempts = 0
    private var currentNoteWrongCount = 0
    private var status = EngineStatus.IDLE

    private var notePitchStartTimestamp: Long? = null
    private var lastCorrectTimestampMs: Long? = null
    private var restSoundFrames: Int = 0
    private var greenTimeMs: Long = 0L
    private var orangeTimeMs: Long = 0L
    private var lastAcceptedSampleTs: Long? = null
    private var lastPitchCents: Int = 0
    private var sessionStartTime: Long = 0L

    init {
        reset()
    }

    fun reset() {
        currentIndex = 0
        noteStatuses.clear()
        evaluations.clear()
        totalWrongAttempts = 0
        currentNoteWrongCount = 0
        notePitchStartTimestamp = null
        lastCorrectTimestampMs = null
        restSoundFrames = 0
        resetBandStats()
        lastPitchCents = 0
        debouncer?.reset()
        sessionStartTime = System.currentTimeMillis()
        status = if (notes.isNotEmpty()) EngineStatus.WAITING_FOR_NOTE else EngineStatus.FINISHED
        if (notes.isNotEmpty()) noteStatuses[0] = NoteStatus.ACTIVE
        notifyState(null)
    }

    fun processAudioInput(pitchResult: PitchResult?, timestampMs: Long = System.currentTimeMillis()) {
        if (status == EngineStatus.FINISHED || status == EngineStatus.IDLE || currentIndex >= notes.size) return

        val currentNote = notes[currentIndex]
        if (currentNote.isRest) {
            handleRestNote(pitchResult, currentNote, timestampMs)
            return
        }

        if (pitchResult == null || !pitchResult.isStable) {
            handleGapOrUnstable(pitchResult, currentNote, timestampMs)
            return
        }

        val filteredMidi: Int? = if (debouncer != null) debouncer.filterMidi(pitchResult.midi, timestampMs) else pitchResult.midi
        if (filteredMidi == null) {
            handleGapOrUnstable(pitchResult, currentNote, timestampMs)
            return
        }

        val centsOff = (filteredMidi - currentNote.midi) * 100 + pitchResult.centsDeviation
        val isAccepted = if (acceptedPitchBandCents <= pitchToleranceCents) {
            filteredMidi == currentNote.midi
        } else {
            kotlin.math.abs(centsOff) <= acceptedPitchBandCents
        }
        val isInGreenZone = acceptedPitchBandCents <= pitchToleranceCents || kotlin.math.abs(centsOff) <= pitchToleranceCents

        lastPitchCents = centsOff
        if (isAccepted) {
            if (noteStatuses[currentIndex] == NoteStatus.WRONG_PITCH) {
                noteStatuses[currentIndex] = NoteStatus.ACTIVE
            }
            if (notePitchStartTimestamp == null) {
                notePitchStartTimestamp = timestampMs
                status = EngineStatus.MEASURING_DURATION
                resetBandStats()
            }
            val dt = if (lastAcceptedSampleTs != null) (timestampMs - lastAcceptedSampleTs!!).coerceAtLeast(1L) else 25L
            if (isInGreenZone) greenTimeMs += dt else orangeTimeMs += dt
            lastAcceptedSampleTs = timestampMs
            lastCorrectTimestampMs = timestampMs

            val sustainedSec = (timestampMs - notePitchStartTimestamp!!) / 1000f
            val autoAdvanceMin = DurationEvaluator.minAutoAdvanceDuration(
                currentNote.expectedDurationSec, tolerancePercent, syncToBeatOnAutoAdvance
            )
            if (sustainedSec >= autoAdvanceMin) {
                val durResult = DurationEvaluator.evaluate(currentNote.expectedDurationSec, sustainedSec, tolerancePercent)
                val mostlyInTune = greenTimeMs >= orangeTimeMs
                notePitchStartTimestamp = null
                lastCorrectTimestampMs = null
                advanceCurrentNote(mostlyInTune, durResult.isAcceptable, centsOff, sustainedSec, timestampMs)
                notifyState(null)
            } else {
                notifyState(pitchResult)
            }
        } else {
            val startTs = notePitchStartTimestamp
            val lastValidTs = lastCorrectTimestampMs ?: startTs
            if (startTs != null && lastValidTs != null && glitchGraceMs > 0L && (timestampMs - lastValidTs) <= glitchGraceMs) {
                notifyState(null)
                return
            }
            recordWrongSound(pitchResult, timestampMs)
        }
    }

    private fun handleRestNote(pitchResult: PitchResult?, currentNote: NoteEvent, timestampMs: Long) {
        if (pitchResult == null || !pitchResult.isStable) {
            restSoundFrames = 0
            if (noteStatuses[currentIndex] == NoteStatus.WRONG_PITCH) {
                noteStatuses[currentIndex] = NoteStatus.ACTIVE
            }
            if (notePitchStartTimestamp == null) {
                notePitchStartTimestamp = timestampMs
                status = EngineStatus.MEASURING_DURATION
            }
            val sustainedSilenceSec = ((timestampMs - notePitchStartTimestamp!!) / 1000f).coerceAtLeast(0f)
            val minRest = DurationEvaluator.minRestDuration(
                currentNote.expectedDurationSec, tolerancePercent, syncToBeatOnAutoAdvance
            )
            if (sustainedSilenceSec >= minRest) {
                advanceCurrentNote(true, true, 0, sustainedSilenceSec, timestampMs)
            }
            notifyState(pitchResult)
            return
        }
        if (debouncer != null) {
            if (!debouncer.isReady(timestampMs)) {
                notifyState(null)
                return
            }
            val filteredMidi = debouncer.filterMidi(pitchResult.midi, timestampMs)
            if (filteredMidi == null && ++restSoundFrames < 2) {
                notifyState(null)
                return
            }
        }
        recordWrongSound(pitchResult, timestampMs)
    }

    private fun recordWrongSound(pitchResult: PitchResult, timestampMs: Long) {
        if (pitchResult.volume < minWrongNoteVolume) {
            notifyState(null)
            return
        }
        notePitchStartTimestamp = null
        lastCorrectTimestampMs = null
        restSoundFrames = 0
        resetBandStats()
        noteStatuses[currentIndex] = NoteStatus.WRONG_PITCH
        totalWrongAttempts++
        currentNoteWrongCount++
        debouncer?.startDebounce(timestampMs)
        status = EngineStatus.WAITING_FOR_NOTE
        notifyState(pitchResult)
    }

    private fun handleGapOrUnstable(pitchResult: PitchResult?, currentNote: NoteEvent, timestampMs: Long) {
        val startTs = notePitchStartTimestamp
        if (startTs != null) {
            val lastValidTs = lastCorrectTimestampMs ?: startTs
            if (glitchGraceMs > 0L && (timestampMs - lastValidTs) <= glitchGraceMs) {
                notifyState(null)
                return
            }
            val endTs = if (glitchGraceMs > 0L) lastValidTs else timestampMs
            val sustainedSec = ((endTs - startTs) / 1000f).coerceAtLeast(0f)
            val mostlyInTune = greenTimeMs >= orangeTimeMs
            notePitchStartTimestamp = null
            lastCorrectTimestampMs = null
            resetBandStats()
            if (sustainedSec < TRANSIENT_THRESHOLD_SEC) {
                status = EngineStatus.WAITING_FOR_NOTE
                notifyState(pitchResult)
                return
            }
            val durResult = DurationEvaluator.evaluate(currentNote.expectedDurationSec, sustainedSec, tolerancePercent)
            advanceCurrentNote(mostlyInTune, durResult.isAcceptable, lastPitchCents, sustainedSec, timestampMs)
        }
        notifyState(pitchResult)
    }

    private fun advanceCurrentNote(
        pitchCorrect: Boolean,
        durationCorrect: Boolean,
        centsDeviation: Int,
        playedDurationSec: Float,
        timestampMs: Long
    ) {
        val note = notes[currentIndex]
        evaluations.add(
            EvaluationRecord(
                noteId = note.id, expectedMidi = note.midi, pitchCorrect = pitchCorrect,
                durationCorrect = durationCorrect, centsDeviation = centsDeviation,
                expectedDurationSec = note.expectedDurationSec, playedDurationSec = playedDurationSec,
                wrongAttemptsCount = currentNoteWrongCount
            )
        )
        noteStatuses[currentIndex] = when {
            !pitchCorrect -> {
                status = EngineStatus.COMPLETED_WITH_ERROR
                NoteStatus.WRONG_PITCH
            }
            !durationCorrect -> {
                status = EngineStatus.COMPLETED_WITH_ERROR
                NoteStatus.WRONG_DURATION
            }
            else -> {
                status = EngineStatus.COMPLETED_CORRECTLY
                NoteStatus.CORRECT
            }
        }
        currentIndex++
        currentNoteWrongCount = 0
        notePitchStartTimestamp = null
        lastCorrectTimestampMs = null
        restSoundFrames = 0
        resetBandStats()
        debouncer?.startDebounce(timestampMs)
        if (currentIndex >= notes.size) {
            status = EngineStatus.FINISHED
        } else {
            noteStatuses[currentIndex] = NoteStatus.ACTIVE
            status = EngineStatus.WAITING_FOR_NOTE
        }
    }

    fun getState(detectedPitch: PitchResult? = null): PracticeEngineState {
        val isFinished = status == EngineStatus.FINISHED || currentIndex >= notes.size
        val score = if (isFinished) ScoringEngine.calculateOverallScore(notes.size, evaluations, totalWrongAttempts) else null
        val currentNote = if (currentIndex < notes.size) notes[currentIndex] else null
        return PracticeEngineState(
            status = status, currentIndex = currentIndex, totalNotes = notes.size,
            noteStatuses = noteStatuses.toMap(), evaluations = evaluations.toList(),
            totalWrongAttempts = totalWrongAttempts, overallScore = score,
            currentExpectedNote = currentNote, detectedPitch = detectedPitch,
            sessionStartTime = sessionStartTime,
            sessionDurationSec = (System.currentTimeMillis() - sessionStartTime) / 1000L
        )
    }

    private fun resetBandStats() {
        greenTimeMs = 0L
        orangeTimeMs = 0L
        lastAcceptedSampleTs = null
    }

    private fun notifyState(pitchResult: PitchResult?) {
        onStateChange?.invoke(getState(pitchResult))
    }
}
