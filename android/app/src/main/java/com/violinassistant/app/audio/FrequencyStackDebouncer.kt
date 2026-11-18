package com.violinassistant.app.audio

/**
 * Trala-inspired StackDebouncer for violin pitch detection and progression.
 *
 * In violin audio signal processing:
 * 1. Initial bow attack (chiff, transient scratch) generates erratic frequencies before standing waves settle.
 * 2. Harmonic ambiguity (especially 2nd harmonic on G & D strings) can cause octave flickers.
 * 3. Fast repeated triggers for the same note event lead to false multiple failures.
 *
 * FrequencyStackDebouncer enforces:
 * - A debounce cool-down window after note transitions (preventing re-triggering on residual resonance).
 * - Minimum consecutive frame confirmations (Stack count) before committing a note match or wrong attempt.
 * - Squelch on transient noise.
 */
class FrequencyStackDebouncer(
    private val debounceCooldownMs: Long = 180L,
    private val minConsecutiveFrames: Int = 2
) {
    enum class DebounceState {
        READY,
        DEBOUNCING
    }

    private var state = DebounceState.READY
    private var lastDebounceStartTime = 0L
    private var candidateMidi: Int? = null
    private var consecutiveCount: Int = 0

    fun startDebounce(now: Long = System.currentTimeMillis()) {
        state = DebounceState.DEBOUNCING
        lastDebounceStartTime = now
        consecutiveCount = 0
        candidateMidi = null
    }

    fun isReady(now: Long = System.currentTimeMillis()): Boolean {
        if (state == DebounceState.DEBOUNCING) {
            if (now - lastDebounceStartTime >= debounceCooldownMs) {
                state = DebounceState.READY
            } else {
                return false
            }
        }
        return true
    }

    /**
     * Filters incoming raw MIDI detections.
     * Returns the stabilized MIDI note only when it has been detected
     * consistently across [minConsecutiveFrames] frames and after debounce cooldown.
     */
    fun filterMidi(midi: Int, now: Long = System.currentTimeMillis()): Int? {
        if (!isReady(now)) return null

        if (midi == candidateMidi) {
            consecutiveCount++
            if (consecutiveCount >= minConsecutiveFrames) {
                return midi
            }
        } else {
            candidateMidi = midi
            consecutiveCount = 1
            if (minConsecutiveFrames <= 1) {
                return midi
            }
        }
        return null
    }

    fun reset() {
        state = DebounceState.READY
        lastDebounceStartTime = 0L
        candidateMidi = null
        consecutiveCount = 0
    }
}
