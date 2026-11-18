package com.violinassistant.app.audio

import org.junit.Assert.*
import org.junit.Test

class FrequencyStackDebouncerTest {

    @Test
    fun testConsecutiveFramesFilter() {
        val debouncer = FrequencyStackDebouncer(debounceCooldownMs = 100L, minConsecutiveFrames = 2)
        val now = 1000L

        // First frame candidate -> returns null
        assertNull(debouncer.filterMidi(60, now))

        // Second frame consistent -> returns 60
        assertEquals(60, debouncer.filterMidi(60, now + 20))
    }

    @Test
    fun testDebounceCooldownRejection() {
        val debouncer = FrequencyStackDebouncer(debounceCooldownMs = 100L, minConsecutiveFrames = 1)
        val now = 1000L

        assertEquals(60, debouncer.filterMidi(60, now))

        // Start debounce cooldown
        debouncer.startDebounce(now)
        assertFalse(debouncer.isReady(now + 50))
        assertNull(debouncer.filterMidi(62, now + 50))

        // Cooldown passed
        assertTrue(debouncer.isReady(now + 120))
        assertEquals(62, debouncer.filterMidi(62, now + 120))
    }

    @Test
    fun testReset() {
        val debouncer = FrequencyStackDebouncer(debounceCooldownMs = 100L, minConsecutiveFrames = 2)
        debouncer.startDebounce(1000L)
        assertFalse(debouncer.isReady(1050L))

        debouncer.reset()
        assertTrue(debouncer.isReady(1050L))
    }
}
