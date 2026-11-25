package com.violinassistant.app.practice

import com.violinassistant.app.audio.MetronomeEngine
import com.violinassistant.app.audio.MetronomeState
import com.violinassistant.app.model.NoteStatus
import com.violinassistant.app.tuner.SoundcorsetTunerEngine
import com.violinassistant.app.tuner.TunerUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Coordinates mutually exclusive mic ownership across Soundcorset tuner and Trala
 * sheet practice, plus tempo-locked metronome + 3-2-1-Go countdown transitions.
 */
class PracticeAudioCoordinator(
    private val metronomeEngine: MetronomeEngine = MetronomeEngine(),
    val tunerEngine: SoundcorsetTunerEngine = SoundcorsetTunerEngine(),
    private val postToMain: (() -> Unit) -> Unit = { block -> block() },
    private val onStartListening: () -> Unit,
    private val onStopListening: () -> Unit,
    private val onError: (String) -> Unit = {}
) {
    val metronomeState: StateFlow<MetronomeState> = metronomeEngine.state
    val tunerState: StateFlow<TunerUiState> = tunerEngine.state

    private val _isPracticing = MutableStateFlow(false)
    val isPracticing: StateFlow<Boolean> = _isPracticing.asStateFlow()

    fun configureMetronome(tempoBpm: Float, beatsPerMeasure: Int) {
        metronomeEngine.configure(tempoBpm, beatsPerMeasure)
    }

    fun startPracticeWithCountdown(tempoBpm: Float, beatsPerMeasure: Int) {
        tunerEngine.stop()
        onStopListening()
        _isPracticing.value = true
        metronomeEngine.start(
            tempoBpm = tempoBpm,
            beatsPerMeasure = beatsPerMeasure,
            withCountdown = true,
            onCountdownGo = {
                postToMain {
                    if (!_isPracticing.value) return@postToMain
                    try {
                        onStartListening()
                    } catch (e: Exception) {
                        stopPractice()
                        onError(e.message ?: "خطا در فعال‌سازی میکروفون")
                    }
                }
            }
        )
    }

    fun togglePractice(tempoBpm: Float, beatsPerMeasure: Int) {
        if (_isPracticing.value) {
            stopPractice()
        } else {
            startPracticeWithCountdown(tempoBpm, beatsPerMeasure)
        }
    }

    fun stopPractice() {
        _isPracticing.value = false
        metronomeEngine.stop(resetPhaseToIdle = true)
        onStopListening()
    }

    fun toggleMetronomeMute() {
        metronomeEngine.toggleMute()
    }

    /** Synchronizes metronome HOLD/RESUME with live practice progress (Trala Conductor model). */
    fun syncMetronomeWithEngineState(state: PracticeEngineState) {
        if (!_isPracticing.value) return
        when (state.status) {
            EngineStatus.MEASURING_DURATION -> metronomeEngine.setHold(held = false)
            EngineStatus.WAITING_FOR_NOTE -> {
                val isWrong = state.noteStatuses[state.currentIndex] == NoteStatus.WRONG_PITCH
                metronomeEngine.setHold(held = true, immediate = isWrong)
            }
            EngineStatus.FINISHED -> stopPractice()
            else -> Unit
        }
    }

    fun setTunerActive(active: Boolean, hasMicPermission: Boolean) {
        if (active) {
            stopPractice()
            if (hasMicPermission) tunerEngine.start() else tunerEngine.stop()
        } else {
            tunerEngine.stop()
        }
    }

    fun updateTunerSettings(a4Hz: Float, useFlat: Boolean, useSolfege: Boolean) {
        tunerEngine.updateSettings(a4Hz, useFlat, useSolfege)
    }

    fun releaseAll() {
        stopPractice()
        tunerEngine.stop()
    }
}
