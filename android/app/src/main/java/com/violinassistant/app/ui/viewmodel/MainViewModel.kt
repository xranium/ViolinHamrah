package com.violinassistant.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.violinassistant.app.audio.AudioRecordProcessor
import com.violinassistant.app.config.AppConfig
import com.violinassistant.app.data.AppDatabase
import com.violinassistant.app.data.GitHubSyncService
import com.violinassistant.app.data.PracticeSessionEntity
import com.violinassistant.app.data.ScoreEntity
import com.violinassistant.app.model.*
import com.violinassistant.app.musicxml.MxlReader
import com.violinassistant.app.musicxml.MusicXmlParser
import com.violinassistant.app.practice.PracticeEngine
import com.violinassistant.app.practice.PracticeEngineState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val scoreDao = database.scoreDao()
    private val sessionDao = database.practiceSessionDao()
    private val syncService = GitHubSyncService(scoreDao)
    private val audioProcessor = AudioRecordProcessor()

    val scores: StateFlow<List<ScoreEntity>> = scoreDao.getAllScoresFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessions: StateFlow<List<PracticeSessionEntity>> = sessionDao.getAllSessionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: AppSettings = AppConfig.toAppSettings()

    private val _selectedScore = MutableStateFlow<ScoreEntity?>(null)
    val selectedScore: StateFlow<ScoreEntity?> = _selectedScore.asStateFlow()

    private val _parsedScore = MutableStateFlow<ParsedScore?>(null)
    val parsedScore: StateFlow<ParsedScore?> = _parsedScore.asStateFlow()

    private val _engineState = MutableStateFlow(PracticeEngineState())
    val engineState: StateFlow<PracticeEngineState> = _engineState.asStateFlow()

    private val _isPracticing = MutableStateFlow(false)
    val isPracticing: StateFlow<Boolean> = _isPracticing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private val _micError = MutableStateFlow<String?>(null)
    val micError: StateFlow<String?> = _micError.asStateFlow()

    private var practiceEngine: PracticeEngine? = null

    init {
        viewModelScope.launch {
            syncService.seedBuiltInScoresIfEmpty()
        }
    }

    fun selectScore(score: ScoreEntity) {
        _selectedScore.value = score
        try {
            val parsed = MusicXmlParser.parse(score.xmlContent)
            _parsedScore.value = parsed
            val engine = PracticeEngine(
                notes = parsed.notes,
                tolerancePercent = settings.durationTolerancePercent,
                practiceMode = settings.practiceMode,
                onStateChange = { newState ->
                    _engineState.value = newState
                    if (newState.status.name == "FINISHED" && newState.overallScore != null) {
                        saveSessionRecord(score, newState)
                        stopPractice()
                    }
                }
            )
            practiceEngine = engine
            _engineState.value = engine.getState()
        } catch (e: Exception) {
            _micError.value = "خطا در پردازش نت: ${e.message}"
        }
    }

    fun togglePractice() {
        if (_isPracticing.value) {
            stopPractice()
        } else {
            startPractice()
        }
    }

    private fun startPractice() {
        _micError.value = null
        try {
            audioProcessor.updateTuningRef(settings.tuningRefHz)
            audioProcessor.start { pitchResult ->
                practiceEngine?.processAudioInput(pitchResult)
            }
            _isPracticing.value = true
        } catch (e: Exception) {
            _micError.value = e.message ?: "خطا در فعال‌سازی میکروفون"
            _isPracticing.value = false
        }
    }

    fun stopPractice() {
        audioProcessor.stop()
        _isPracticing.value = false
    }

    fun resetPractice() {
        practiceEngine?.reset()
        _engineState.value = practiceEngine?.getState() ?: PracticeEngineState()
    }

    private fun saveSessionRecord(score: ScoreEntity, state: PracticeEngineState) {
        viewModelScope.launch(Dispatchers.IO) {
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
    }

    fun importCustomScore(inputStream: InputStream, fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
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
                _syncMessage.value = "قطعه «${entity.title}» با موفقیت افزوده شد."
            } catch (e: Exception) {
                _syncMessage.value = "خطا در وارد کردن فایل: ${e.message}"
            }
        }
    }

    fun syncWithGitHub() {
        viewModelScope.launch {
            _syncMessage.value = "در حال ارتباط با مخزن گیت‌هاب..."
            val result = syncService.syncWithGitHub(settings)
            _syncMessage.value = result.message
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioProcessor.stop()
    }
}
