package com.violinassistant.app.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.trala.learn.violin.music.components.ArrangementFrequencyEventService
import com.violinassistant.app.model.PitchResult
import com.violinassistant.app.musicxml.PitchMath
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

class AudioRecordProcessor(
    private var tuningRefHz: Int = 440
) {
    companion object {
        private const val TAG = "AudioRecordProcessor"
        const val SAMPLE_RATE = 44100
        const val BUFFER_SIZE_FRAMES = 2048
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val isRecording = AtomicBoolean(false)
    private val pitchDetector = ViolinPitchDetector(tuningRefHz)
    private var tralaNativeEngine: ArrangementFrequencyEventService? = null

    fun updateTuningRef(refHz: Int) {
        this.tuningRefHz = refHz
        pitchDetector.setTuningRef(refHz)
    }

    @SuppressLint("MissingPermission")
    fun start(onPitch: (PitchResult?) -> Unit) {
        if (isRecording.get()) return

        if (ArrangementFrequencyEventService.isAvailable()) {
            try {
                val nativeService = ArrangementFrequencyEventService()
                if (nativeService.initEngine(useYinMethod = false) && nativeService.startEngine()) {
                    tralaNativeEngine = nativeService
                    isRecording.set(true)
                    Log.i(TAG, "Using Trala native C++/Oboe voice recognition engine.")

                    recordingJob = CoroutineScope(Dispatchers.Default).launch {
                        while (isActive && isRecording.get()) {
                            val freq = nativeService.getFrequency()
                            val result = if (freq > 50.0 && freq < 3500.0) {
                                val midiCents = PitchMath.frequencyToMidiAndCents(freq.toFloat(), tuningRefHz)
                                val pitchName = PitchMath.midiToPitchName(midiCents.midi)
                                PitchResult(
                                    frequency = freq.toFloat(),
                                    midi = midiCents.midi,
                                    pitchName = pitchName,
                                    centsDeviation = midiCents.cents,
                                    clarity = 0.95f,
                                    volume = 0.5f,
                                    isStable = true
                                )
                            } else {
                                null
                            }

                            withContext(Dispatchers.Main) {
                                onPitch(result)
                            }
                            delay(25)
                        }
                    }
                    return
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Trala native engine initialization failed, falling back: ${t.message}")
            }
        }

        // Fallback: Kotlin AudioRecord processor
        startFallbackEngine(onPitch)
    }

    @SuppressLint("MissingPermission")
    private fun startFallbackEngine(onPitch: (PitchResult?) -> Unit) {
        val minBufSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBufSize, BUFFER_SIZE_FRAMES * 2)

        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            throw IllegalStateException("خطا در راه‌اندازی ورودی میکروفون دستگاه.")
        }

        audioRecord = record
        record.startRecording()
        isRecording.set(true)
        pitchDetector.reset()

        recordingJob = CoroutineScope(Dispatchers.Default).launch {
            val shortBuffer = ShortArray(BUFFER_SIZE_FRAMES)
            val floatBuffer = FloatArray(BUFFER_SIZE_FRAMES)

            while (isActive && isRecording.get()) {
                val readCount = record.read(shortBuffer, 0, BUFFER_SIZE_FRAMES)
                if (readCount > 0) {
                    for (i in 0 until readCount) {
                        floatBuffer[i] = shortBuffer[i] / 32768.0f
                    }
                    val result = pitchDetector.processSamples(floatBuffer, SAMPLE_RATE)
                    withContext(Dispatchers.Main) {
                        onPitch(result)
                    }
                } else {
                    delay(10)
                }
            }
        }
    }

    fun stop() {
        isRecording.set(false)
        recordingJob?.cancel()
        recordingJob = null

        tralaNativeEngine?.stopEngine()
        tralaNativeEngine = null

        try {
            audioRecord?.apply {
                if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    stop()
                }
                release()
            }
        } catch (_: Exception) {}
        audioRecord = null
        pitchDetector.reset()
    }

    fun isListening(): Boolean = isRecording.get()
}
