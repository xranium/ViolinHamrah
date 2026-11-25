package com.violinassistant.app.ui.viewmodel

import com.violinassistant.app.data.PracticeSessionDao
import com.violinassistant.app.data.PracticeSessionEntity
import com.violinassistant.app.data.ScoreDao
import com.violinassistant.app.data.ScoreEntity
import com.violinassistant.app.musicxml.MusicXmlParser
import com.violinassistant.app.musicxml.MxlReader
import com.violinassistant.app.practice.PracticeEngineState
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal object ScoreRepositoryHelper {
    suspend fun saveSession(
        sessionDao: PracticeSessionDao,
        score: ScoreEntity,
        state: PracticeEngineState
    ) {
        val isoDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())
        val entity = PracticeSessionEntity(
            id = "sess-${System.currentTimeMillis()}",
            scoreId = score.id,
            scoreTitle = score.title,
            date = isoDate,
            durationSec = state.sessionDurationSec,
            score = state.overallScore ?: 0,
            completedNotes = state.evaluations.size,
            totalNotes = state.totalNotes
        )
        sessionDao.insertSession(entity)
    }

    suspend fun importScore(
        scoreDao: ScoreDao,
        inputStream: InputStream,
        fileName: String
    ): String {
        val bytes = inputStream.readBytes()
        val isZip = bytes.size >= 4 &&
            bytes[0] == 0x50.toByte() &&
            bytes[1] == 0x4B.toByte()
        val xml = if (fileName.endsWith(".mxl", ignoreCase = true) || isZip) {
            MxlReader.extractXml(bytes.inputStream())
        } else {
            String(bytes, Charsets.UTF_8)
        }

        val title = fileName.substringBeforeLast('.')
        val parsed = MusicXmlParser.parse(xml)
        val entity = ScoreEntity(
            id = "custom-${System.currentTimeMillis()}",
            title = if (parsed.metadata.title != "قطعه بدون عنوان") parsed.metadata.title else title,
            composer = parsed.metadata.composer,
            difficulty = parsed.metadata.difficulty.name,
            category = "EXERCISES",
            description = "فایل وارد شده توسط کاربر",
            version = 1,
            xmlContent = xml,
            isOffline = true
        )
        scoreDao.insertOrUpdate(entity)
        return "قطعه «${entity.title}» با موفقیت افزوده شد."
    }
}
