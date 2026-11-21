package com.violinassistant.app

import com.violinassistant.app.musicxml.MusicXmlLayoutAdjuster
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicXmlLayoutAdjusterTest {

    private val sampleScore = """
        <?xml version="1.0" encoding="UTF-8"?>
        <score-partwise version="3.1">
          <part id="P1">
            <measure number="1">
              <note><pitch><step>C</step><octave>4</octave></pitch><duration>4</duration></note>
            </measure>
            <measure number="2">
              <note><pitch><step>D</step><octave>4</octave></pitch><duration>4</duration></note>
            </measure>
            <measure number="3">
              <print new-system="yes"/>
              <note><pitch><step>E</step><octave>4</octave></pitch><duration>4</duration></note>
            </measure>
            <measure number="4">
              <note><pitch><step>F</step><octave>4</octave></pitch><duration>4</duration></note>
            </measure>
            <measure number="5">
              <note><pitch><step>G</step><octave>4</octave></pitch><duration>4</duration></note>
            </measure>
          </part>
        </score-partwise>
    """.trimIndent()

    @Test
    fun testAdjustMeasuresPerLine2() {
        val result = MusicXmlLayoutAdjuster.adjustMeasuresPerLine(sampleScore, 2)
        // With 2 measures per line: measures 3 and 5 should have <print new-system="yes"/>
        assertTrue(result.contains("""<measure number="3">
    <print new-system="yes"/>"""))
        assertTrue(result.contains("""<measure number="5">
    <print new-system="yes"/>"""))
        // Measure 1, 2, 4 should not have new-system
        val occurrences = "<print new-system=\"yes\"/>".toRegex().findAll(result).count()
        assertEquals(2, occurrences)
    }

    @Test
    fun testAdjustMeasuresPerLine1() {
        val result = MusicXmlLayoutAdjuster.adjustMeasuresPerLine(sampleScore, 1)
        // With 1 measure per line: measures 2, 3, 4, 5 all get new-system
        val occurrences = "<print new-system=\"yes\"/>".toRegex().findAll(result).count()
        assertEquals(4, occurrences)
    }

    @Test
    fun testAdjustMeasuresPerLine4() {
        val result = MusicXmlLayoutAdjuster.adjustMeasuresPerLine(sampleScore, 4)
        // With 4 measures per line: measure 5 gets new-system
        val occurrences = "<print new-system=\"yes\"/>".toRegex().findAll(result).count()
        assertEquals(1, occurrences)
    }
}
