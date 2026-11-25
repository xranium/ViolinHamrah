package com.violinassistant.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.violinassistant.app.audio.AudioRecordProcessor
import com.violinassistant.app.audio.MetronomeEngine
import com.violinassistant.app.config.AppConfig
import com.violinassistant.app.data.AppDatabase
import com.violinassistant.app.data.GitHubSyncService
import com.violinassistant.app.data.PracticeSessionEntity
import com.violinassistant.app.data.ScoreEntity
import com.violinassistant.app.model.*
import com.violinassistant.app.musicxml.MusicXmlParser
import com.violinassistant.app.practice.PracticeAudioCoordinator
import com.violinassistant.app.practice.PracticeEngine
import com.violinassistant.app.practice.PracticeEngineState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.InputStream

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

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private val _micError = MutableStateFlow<String?>(null)
    val micError: StateFlow<String?> = _micError.asStateFlow()

    private var practiceEngine: PracticeEngine? = null

    private val audioCoordinator = PracticeAudioCoordinator(
        metronomeEngine = MetronomeEngine(appContext = application),
        postToMain = { block -> viewModelScope.launch(Dispatchers.Main.immediate) { block() } },
        onStartListening = {
            _micError.value = null
            audioProcessor.updateTuningRef(settings.tuningRefHz)
            audioProcessor.start { pitchResult ->
                practiceEngine?.processAudioInput(pitchResult)
            }
        },
        onStopListening = { audioProcessor.stop() },
        onError = { msg -> _micError.value = msg }
    )

    val isPracticing: StateFlow<Boolean> = audioCoordinator.isPracticing
    val metronomeState = audioCoordinator.metronomeState
    val tunerState = audioCoordinator.tunerState

    init {
        viewModelScope.launch {
            syncService.seedBuiltInScoresIfEmpty()
        }
    }

    fun selectScore(score: ScoreEntity, hasMicPermission: Boolean = false) {
        audioCoordinator.stopPractice()
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
            if (hasMicPermission) {
                audioCoordinator.startPracticeWithCountdown(parsed.tempo, parsed.timeSignature.beats)
            } else {
                audioCoordinator.configureMetronome(parsed.tempo, parsed.timeSignature.beats)
            }
        } catch (e: Exception) {
            _micError.value = "خطا در پردازش نت: ${e.message}"
        }
    }

    fun togglePractice() {
        val parsed = _parsedScore.value
        val tempo = parsed?.tempo ?: 80f
        val beats = parsed?.timeSignature?.beats ?: 4
        if (!isPracticing.value && _engineState.value.status.name == "FINISHED") {
            practiceEngine?.reset()
            _engineState.value = practiceEngine?.getState() ?: PracticeEngineState()
        }
        audioCoordinator.togglePractice(tempo, beats)
    }

    fun startPracticeAfterPermission() {
        if (isPracticing.value) return
        val parsed = _parsedScore.value ?: return
        audioCoordinator.startPracticeWithCountdown(parsed.tempo, parsed.timeSignature.beats)
    }

    fun toggleMetronomeMute() {
        audioCoordinator.toggleMetronomeMute()
    }

    fun stopPractice() {
        audioCoordinator.stopPractice()
    }

    fun resetPractice(startCountdownAfterReset: Boolean = true) {
        audioCoordinator.stopPractice()
        practiceEngine?.reset()
        _engineState.value = practiceEngine?.getState() ?: PracticeEngineState()
        if (startCountdownAfterReset) {
            _parsedScore.value?.let { parsed ->
                audioCoordinator.startPracticeWithCountdown(parsed.tempo, parsed.timeSignature.beats)
            }
        }
    }

    fun setTunerActive(active: Boolean, hasMicPermission: Boolean) {
        audioCoordinator.setTunerActive(active, hasMicPermission)
    }

    fun updateTunerSettings(a4Hz: Float, useFlat: Boolean, useSolfege: Boolean) {
        audioCoordinator.updateTunerSettings(a4Hz, useFlat, useSolfege)
    }

    private fun saveSessionRecord(score: ScoreEntity, state: PracticeEngineState) {
        viewModelScope.launch(Dispatchers.IO) {
            ScoreRepositoryHelper.saveSession(sessionDao, score, state)
        }
    }

    fun importCustomScore(inputStream: InputStream, fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _syncMessage.value = ScoreRepositoryHelper.importScore(scoreDao, inputStream, fileName)
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
        audioCoordinator.releaseAll()
    }
}
