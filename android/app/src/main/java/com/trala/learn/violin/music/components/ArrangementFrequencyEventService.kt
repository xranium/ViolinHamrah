package com.trala.learn.violin.music.components

import android.util.Log

/**
 * Direct JNI bridge to Trala's native audio and pitch detection engine (libtrala-android-native.so).
 *
 * This class mirrors the exact package and class name expected by the C++ JNI exports:
 * - Java_com_trala_learn_violin_music_components_ArrangementFrequencyEventService_ndkInitEngine
 * - Java_com_trala_learn_violin_music_components_ArrangementFrequencyEventService_ndkStartEngine
 * - Java_com_trala_learn_violin_music_components_ArrangementFrequencyEventService_ndkStopEngine
 * - Java_com_trala_learn_violin_music_components_ArrangementFrequencyEventService_ndkGetFrequency
 * - Java_com_trala_learn_violin_music_components_ArrangementFrequencyEventService_ndkStopRecording
 */
class ArrangementFrequencyEventService {

    companion object {
        private const val TAG = "TralaNativeAudio"
        private var isNativeLoaded = false

        init {
            try {
                System.loadLibrary("c++_shared")
                System.loadLibrary("oboe")
                System.loadLibrary("trala-c")
                System.loadLibrary("trala-android-native")
                isNativeLoaded = true
                Log.i(TAG, "Trala native audio libraries successfully loaded.")
            } catch (t: Throwable) {
                isNativeLoaded = false
                Log.w(TAG, "Native library load deferred or unsupported on this architecture: ${t.message}")
            }
        }

        fun isAvailable(): Boolean = isNativeLoaded
    }

    private external fun ndkInitEngine(useYinMethod: Boolean)
    private external fun ndkStartEngine()
    private external fun ndkStopEngine()
    private external fun ndkGetFrequency(): Double
    private external fun ndkStopRecording()

    fun initEngine(useYinMethod: Boolean = false): Boolean {
        if (!isNativeLoaded) return false
        return try {
            ndkInitEngine(useYinMethod)
            true
        } catch (t: Throwable) {
            Log.e(TAG, "ndkInitEngine failed", t)
            false
        }
    }

    fun startEngine(): Boolean {
        if (!isNativeLoaded) return false
        return try {
            ndkStartEngine()
            true
        } catch (t: Throwable) {
            Log.e(TAG, "ndkStartEngine failed", t)
            false
        }
    }

    fun stopEngine() {
        if (!isNativeLoaded) return
        try {
            ndkStopRecording()
            ndkStopEngine()
        } catch (t: Throwable) {
            Log.e(TAG, "ndkStopEngine failed", t)
        }
    }

    fun getFrequency(): Double {
        if (!isNativeLoaded) return -1.0
        return try {
            ndkGetFrequency()
        } catch (t: Throwable) {
            -1.0
        }
    }
}
