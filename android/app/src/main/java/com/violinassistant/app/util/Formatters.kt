package com.violinassistant.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {
    private val PERSIAN_DIGITS = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    fun toPersianDigits(input: Any?): String {
        if (input == null) return ""
        val str = input.toString()
        val builder = StringBuilder(str.length)
        for (ch in str) {
            if (ch in '0'..'9') {
                builder.append(PERSIAN_DIGITS[ch - '0'])
            } else {
                builder.append(ch)
            }
        }
        return builder.toString()
    }

    fun formatDurationMMSS(totalSeconds: Long): String {
        val mins = totalSeconds / 60
        val secs = totalSeconds % 60
        val pad = { n: Long -> if (n < 10) "0$n" else "$n" }
        return toPersianDigits("${pad(mins)}:${pad(secs)}")
    }

    fun formatDurationHuman(totalSeconds: Long): String {
        val mins = totalSeconds / 60
        val secs = totalSeconds % 60
        return when {
            mins == 0L -> "${toPersianDigits(secs)} ثانیه"
            secs == 0L -> "${toPersianDigits(mins)} دقیقه"
            else -> "${toPersianDigits(mins)} دقیقه و ${toPersianDigits(secs)} ثانیه"
        }
    }

    fun formatToJalali(isoDate: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val date = parser.parse(isoDate.substringBefore('.')) ?: Date()
            val formatter = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("fa", "IR"))
            toPersianDigits(formatter.format(date))
        } catch (_: Exception) {
            toPersianDigits(isoDate.take(16).replace('T', ' '))
        }
    }
}
