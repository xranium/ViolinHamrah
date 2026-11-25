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
    val onPrimary: Color,
    val accent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val error: Color = Color(0xFFE05252),
    val warning: Color = Color(0xFFE59E36),
    val previewDots: List<Color>
) {
    fun toColorScheme(): ColorScheme = if (isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = bgCardHover,
            onPrimaryContainer = textPrimary,
            surface = bgCard,
            onSurface = textPrimary,
            surfaceVariant = bgCardHover,
            onSurfaceVariant = textSecondary,
            background = bgPrimary,
            onBackground = textPrimary,
            error = error,
            onError = Color.White,
            outline = borderColor
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = bgCardHover,
            onPrimaryContainer = textPrimary,
            surface = bgCard,
            onSurface = textPrimary,
            surfaceVariant = bgCardHover,
            onSurfaceVariant = textSecondary,
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
        nameFa = "شب کنسرت",
        moodFa = "ابنوسی عمیق و برنجی گرم",
        isDark = true,
        bgPrimary = Color(0xFF111318),
        bgDark = Color(0xFF0C0E12),
        bgCard = Color(0xFF181B22),
        bgCardHover = Color(0xFF212530),
        borderColor = Color(0xFF2A2F3B),
        primary = Color(0xFFD9A44E),
        primaryHover = Color(0xFFC48E38),
        onPrimary = Color(0xFF14120E),
        accent = Color(0xFF4ADE80),
        textPrimary = Color(0xFFF2EFE9),
        textSecondary = Color(0xFFA0A6B4),
        textMuted = Color(0xFF707786),
        previewDots = listOf(Color(0xFF111318), Color(0xFF181B22), Color(0xFFD9A44E))
    )

    val Amber = AppThemePalette(
        id = "amber",
        nameFa = "چوب ویولن",
        moodFa = "افرا و لاک استرادیواری",
        isDark = true,
        bgPrimary = Color(0xFF140F0D),
        bgDark = Color(0xFF19120F),
        bgCard = Color(0xFF201815),
        bgCardHover = Color(0xFF2B201C),
        borderColor = Color(0xFF3B2C25),
        primary = Color(0xFFE58A3C),
        primaryHover = Color(0xFFCC7327),
        onPrimary = Color(0xFF1A1109),
        accent = Color(0xFF52C48A),
        textPrimary = Color(0xFFF7EFE7),
        textSecondary = Color(0xFFB5A396),
        textMuted = Color(0xFF817267),
        previewDots = listOf(Color(0xFF140F0D), Color(0xFF201815), Color(0xFFE58A3C))
    )

    val Emerald = AppThemePalette(
        id = "emerald",
        nameFa = "تالار سبز",
        moodFa = "مخمل ارکستر و تمرکز",
        isDark = true,
        bgPrimary = Color(0xFF0D1512),
        bgDark = Color(0xFF101B17),
        bgCard = Color(0xFF16241F),
        bgCardHover = Color(0xFF1F322B),
        borderColor = Color(0xFF2A4038),
        primary = Color(0xFF4DBE8B),
        primaryHover = Color(0xFF3AA675),
        onPrimary = Color(0xFF0B1511),
        accent = Color(0xFFE2B354),
        textPrimary = Color(0xFFEEF6F2),
        textSecondary = Color(0xFF96B0A5),
        textMuted = Color(0xFF667F75),
        previewDots = listOf(Color(0xFF0D1512), Color(0xFF16241F), Color(0xFF4DBE8B))
    )

    val Amethyst = AppThemePalette(
        id = "amethyst",
        nameFa = "مخمل شب",
        moodFa = "زرشکی تیره و صدفی",
        isDark = true,
        bgPrimary = Color(0xFF141018),
        bgDark = Color(0xFF191320),
        bgCard = Color(0xFF201928),
        bgCardHover = Color(0xFF2C2237),
        borderColor = Color(0xFF3C2E4A),
        primary = Color(0xFFC98BB8),
        primaryHover = Color(0xFFB374A2),
        onPrimary = Color(0xFF191018),
        accent = Color(0xFF52C48A),
        textPrimary = Color(0xFFF6F2F8),
        textSecondary = Color(0xFFAEA2B8),
        textMuted = Color(0xFF7B7087),
        previewDots = listOf(Color(0xFF141018), Color(0xFF201928), Color(0xFFC98BB8))
    )

    val Ivory = AppThemePalette(
        id = "ivory",
        nameFa = "کاغذ نت",
        moodFa = "صفحه چاپی گرم و جوهر",
        isDark = false,
        bgPrimary = Color(0xFFF6F2EA),
        bgDark = Color(0xFFECE6DA),
        bgCard = Color(0xFFFFFDF9),
        bgCardHover = Color(0xFFEFE9DD),
        borderColor = Color(0xFFDDD6C7),
        primary = Color(0xFF1D4ED8),
        primaryHover = Color(0xFF1E40AF),
        onPrimary = Color(0xFFFFFFFF),
        accent = Color(0xFF15803D),
        textPrimary = Color(0xFF181A1F),
        textSecondary = Color(0xFF525866),
        textMuted = Color(0xFF818898),
        error = Color(0xFFDC2626),
        warning = Color(0xFFD97706),
        previewDots = listOf(Color(0xFFF6F2EA), Color(0xFFFFFDF9), Color(0xFF1D4ED8))
    )

    val ALL_THEMES = listOf(Midnight, Amber, Emerald, Amethyst, Ivory)

    fun fromId(id: String?): AppThemePalette =
        ALL_THEMES.find { it.id == id } ?: Midnight
}
