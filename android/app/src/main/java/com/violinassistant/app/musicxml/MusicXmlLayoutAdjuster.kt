package com.violinassistant.app.musicxml

import java.util.regex.Pattern

/**
 * Adjusts MusicXML layout by configuring the number of measures per line (system).
 * Follows Trala's mechanism: removes existing system breaks and injects
 * <print new-system="yes"/> at measure boundaries matching (measureIndex - 1) % measuresPerLine == 0.
 */
object MusicXmlLayoutAdjuster {

    private val PRINT_SYSTEM_REGEX = Pattern.compile(
        """<print\b[^>]*\bnew-system=["'][^"']*["'][^>]*>(?:(?!<\/print>).)*<\/print>|<print\b[^>]*\bnew-system=["'][^"']*["'][^>]*\/>""",
        Pattern.CASE_INSENSITIVE or Pattern.DOTALL
    )

    private val MEASURE_START_REGEX = Pattern.compile(
        """(<measure\b[^>]*>)""",
        Pattern.CASE_INSENSITIVE
    )

    fun adjustMeasuresPerLine(xmlContent: String, measuresPerLine: Int): String {
        if (xmlContent.isBlank() || measuresPerLine <= 0) {
            return xmlContent
        }

        // 1. Remove existing new-system print elements
        val cleanedXml = PRINT_SYSTEM_REGEX.matcher(xmlContent).replaceAll("")

        // 2. Count sequential measures and inject <print new-system="yes"/>
        val matcher = MEASURE_START_REGEX.matcher(cleanedXml)
        val sb = StringBuffer()
        var measureIndex = 0

        while (matcher.find()) {
            measureIndex++
            val measureTag = matcher.group(1) ?: "<measure>"
            if (measureIndex > 1 && (measureIndex - 1) % measuresPerLine == 0) {
                matcher.appendReplacement(sb, "$measureTag\n    <print new-system=\"yes\"/>")
            } else {
                matcher.appendReplacement(sb, measureTag)
            }
        }
        matcher.appendTail(sb)

        return sb.toString()
    }
}
