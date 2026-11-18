package com.violinassistant.app

import com.violinassistant.app.musicxml.PitchMath
import org.junit.Assert.assertEquals
import org.junit.Test

class PitchMathTest {
    @Test
    fun testViolinOpenStringsMidi() {
        assertEquals(55, PitchMath.calculateMidiPitch("G", 0, 3)) // Open G
        assertEquals(62, PitchMath.calculateMidiPitch("D", 0, 4)) // Open D
        assertEquals(69, PitchMath.calculateMidiPitch("A", 0, 4)) // Open A
        assertEquals(76, PitchMath.calculateMidiPitch("E", 0, 5)) // Open E
        assertEquals(66, PitchMath.calculateMidiPitch("F", 1, 4)) // F#4
    }

    @Test
    fun testMidiToPitchName() {
        assertEquals("C4", PitchMath.midiToPitchName(60))
        assertEquals("A4", PitchMath.midiToPitchName(69))
        assertEquals("F#4", PitchMath.midiToPitchName(66))
        assertEquals("G3", PitchMath.midiToPitchName(55))
    }

    @Test
    fun testFrequencyToMidiAndCents() {
        val standardA4 = PitchMath.frequencyToMidiAndCents(440f, 440)
        assertEquals(69, standardA4.midi)
        assertEquals(0, standardA4.cents)

        val orchestralA4 = PitchMath.frequencyToMidiAndCents(442f, 442)
        assertEquals(69, orchestralA4.midi)
        assertEquals(0, orchestralA4.cents)
    }
}
