package com.violinassistant.app.config

import com.violinassistant.app.model.AppSettings
import com.violinassistant.app.model.PracticeMode

/**
 * پیکربندی و تنظیمات پایه برنامه قبل از بیلد APK.
 * این مقادیر بدون نیاز به منوی تنظیمات درون برنامه، توسط توسعه‌دهنده به راحتی در اینجا قابل تغییر هستند.
 */
object AppConfig {
    /**
     * کوک مبنای فرکانس نت لا (A4):
     * - 440 هرتز (استاندارد بین‌المللی)
     * - 442 هرتز (مورد علاقه ارکسترها و سولیست‌ها)
     */
    const val TUNING_REF_HZ = 440

    /**
     * حداکثر انحراف مجاز فرکانس بر حسب سِنت (Cents):
     * مقدار پیش‌فرض: 30 سِنت
     */
    const val PITCH_TOLERANCE_CENTS = 50
    const val ACCEPTED_PITCH_BAND_CENTS = 150

    /**
     * درصد تلورانس و خطای مجاز برای کشش زمانی (دیرند) نتها:
     * مقدار پیش‌فرض: 45 درصد (سازگارتر با لرزش آرشه و نوازندگی مبتدی)
     */
    const val DURATION_TOLERANCE_PERCENT = 45

    /**
     * حالت پیش‌فرض تمرین:
     * PracticeMode.GUIDED (هدایت‌شده، پیشروی پس از اجرای صحیح)
     */
    val PRACTICE_MODE = PracticeMode.GUIDED

    // ==========================================
    // تنظیمات مخزن گیت‌هاب جهت دریافت نتهای جدید MusicXML
    // ==========================================
    const val GITHUB_OWNER = "xranium"
    const val GITHUB_REPO = "ViolinHamrah"
    const val GITHUB_BRANCH = "main"
    const val GITHUB_MANIFEST_PATH = "musicxml/manifest.json"

    fun toAppSettings(): AppSettings = AppSettings(
        tuningRefHz = TUNING_REF_HZ,
        practiceMode = PRACTICE_MODE,
        durationTolerancePercent = DURATION_TOLERANCE_PERCENT,
        githubOwner = GITHUB_OWNER,
        githubRepo = GITHUB_REPO,
        githubBranch = GITHUB_BRANCH,
        manifestPath = GITHUB_MANIFEST_PATH
    )
}
