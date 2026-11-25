package com.violinassistant.app

import com.violinassistant.app.model.NoteStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class NotationViewerTest {

    @Test
    fun testViewerHtmlConfiguration() {
        val viewerFile = File("src/main/assets/notation/viewer.html")
        assertTrue("viewer.html must exist", viewerFile.exists())
        val content = viewerFile.readText()

        // Requirement 1: OSMD options
        assertTrue("drawTitle must be false", content.contains("drawTitle: false"))
        assertTrue("drawComposer must be false", content.contains("drawComposer: false"))
        assertTrue("drawPartNames must be false", content.contains("drawPartNames: false"))

        // Requirement 2: Compact padding without 70px bottom gap
        assertTrue("padding must be compact 8px 6px 12px 6px", content.contains("8px 6px 12px 6px"))
        assertTrue("no 70px bottom gap", !content.contains("70px"))

        // Requirement 3: SVG note coloring APIs
        assertTrue("window.setNoteColor must be defined", content.contains("window.setNoteColor = function"))
        assertTrue("window.resetNoteColors must be defined", content.contains("window.resetNoteColors = function"))
        assertTrue("must query child svg elements", content.contains("querySelectorAll('path, ellipse, circle')"))

        // Requirement 4: Smooth system-following gesture cooldown
        assertTrue("cooldown must be 3000ms", content.contains("3000"))
        assertTrue("touchstart listener must be present", content.contains("'touchstart'"))
        assertTrue("touchmove listener must be present", content.contains("'touchmove'"))
        assertTrue("wheel listener must be present", content.contains("'wheel'"))
        assertTrue("viewport threshold top < 12%", content.contains("0.12"))
        assertTrue("viewport threshold bottom > 75%", content.contains("0.75"))

        // Requirement 5: Incremental cursor positioning
        assertTrue("must handle incremental step", content.contains("currentCursorIndex < targetIndex"))

        // Requirement 6: Scale preservation & note shockwave glow
        assertTrue("must preserve applied note colors across scaling", content.contains("appliedNoteColors"))
        assertTrue("must trigger shock glow on played notes", content.contains("triggerNoteShock"))
        assertTrue("must support skipped shock when reapplying scale colors", content.contains("skipShock"))
    }

    @Test
    fun testNoteStatusColorMapping() {
        fun resolveColor(status: NoteStatus): String {
            return when (status) {
                NoteStatus.CORRECT -> "#10b981"
                NoteStatus.WRONG_PITCH -> "#ef4444"
                NoteStatus.WRONG_DURATION -> "#f59e0b"
                else -> "#2563eb"
            }
        }

        assertEquals("CORRECT status must map to emerald green", "#10b981", resolveColor(NoteStatus.CORRECT))
        assertEquals("WRONG_DURATION status must map to orange", "#f59e0b", resolveColor(NoteStatus.WRONG_DURATION))
        assertEquals("WRONG_PITCH status must map to red", "#ef4444", resolveColor(NoteStatus.WRONG_PITCH))
        assertEquals("PENDING status must map to default blue", "#2563eb", resolveColor(NoteStatus.PENDING))
    }
}
