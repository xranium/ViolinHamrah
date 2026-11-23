package com.violinassistant.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

data class AppThemePalette(
    val id: String,
    val nameFa: String,
    val moodFa: String,
    val isDark: Boolean,
    val bgPrimary: Color,
    val bgDark: Color,
    val bgCard: Color,
    val bgCardHover: Color,
    val borderColor: Color,
    val primary: Color,
    val primaryHover: Color,
    val accent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val error: Color = Color(0xFFEF4444),
    val warning: Color = Color(0xFFF59E0B),
    val previewDots: List<Color>
) {
    fun toColorScheme(): ColorScheme = if (isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = if (isDark) Color.White else textPrimary,
            primaryContainer = bgCardHover,
            onPrimaryContainer = textPrimary,
            surface = bgCard,
            onSurface = textPrimary,
            background = bgPrimary,
            onBackground = textPrimary,
            error = error,
            onError = Color.White,
            outline = borderColor
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = bgCardHover,
            onPrimaryContainer = textPrimary,
            surface = bgCard,
            onSurface = textPrimary,
            background = bgPrimary,
            onBackground = textPrimary,
            error = error,
            onError = Color.White,
            outline = borderColor
        )
    }
}

object AppThemes {
    val Midnight = AppThemePalette(
        id = "midnight",
        nameFa = "استودیوی شب",
        moodFa = "تیره مدرن با جلوه آسمانی",
        isDark = true,
        bgPrimary = Color(0xFF0B0F19),
        bgDark = Color(0xFF0F172A),
        bgCard = Color(0xFF151D2F),
        bgCardHover = Color(0xFF1E293B),
        borderColor = Color(0xFF26334D),
        primary = Color(0xFF38BDF8),
        primaryHover = Color(0xFF0EA5E9),
        accent = Color(0xFF10B981),
        textPrimary = Color(0xFFF1F5F9),
        textSecondary = Color(0xFF94A3B8),
        textMuted = Color(0xFF64748B),
        previewDots = listOf(Color(0xFF0B0F19), Color(0xFF151D2F), Color(0xFF38BDF8))
    )

    val Amber = AppThemePalette(
        id = "amber",
        nameFa = "ویولن کهربایی",
        moodFa = "چوب کلاسیک و وارنیش گرم",
        isDark = true,
        bgPrimary = Color(0xFF120E0C),
        bgDark = Color(0xFF18120F),
        bgCard = Color(0xFF1F1713),
        bgCardHover = Color(0xFF2E221D),
        borderColor = Color(0xFF45332B),
        primary = Color(0xFFF59E0B),
        primaryHover = Color(0xFFD97706),
        accent = Color(0xFF10B981),
        textPrimary = Color(0xFFFAF5F0),
        textSecondary = Color(0xFFA8998C),
        textMuted = Color(0xFF786C62),
        previewDots = listOf(Color(0xFF120E0C), Color(0xFF1F1713), Color(0xFFF59E0B))
    )

    val Emerald = AppThemePalette(
        id = "emerald",
        nameFa = "سبز ارکسترال",
        moodFa = "آرامش‌بخش و تمرکز عمیق",
        isDark = true,
        bgPrimary = Color(0xFF081411),
        bgDark = Color(0xFF0D1E1A),
        bgCard = Color(0xFF122823),
        bgCardHover = Color(0xFF1B3B34),
        borderColor = Color(0xFF27554B),
        primary = Color(0xFF10B981),
        primaryHover = Color(0xFF059669),
        accent = Color(0xFFF59E0B),
        textPrimary = Color(0xFFECFDF5),
        textSecondary = Color(0xFF86A89B),
        textMuted = Color(0xFF5A786E),
        previewDots = listOf(Color(0xFF081411), Color(0xFF122823), Color(0xFF10B981))
    )

    val Amethyst = AppThemePalette(
        id = "amethyst",
        nameFa = "ارغوان سلطنتی",
        moodFa = "بنفش مخملی لوکس",
        isDark = true,
        bgPrimary = Color(0xFF0D0B18),
        bgDark = Color(0xFF131024),
        bgCard = Color(0xFF1B1733),
        bgCardHover = Color(0xFF28224D),
        borderColor = Color(0xFF3E3574),
        primary = Color(0xFFA78BFA),
        primaryHover = Color(0xFF8B5CF6),
        accent = Color(0xFF34D399),
        textPrimary = Color(0xFFF5F3FF),
        textSecondary = Color(0xFFA19EBE),
        textMuted = Color(0xFF6E6B8A),
        previewDots = listOf(Color(0xFF0D0B18), Color(0xFF1B1733), Color(0xFFA78BFA))
    )

    val Ivory = AppThemePalette(
        id = "ivory",
        nameFa = "کاغذی روز",
        moodFa = "پارچمنت روشن چاپ نتها",
        isDark = false,
        bgPrimary = Color(0xFFF7F5F0),
        bgDark = Color(0xFFEFECE4),
        bgCard = Color(0xFFFFFFFF),
        bgCardHover = Color(0xFFF0EDE5),
        borderColor = Color(0xFFDDD7CC),
        primary = Color(0xFF2563EB),
        primaryHover = Color(0xFF1D4ED8),
        accent = Color(0xFF059669),
        textPrimary = Color(0xFF1E293B),
        textSecondary = Color(0xFF64748B),
        textMuted = Color(0xFF94A3B8),
        previewDots = listOf(Color(0xFFF7F5F0), Color(0xFFFFFFFF), Color(0xFF2563EB))
    )

    val ALL_THEMES = listOf(Midnight, Amber, Emerald, Amethyst, Ivory)

    fun fromId(id: String?): AppThemePalette =
        ALL_THEMES.find { it.id == id } ?: Midnight
}
