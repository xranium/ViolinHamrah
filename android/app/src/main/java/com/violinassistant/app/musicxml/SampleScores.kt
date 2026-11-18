package com.violinassistant.app.musicxml

import com.violinassistant.app.model.ScoreCategory
import com.violinassistant.app.model.ScoreDifficulty
import com.violinassistant.app.model.ScoreMetadata
import com.violinassistant.app.musicxml.sample.SAMPLE_D_MAJOR_SCALE_XML
import com.violinassistant.app.musicxml.sample.SAMPLE_MORGH_E_SAHAR_XML
import com.violinassistant.app.musicxml.sample.SAMPLE_ODE_TO_JOY_XML

object SampleScores {
    val BUILT_IN_SCORES = listOf(
        ScoreMetadata(
            id = "ode-to-joy",
            title = "سرود شادی (بتهوون)",
            composer = "لودویگ فان بتهوون",
            difficulty = ScoreDifficulty.BEGINNER,
            category = ScoreCategory.CLASSICAL,
            description = "ملودی مشهور سمفونی ۹ بتهوون مناسب نوآموزان ویولن",
            version = 1,
            file = "sheets/classical/ode-to-joy.musicxml",
            isOffline = true
        ) to SAMPLE_ODE_TO_JOY_XML,
        ScoreMetadata(
            id = "morgh-e-sahar",
            title = "مرغ سحر (ماهور)",
            composer = "مرتضی نی‌داوود",
            difficulty = ScoreDifficulty.INTERMEDIATE,
            category = ScoreCategory.IRANIAN,
            description = "تصنیف ماندگار موسیقی اصیل ایرانی در دستگاه ماهور",
            version = 1,
            file = "sheets/iranian/morgh-e-sahar.musicxml",
            isOffline = true
        ) to SAMPLE_MORGH_E_SAHAR_XML,
        ScoreMetadata(
            id = "d-major-scale",
            title = "اتود گام ر ماژور و آرپژ",
            composer = "اتود آموزشی",
            difficulty = ScoreDifficulty.BEGINNER,
            category = ScoreCategory.EXERCISES,
            description = "تمرین پایه انگشت‌گذاری سیم ر و لا در پوزیسیون اول ویولن",
            version = 1,
            file = "sheets/exercises/d-major-scale.musicxml",
            isOffline = true
        ) to SAMPLE_D_MAJOR_SCALE_XML
    )
}
