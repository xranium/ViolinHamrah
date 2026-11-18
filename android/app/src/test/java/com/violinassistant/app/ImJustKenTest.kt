package com.violinassistant.app

import com.violinassistant.app.musicxml.MusicXmlParser
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ImJustKenTest {
    @Test
    fun testParseImJustKen() {
        val file = File("../../musicxml/im-just-ken-ryan-gosling.mxl")
        assertTrue(file.exists())
        val xml = com.violinassistant.app.musicxml.MxlReader.extractXml(file.inputStream())
        val parsed = MusicXmlParser.parse(xml)
        assertNotNull(parsed)
        assertTrue(parsed.notes.isNotEmpty())
        println("Parsed title: " + parsed.metadata.title + ", notes count: " + parsed.notes.size)
    }
}