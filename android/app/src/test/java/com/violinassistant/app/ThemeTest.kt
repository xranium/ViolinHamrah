package com.violinassistant.app

import com.violinassistant.app.ui.theme.AppThemes
import org.junit.Assert.*
import org.junit.Test

class ThemeTest {

    @Test
    fun testAllThemesCountAndUniqueness() {
        val themes = AppThemes.ALL_THEMES
        assertEquals(5, themes.size)

        val ids = themes.map { it.id }.toSet()
        assertEquals(5, ids.size)
        assertTrue(ids.containsAll(listOf("midnight", "amber", "emerald", "amethyst", "ivory")))
    }

    @Test
    fun testFromIdResolutionAndFallback() {
        assertEquals(AppThemes.Midnight, AppThemes.fromId("midnight"))
        assertEquals(AppThemes.Amber, AppThemes.fromId("amber"))
        assertEquals(AppThemes.Emerald, AppThemes.fromId("emerald"))
        assertEquals(AppThemes.Amethyst, AppThemes.fromId("amethyst"))
        assertEquals(AppThemes.Ivory, AppThemes.fromId("ivory"))

        // Fallback for null or unknown
        assertEquals(AppThemes.Midnight, AppThemes.fromId(null))
        assertEquals(AppThemes.Midnight, AppThemes.fromId("unknown_theme"))
    }

    @Test
    fun testThemeAttributesValidity() {
        AppThemes.ALL_THEMES.forEach { theme ->
            assertTrue("Theme ${theme.id} nameFa must not be blank", theme.nameFa.isNotBlank())
            assertTrue("Theme ${theme.id} moodFa must not be blank", theme.moodFa.isNotBlank())
            assertEquals("Theme ${theme.id} must provide 3 preview dots", 3, theme.previewDots.size)

            val colorScheme = theme.toColorScheme()
            assertNotNull(colorScheme)
            assertEquals(theme.primary, colorScheme.primary)
            assertEquals(theme.bgPrimary, colorScheme.background)
        }
    }
}
