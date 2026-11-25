package com.violinassistant.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin

enum class CountdownPhase {
    IDLE,
    COUNT_3,
    COUNT_2,
    COUNT_1,
    GO,
    LISTENING
}

data class MetronomeState(
    val tempoBpm: Float = 80f,
    val beatsPerMeasure: Int = 4,
    val isPlaying: Boolean = false,
    val isMuted: Boolean = false,
    val isHeld: Boolean = false,
    val currentBeatInMeasure: Int = 0,
    val beatPulseCount: Long = 0L,
    val countdownPhase: CountdownPhase = CountdownPhase.IDLE
)

/**
 * Abstract PCM sink enabling deterministic JVM unit testing without hardware AudioTrack.
 */
interface MetronomeAudioSink {
    val sampleRate: Int
    fun open(): Boolean
    fun writeSamples(buffer: ShortArray, count: Int)
    fun playbackHeadFrames(): Long
    fun close()
}

class AndroidMetronomeAudioSink(
    override val sampleRate: Int = 44100
) : MetronomeAudioSink {
    private var track: AudioTrack? = null

    override fun open(): Boolean {
        val minBytes = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(2048)
        val bufferBytes = maxOf(minBytes, 4096)
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferBytes)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        if (audioTrack.state != AudioTrack.STATE_INITIALIZED) {
            audioTrack.release()
            return false
        }
        track = audioTrack
        audioTrack.play()
        return true
    }

    override fun writeSamples(buffer: ShortArray, count: Int) {
        track?.write(buffer, 0, count)
    }

    override fun playbackHeadFrames(): Long =
        (track?.playbackHeadPosition?.toLong() ?: 0L) and 0xFFFFFFFFL

    override fun close() {
        runCatching {
            track?.pause()
            track?.flush()
            track?.release()
        }
        track = null
    }
}

internal object MetronomeWaveforms {
    fun loadRawPcm16(context: Context, resId: Int): ShortArray {
        val bytes = context.resources.openRawResource(resId).use { it.readBytes() }
        val count = bytes.size / 2
        val shorts = ShortArray(count)
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
        return shorts
    }

    fun synthesizeWoodblock(
        sampleRate: Int,
        freqHz: Double,
        amplitude: Double
    ): ShortArray {
        val length = (sampleRate * 0.018).roundToInt().coerceAtLeast(256)
        val out = ShortArray(length)
        for (i in 0 until length) {
            val t = i.toDouble() / sampleRate
            val env = exp(-t * 240.0) * minOf(1.0, i / 12.0)
            val tone = 0.8 * sin(2.0 * PI * freqHz * t) + 0.2 * sin(2.0 * PI * freqHz * 2.02 * t)
            out[i] = (tone * env * amplitude * Short.MAX_VALUE)
                .roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
        return out
    }
}
