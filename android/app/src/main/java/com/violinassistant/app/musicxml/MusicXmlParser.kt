package com.violinassistant.app.musicxml

import com.violinassistant.app.model.*
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

object MusicXmlParser {

    fun parse(xmlString: String, overrideMeta: ScoreMetadata? = null): ParsedScore {
        require(xmlString.isNotBlank()) { "محتوای فایل نت خالی است." }

        // Strip UTF-8 BOM and sanitize doctype to prevent XXE
        val cleanXml = xmlString.trim().removePrefix("\uFEFF").replace(Regex("<!DOCTYPE[^>]*>", RegexOption.IGNORE_CASE), "")

        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = false
        factory.isValidating = false
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(InputSource(StringReader(cleanXml)))
        doc.documentElement.normalize()

        val rootName = doc.documentElement.nodeName
        require(rootName == "score-partwise" || rootName == "score-timewise") {
            "فایل انتخاب‌شده یک فایل معتبر MusicXML نیست."
        }

        // Title and Composer
        val workTitle = doc.getElementsByTagName("work-title").item(0)?.textContent?.trim()
            ?: doc.getElementsByTagName("movement-title").item(0)?.textContent?.trim()
            ?: overrideMeta?.title ?: "قطعه بدون عنوان"

        val composer = doc.getElementsByTagName("creator").item(0)?.textContent?.trim()
            ?: overrideMeta?.composer ?: "ناشناس"

        var divisions = 1
        var tempo = extractTempo(doc)
        var timeBeats = 4
        var timeBeatType = 4
        var keyFifths = 0

        val measureNodes = doc.getElementsByTagName("measure")
        val notes = mutableListOf<NoteEvent>()
        var eventCounter = 0

        for (mIdx in 0 until measureNodes.length) {
            val measureElem = measureNodes.item(mIdx) as Element
            val measureNum = measureElem.getAttribute("number").toIntOrNull() ?: (mIdx + 1)

            // Read measure attributes if present
            val divElem = measureElem.getElementsByTagName("divisions").item(0)
            if (divElem != null) {
                val d = divElem.textContent.trim().toIntOrNull()
                if (d != null && d > 0) divisions = d
            }

            val beatsElem = measureElem.getElementsByTagName("beats").item(0)
            val beatTypeElem = measureElem.getElementsByTagName("beat-type").item(0)
            if (beatsElem != null && beatTypeElem != null) {
                timeBeats = beatsElem.textContent.trim().toIntOrNull() ?: timeBeats
                timeBeatType = beatTypeElem.textContent.trim().toIntOrNull() ?: timeBeatType
            }

            val fifthsElem = measureElem.getElementsByTagName("fifths").item(0)
            if (fifthsElem != null) {
                keyFifths = fifthsElem.textContent.trim().toIntOrNull() ?: keyFifths
            }

            // Iterate children to only select direct <note> nodes
            val children = measureElem.childNodes
            for (c in 0 until children.length) {
                val node = children.item(c)
                if (node.nodeType != Node.ELEMENT_NODE || node.nodeName != "note") continue
                val noteElem = node as Element

                // Skip secondary notes of chords (focus on primary melody)
                if (noteElem.getElementsByTagName("chord").length > 0) continue

                val isRest = noteElem.getElementsByTagName("rest").length > 0
                val durationVal = noteElem.getElementsByTagName("duration").item(0)?.textContent?.trim()?.toIntOrNull() ?: 1
                val voice = noteElem.getElementsByTagName("voice").item(0)?.textContent?.trim()?.toIntOrNull() ?: 1

                val durationInBeats = durationVal.toFloat() / divisions.toFloat()
                val quarterSec = 60f / tempo
                val expectedDurationSec = (durationInBeats * quarterSec * (4f / timeBeatType)).coerceAtLeast(0.1f)

                val tieStart = hasMatchingAttr(noteElem, "tie", "type", "start")
                val tieStop = hasMatchingAttr(noteElem, "tie", "type", "stop")

                var step = "A"
                var alter = 0
                var octave = 4
                var midi = 69
                var pitchName = "A4"

                if (!isRest) {
                    val stepText = noteElem.getElementsByTagName("step").item(0)?.textContent?.trim()
                    val alterText = noteElem.getElementsByTagName("alter").item(0)?.textContent?.trim()
                    val octaveText = noteElem.getElementsByTagName("octave").item(0)?.textContent?.trim()

                    step = stepText?.uppercase() ?: "A"
                    alter = alterText?.toIntOrNull() ?: 0
                    octave = octaveText?.toIntOrNull() ?: 4

                    midi = PitchMath.calculateMidiPitch(step, alter, octave)
                    pitchName = PitchMath.midiToPitchName(midi)
                }

                notes.add(
                    NoteEvent(
                        id = "note-${measureNum}-${eventCounter++}",
                        step = step,
                        alter = alter,
                        octave = octave,
                        midi = midi,
                        pitchName = if (isRest) "سکوت" else pitchName,
                        durationInBeats = durationInBeats,
                        expectedDurationSec = expectedDurationSec,
                        measure = measureNum,
                        voice = voice,
                        isRest = isRest,
                        isTiedToNext = tieStart,
                        isTiedFromPrev = tieStop,
                        status = NoteStatus.PENDING
                    )
                )
            }
        }

        val meta = overrideMeta ?: ScoreMetadata(
            id = "score-${System.currentTimeMillis()}",
            title = workTitle,
            composer = composer,
            difficulty = ScoreDifficulty.BEGINNER,
            category = ScoreCategory.CLASSICAL
        )

        return ParsedScore(
            metadata = meta,
            tempo = tempo,
            timeSignature = TimeSignature(timeBeats, timeBeatType),
            keySignature = KeySignature(keyFifths),
            notes = notes,
            xmlContent = cleanXml
        )
    }

    private fun extractTempo(doc: org.w3c.dom.Document): Float {
        val soundNodes = doc.getElementsByTagName("sound")
        for (i in 0 until soundNodes.length) {
            val elem = soundNodes.item(i) as? Element
            val t = elem?.getAttribute("tempo")?.trim()?.toFloatOrNull()
            if (t != null && t in 20f..300f) return t
        }
        val perMinNodes = doc.getElementsByTagName("per-minute")
        for (i in 0 until perMinNodes.length) {
            val t = perMinNodes.item(i)?.textContent?.trim()?.toFloatOrNull()
            if (t != null && t in 20f..300f) return t
        }
        return 80f
    }

    private fun hasMatchingAttr(elem: Element, tag: String, attrName: String, attrVal: String): Boolean {
        val list = elem.getElementsByTagName(tag)
        for (i in 0 until list.length) {
            val sub = list.item(i) as? Element
            if (sub?.getAttribute(attrName) == attrVal) return true
        }
        return false
    }
}
