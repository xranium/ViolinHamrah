package com.violinassistant.app.audio

import android.content.Context
import android.os.Process
import com.violinassistant.app.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.roundToLong

/**
 * Sample-accurate built-in metronome using Soundcorset's recorded Tick/Tock PCM sounds
 * with a tempo-synced 3 -> 2 -> 1 -> Go countdown.
 */
class MetronomeEngine(
    private val appContext: Context? = null,
    private val audioSinkFactory: () -> MetronomeAudioSink = { AndroidMetronomeAudioSink() }
) {
    private data class ActiveVoice(val wave: ShortArray, var pos: Int = 0)
    private val _state = MutableStateFlow(MetronomeState())
    val state: StateFlow<MetronomeState> = _state.asStateFlow()

    @Volatile private var running = false
    private var workerThread: Thread? = null
    private var accentPcm: ShortArray = ShortArray(0)
    private var regularPcm: ShortArray = ShortArray(0)

    fun configure(tempoBpm: Float, beatsPerMeasure: Int) {
        _state.update {
            it.copy(
                tempoBpm = tempoBpm.coerceIn(20f, 300f),
                beatsPerMeasure = beatsPerMeasure.coerceIn(1, 12)
            )
        }
    }

    fun toggleMute() {
        _state.update { it.copy(isMuted = !it.isMuted) }
    }

    fun start(
        tempoBpm: Float,
        beatsPerMeasure: Int,
        withCountdown: Boolean,
        onCountdownGo: () -> Unit
    ) {
        stop(resetPhaseToIdle = false)
        configure(tempoBpm, beatsPerMeasure)

        val sink = audioSinkFactory()
        if (accentPcm.isEmpty()) {
            val ctx = appContext
            if (ctx != null) {
                accentPcm = MetronomeWaveforms.loadRawPcm16(ctx, R.raw.soundcorset_tick)
                regularPcm = MetronomeWaveforms.loadRawPcm16(ctx, R.raw.soundcorset_tock)
            } else {
                accentPcm = MetronomeWaveforms.synthesizeWoodblock(sink.sampleRate, 1500.0, 0.65)
                regularPcm = MetronomeWaveforms.synthesizeWoodblock(sink.sampleRate, 1000.0, 0.45)
            }
        }
        if (!sink.open()) {
            _state.update { it.copy(isPlaying = false, countdownPhase = CountdownPhase.LISTENING) }
            onCountdownGo()
            return
        }

        running = true
        val initialPhase = if (withCountdown) CountdownPhase.COUNT_3 else CountdownPhase.LISTENING
        _state.update {
            it.copy(
                isPlaying = true,
                currentBeatInMeasure = 1,
                beatPulseCount = 0L,
                countdownPhase = initialPhase
            )
        }
        if (!withCountdown) onCountdownGo()

        val thread = Thread({
            runCatching { Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO) }
            runStreamLoop(sink, withCountdown, onCountdownGo)
        }, "MetronomeEngineThread")
        workerThread = thread
        thread.start()
    }

    fun stop(resetPhaseToIdle: Boolean = true) {
        running = false
        val t = workerThread
        workerThread = null
        if (t != null && t !== Thread.currentThread()) {
            runCatching { t.join(350) }
        }
        _state.update {
            it.copy(
                isPlaying = false,
                currentBeatInMeasure = 0,
                countdownPhase = if (resetPhaseToIdle) CountdownPhase.IDLE else it.countdownPhase
            )
        }
    }

    private fun runStreamLoop(
        sink: MetronomeAudioSink,
        withCountdown: Boolean,
        onCountdownGo: () -> Unit
    ) {
        val sr = sink.sampleRate
        val chunkFrames = 512
        val buf = ShortArray(chunkFrames)
        val activeVoices = ArrayList<ActiveVoice>(4)
        var writtenFrames = 0L
        var beatIndex = 0L
        var nextBeatFrame = 0L
        var goTriggered = !withCountdown
        var goFrame = Long.MAX_VALUE

        try {
            while (running) {
                val snapshot = _state.value
                val framesPerBeat = ((sr * 60.0) / snapshot.tempoBpm).roundToLong().coerceAtLeast(1000L)
                val muted = snapshot.isMuted
                val beatsPerMeasure = snapshot.beatsPerMeasure

                for (sampleIdx in 0 until chunkFrames) {
                    val globalFrame = writtenFrames + sampleIdx
                    while (nextBeatFrame <= globalFrame) {
                        val phase = countdownPhaseForBeat(beatIndex, withCountdown)
                        val isAccent = if (withCountdown && beatIndex < 4L) {
                            beatIndex == 3L
                        } else {
                            val measureBeat = if (withCountdown) (beatIndex - 3L) else beatIndex
                            (measureBeat % beatsPerMeasure) == 0L
                        }
                        val displayBeat = if (withCountdown && beatIndex < 3L) {
                            (3 - beatIndex).toInt()
                        } else {
                            val mb = if (withCountdown) (beatIndex - 3L) else beatIndex
                            ((mb % beatsPerMeasure) + 1L).toInt()
                        }

                        if (!muted) activeVoices.add(ActiveVoice(if (isAccent) accentPcm else regularPcm))
                        _state.update {
                            val resolved = if (goTriggered && phase == CountdownPhase.GO) CountdownPhase.LISTENING else phase
                            it.copy(currentBeatInMeasure = displayBeat, beatPulseCount = it.beatPulseCount + 1L, countdownPhase = resolved)
                        }
                        if (!goTriggered && beatIndex == 3L) goFrame = nextBeatFrame
                        beatIndex++
                        nextBeatFrame += framesPerBeat
                    }

                    var mix = 0
                    for (v in activeVoices.size - 1 downTo 0) {
                        val voice = activeVoices[v]
                        mix += voice.wave[voice.pos++].toInt()
                        if (voice.pos >= voice.wave.size) activeVoices.removeAt(v)
                    }
                    buf[sampleIdx] = mix.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }

                sink.writeSamples(buf, chunkFrames)
                writtenFrames += chunkFrames

                if (!goTriggered && goFrame != Long.MAX_VALUE &&
                    (sink.playbackHeadFrames() >= goFrame - chunkFrames || (writtenFrames - goFrame) >= 2048L)
                ) {
                    goTriggered = true
                    onCountdownGo()
                }
                if (goTriggered && _state.value.countdownPhase == CountdownPhase.GO &&
                    (writtenFrames - goFrame) > (sr * 0.45).roundToLong()
                ) {
                    _state.update { it.copy(countdownPhase = CountdownPhase.LISTENING) }
                }
            }
        } finally {
            sink.close()
        }
    }

    private fun countdownPhaseForBeat(beatIndex: Long, withCountdown: Boolean): CountdownPhase =
        if (!withCountdown) CountdownPhase.LISTENING
        else when (beatIndex) {
            0L -> CountdownPhase.COUNT_3
            1L -> CountdownPhase.COUNT_2
            2L -> CountdownPhase.COUNT_1
            3L -> CountdownPhase.GO
            else -> CountdownPhase.LISTENING
        }
}
