package com.violinassistant.app.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.violinassistant.app.model.PitchResult
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

class AudioRecordProcessor(
    private var tuningRefHz: Int = 440
) {
    companion object {
        const val SAMPLE_RATE = 44100
        const val BUFFER_SIZE_FRAMES = 2048
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val isRecording = AtomicBoolean(false)
    private val pitchDetector = ViolinPitchDetector(tuningRefHz)

    fun updateTuningRef(refHz: Int) {
        this.tuningRefHz = refHz
        pitchDetector.setTuningRef(refHz)
    }

    @SuppressLint("MissingPermission")
    fun start(onPitch: (PitchResult?) -> Unit) {
        if (isRecording.get()) return

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

            // Highpass filter state (180 Hz cutoff at 44100 sample rate)
            // y[i] = alpha * (y[i-1] + x[i] - x[i-1])
            val rc = 1.0f / (2.0f * Math.PI.toFloat() * 180.0f)
            val dt = 1.0f / SAMPLE_RATE.toFloat()
            val alpha = rc / (rc + dt)
            var prevX = 0f
            var prevY = 0f

            while (isActive && isRecording.get()) {
                val readCount = record.read(shortBuffer, 0, BUFFER_SIZE_FRAMES)
                if (readCount > 0) {
                    for (i in 0 until readCount) {
                        val rawSample = shortBuffer[i] / 32768.0f
                        val filtered = alpha * (prevY + rawSample - prevX)
                        prevX = rawSample
                        prevY = filtered
                        floatBuffer[i] = filtered
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
