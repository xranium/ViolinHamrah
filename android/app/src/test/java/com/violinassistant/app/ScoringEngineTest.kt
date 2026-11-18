package com.violinassistant.app

import com.violinassistant.app.practice.EvaluationRecord
import com.violinassistant.app.practice.ScoringEngine
import com.violinassistant.app.util.Formatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoringEngineTest {
    @Test
    fun testPerfectScoreCalculation() {
        val evals = listOf(
            EvaluationRecord("1", 60, true, true, 0, 1f, 1f, 0),
            EvaluationRecord("2", 62, true, true, 1, 1f, 1f, 0)
        )
        val score = ScoringEngine.calculateOverallScore(2, evals, 0)
        assertTrue("امتیاز کامل باید بالای ۹۸ باشد", score in 98..100)
    }

    @Test
    fun testPenaltiesDeductScore() {
        val evals = listOf(
            EvaluationRecord("1", 60, true, false, 0, 1f, 0.3f, 2),
            EvaluationRecord("2", 62, false, false, 80, 1f, 0f, 3)
        )
        val score = ScoringEngine.calculateOverallScore(2, evals, 5)
        assertTrue("امتیاز با خطا باید کمتر از ۶۰ باشد", score < 60)
        assertTrue(score >= 0)
    }

    @Test
    fun testPersianFormatters() {
        assertEquals("۱۰۰", Formatters.toPersianDigits(100))
        assertEquals("۸۷", Formatters.toPersianDigits(87))
        assertEquals("۰۱:۰۵", Formatters.formatDurationMMSS(65))
    }
}
