package com.violinassistant.app.ui.screens.tuner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.violinassistant.app.tuner.TunerUiState

/**
 * Soundcorset-exact Tuner Tab (live tuner UI + calibration dialog only; no metronome/recorder/ads).
 */
@Composable
fun TunerScreen(
    state: TunerUiState,
    onSetTunerActive: (active: Boolean, hasMicPermission: Boolean) -> Unit,
    onUpdateSettings: (a4Hz: Float, useFlat: Boolean, useSolfege: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val prefs = remember { context.getSharedPreferences("soundcorset_tuner_prefs", Context.MODE_PRIVATE) }

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        onUpdateSettings(
            prefs.getFloat("a4_hz", 440f),
            prefs.getBoolean("use_flat", false),
            prefs.getBoolean("use_solfege", false)
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        onSetTunerActive(granted, granted)
    }

    DisposableEffect(lifecycleOwner, hasMicPermission) {
        onSetTunerActive(hasMicPermission, hasMicPermission)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    val grantedNow = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    hasMicPermission = grantedNow
                    onSetTunerActive(grantedNow, grantedNow)
                }
                Lifecycle.Event.ON_PAUSE -> onSetTunerActive(false, hasMicPermission)
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            onSetTunerActive(false, hasMicPermission)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(SoundcorsetPalette.PitchDisplayBottom)
                .padding(6.dp)
        ) {
            if (!hasMicPermission) {
                TunerMicPermissionCard(
                    onRequestPermission = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    val isPortrait = maxHeight >= maxWidth
                    if (isPortrait) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            TunerGaugeCanvas(
                                state = state,
                                onOpenSettings = { showSettings = true },
                                modifier = Modifier.fillMaxWidth().weight(6.0f)
                            )
                            TunerNoteCard(
                                state = state,
                                isPortrait = true,
                                modifier = Modifier.fillMaxWidth().weight(4.0f)
                            )
                            TunerPitchGraph(
                                history = state.pitchHistory,
                                modifier = Modifier.fillMaxWidth().weight(2.5f)
                            )
                            TunerVolumeEnvelope(
                                envelope = state.volumeEnvelope,
                                modifier = Modifier.fillMaxWidth().weight(1.0f)
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxHeight().weight(1.0f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                TunerGaugeCanvas(
                                    state = state,
                                    onOpenSettings = { showSettings = true },
                                    modifier = Modifier.fillMaxWidth().weight(1.8f)
                                )
                                TunerPitchGraph(
                                    history = state.pitchHistory,
                                    modifier = Modifier.fillMaxWidth().weight(1.0f)
                                )
                            }
                            Column(
                                modifier = Modifier.fillMaxHeight().weight(0.81f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                TunerNoteCard(
                                    state = state,
                                    isPortrait = false,
                                    modifier = Modifier.fillMaxWidth().weight(7.0f)
                                )
                                TunerVolumeEnvelope(
                                    envelope = state.volumeEnvelope,
                                    modifier = Modifier.fillMaxWidth().weight(1.0f)
                                )
                            }
                        }
                    }
                }
            }

            if (showSettings) {
                TunerSettingsDialog(
                    currentA4Hz = state.a4Hz,
                    currentUseFlat = state.useFlat,
                    currentUseSolfege = state.useSolfege,
                    onApply = { newA4, newFlat, newSolfege ->
                        prefs.edit()
                            .putFloat("a4_hz", newA4)
                            .putBoolean("use_flat", newFlat)
                            .putBoolean("use_solfege", newSolfege)
                            .apply()
                        onUpdateSettings(newA4, newFlat, newSolfege)
                    },
                    onDismiss = { showSettings = false }
                )
            }
        }
    }
}
