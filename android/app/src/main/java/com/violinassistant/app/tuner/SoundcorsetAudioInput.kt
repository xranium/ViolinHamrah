package com.violinassistant.app.tuner

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Process

/**
 * Hardware AudioRecord capture loop matching Soundcorset's AndroidAudioInput & HasRecorder
 * (48000Hz preferred preset fallback, 4410-sample frame chunks, UNPROCESSED/VOICE_RECOGNITION).
 */
class SoundcorsetAudioInput {
    companion object {
        const val READ_BUFFER_SIZE = 4410
        private val SAMPLE_RATES = intArrayOf(48000, 44100, 22050, 16000, 32000, 11025, 24000, 8000)
        private const val SHORT_TO_NORM = 1.0 / 32767.0
    }

    @Volatile private var running = false
    private var captureThread: Thread? = null
    private var activeRecord: AudioRecord? = null

    val isRunning: Boolean
        get() = running

    @SuppressLint("MissingPermission")
    fun start(
        onStarted: (sampleRate: Int) -> Unit,
        onFrame: (samples: DoubleArray) -> Unit
    ): Boolean {
        if (running) return true
        val (record, resolvedRate) = openBestAudioRecord() ?: return false
        activeRecord = record
        running = true
        onStarted(resolvedRate)

        val started = runCatching { record.startRecording() }.isSuccess
        if (!started) {
            stop()
            return false
        }

        val thread = Thread({
            runCatching { Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO) }
            val shortBuf = ShortArray(READ_BUFFER_SIZE)
            val doubleBuf = DoubleArray(READ_BUFFER_SIZE)
            try {
                while (running) {
                    val read = record.read(shortBuf, 0, READ_BUFFER_SIZE)
                    if (read > 0) {
                        for (i in 0 until read) {
                            doubleBuf[i] = shortBuf[i] * SHORT_TO_NORM
                        }
                        val frame = if (read == READ_BUFFER_SIZE) doubleBuf else doubleBuf.copyOf(read)
                        onFrame(frame)
                    }
                }
            } finally {
                runCatching {
                    record.stop()
                    record.release()
                }
            }
        }, "SoundcorsetAudioThread")
        captureThread = thread
        thread.start()
        return true
    }

    fun stop() {
        running = false
        val rec = activeRecord
        activeRecord = null
        if (rec != null) {
            runCatching { rec.stop() }
        }
        val t = captureThread
        captureThread = null
        if (t != null && t !== Thread.currentThread()) {
            runCatching { t.join(300) }
        }
    }

    @SuppressLint("MissingPermission")
    private fun openBestAudioRecord(): Pair<AudioRecord, Int>? {
        val brand = Build.BRAND.lowercase()
        val source = if (brand == "oneplus" || brand == "oppo") {
            MediaRecorder.AudioSource.UNPROCESSED
        } else {
            MediaRecorder.AudioSource.VOICE_RECOGNITION
        }
        for (rate in SAMPLE_RATES) {
            val minBytes = AudioRecord.getMinBufferSize(
                rate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBytes <= 0) continue
            val bufBytes = maxOf(READ_BUFFER_SIZE * 2, minBytes)
            val rec = AudioRecord(
                source,
                rate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufBytes
            )
            if (rec.state == AudioRecord.STATE_INITIALIZED) {
                return rec to rate
            }
            rec.release()
        }
        return null
    }
}
