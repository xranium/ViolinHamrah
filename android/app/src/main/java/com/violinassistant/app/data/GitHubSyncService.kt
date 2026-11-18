package com.violinassistant.app.data

import com.violinassistant.app.model.AppSettings
import com.violinassistant.app.musicxml.MxlReader
import com.violinassistant.app.musicxml.SampleScores
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class SyncResult(
    val success: Boolean,
    val newCount: Int,
    val updatedCount: Int,
    val message: String
)

class GitHubSyncService(private val scoreDao: ScoreDao) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun seedBuiltInScoresIfEmpty() = withContext(Dispatchers.IO) {
        if (scoreDao.getScoreCount() == 0) {
            val entities = SampleScores.BUILT_IN_SCORES.map { (meta, xml) ->
                ScoreEntity(
                    id = meta.id,
                    title = meta.title,
                    composer = meta.composer,
                    difficulty = meta.difficulty.name,
                    category = meta.category.name,
                    description = meta.description,
                    version = meta.version,
                    xmlContent = xml,
                    isOffline = true
                )
            }
            scoreDao.insertAll(entities)
        }
    }

    suspend fun syncWithGitHub(settings: AppSettings): SyncResult = withContext(Dispatchers.IO) {
        try {
            val cleanOwner = settings.githubOwner.trim()
            val cleanRepo = settings.githubRepo.trim()
            val cleanBranch = settings.githubBranch.trim()
            val cleanPath = settings.manifestPath.trim().removePrefix("/")

            val manifestUrl = "https://raw.githubusercontent.com/$cleanOwner/$cleanRepo/$cleanBranch/$cleanPath"
            val request = Request.Builder().url(manifestUrl).build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext SyncResult(
                    success = false,
                    newCount = 0,
                    updatedCount = 0,
                    message = "پاسخ ناموفق از سرور گیت‌هاب (کد: ${response.code})"
                )
            }

            val bodyText = response.body?.string() ?: return@withContext SyncResult(
                success = false,
                newCount = 0,
                updatedCount = 0,
                message = "محتوای مانیفست خالی است."
            )

            val json = JSONObject(bodyText)
            val sheetsArray = json.optJSONArray("sheets") ?: return@withContext SyncResult(
                success = false,
                newCount = 0,
                updatedCount = 0,
                message = "فرمت نامعتبر مانیفست گیت‌هاب."
            )

            var newCount = 0
            var updatedCount = 0

            for (i in 0 until sheetsArray.length()) {
                val item = sheetsArray.getJSONObject(i)
                val id = item.getString("id")
                val title = item.optString("title", "بدون عنوان")
                val composer = item.optString("composer", "ناشناس")
                val difficulty = item.optString("difficulty", "BEGINNER").uppercase()
                val category = item.optString("category", "CLASSICAL").uppercase()
                val desc = item.optString("description", "")
                val version = item.optInt("version", 1)
                val file = item.optString("file", "")

                val existing = scoreDao.getScoreById(id)
                if (existing == null || existing.version < version) {
                    val fileUrl = "https://raw.githubusercontent.com/$cleanOwner/$cleanRepo/$cleanBranch/$file"
                    val fileReq = Request.Builder().url(fileUrl).build()
                    val fileRes = client.newCall(fileReq).execute()

                    if (fileRes.isSuccessful) {
                        val bodyBytes = fileRes.body?.bytes() ?: byteArrayOf()
                        val isZip = bodyBytes.size >= 4 &&
                            bodyBytes[0] == 0x50.toByte() &&
                            bodyBytes[1] == 0x4B.toByte()

                        val xml = try {
                            if (file.endsWith(".mxl", ignoreCase = true) || isZip) {
                                MxlReader.extractXml(bodyBytes.inputStream())
                            } else {
                                String(bodyBytes, Charsets.UTF_8)
                            }
                        } catch (e: Exception) {
                            ""
                        }

                        if (xml.contains("score-partwise") || xml.contains("score-timewise")) {
                            scoreDao.insertOrUpdate(
                                ScoreEntity(
                                    id = id,
                                    title = title,
                                    composer = composer,
                                    difficulty = difficulty,
                                    category = category,
                                    description = desc,
                                    version = version,
                                    xmlContent = xml,
                                    isOffline = true
                                )
                            )
                            if (existing == null) newCount++ else updatedCount++
                        }
                    }
                }
            }

            SyncResult(
                success = true,
                newCount = newCount,
                updatedCount = updatedCount,
                message = "همگام‌سازی با موفقیت انجام شد ($newCount قطعه جدید، $updatedCount بروزرسانی)."
            )
        } catch (e: Exception) {
            SyncResult(
                success = false,
                newCount = 0,
                updatedCount = 0,
                message = "خطا در اتصال به مخزن گیت‌هاب: ${e.message}"
            )
        }
    }
}
