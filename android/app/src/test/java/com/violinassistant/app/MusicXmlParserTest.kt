package com.violinassistant.app

import com.violinassistant.app.musicxml.MusicXmlParser
import com.violinassistant.app.musicxml.sample.SAMPLE_MORGH_E_SAHAR_XML
import com.violinassistant.app.musicxml.sample.SAMPLE_ODE_TO_JOY_XML
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicXmlParserTest {
    @Test
    fun testParseOdeToJoy() {
        val score = MusicXmlParser.parse(SAMPLE_ODE_TO_JOY_XML)
        assertEquals("سرود شادی", score.metadata.title)
        assertEquals(90f, score.tempo)
        assertEquals(4, score.timeSignature.beats)
        assertTrue(score.notes.isNotEmpty())

        val firstNote = score.notes[0]
        assertEquals("F", firstNote.step)
        assertEquals(1, firstNote.alter)
        assertEquals(4, firstNote.octave)
        assertEquals(66, firstNote.midi)
        assertEquals("F#4", firstNote.pitchName)
    }

    @Test
    fun testParseMorghESahar() {
        val score = MusicXmlParser.parse(SAMPLE_MORGH_E_SAHAR_XML)
        assertEquals("مرغ سحر", score.metadata.title)
        assertEquals(76f, score.tempo)
        assertEquals(3, score.timeSignature.beats)
        assertTrue(score.notes.isNotEmpty())
        assertEquals("A4", score.notes[0].pitchName)
    }

    @Test(expected = Exception::class)
    fun testMalformedXmlThrowsException() {
        MusicXmlParser.parse("invalid non-xml text")
    }
}
