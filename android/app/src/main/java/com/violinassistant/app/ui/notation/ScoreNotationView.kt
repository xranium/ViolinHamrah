package com.violinassistant.app.ui.notation

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.viewinterop.AndroidView
import com.violinassistant.app.model.NoteStatus

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ScoreNotationView(
    xmlContent: String,
    currentNoteIndex: Int,
    currentStatus: NoteStatus,
    modifier: Modifier = Modifier,
    noteStatuses: Map<Int, NoteStatus> = emptyMap()
) {
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isLoaded by remember { mutableStateOf(false) }
    val completedColors = remember { mutableStateMapOf<Int, String>() }
    var prevIndex by remember { mutableStateOf(0) }
    var prevStatus by remember { mutableStateOf(currentStatus) }

    // Musical notation must be strictly LTR
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(ComposeColor(0xFFFAF9F5))
        ) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(Color.parseColor("#faf9f5"))
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            allowFileAccess = true
                            cacheMode = WebSettings.LOAD_NO_CACHE
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoaded = true
                                if (xmlContent.isNotBlank()) {
                                    val escaped = xmlContent
                                        .replace("\\", "\\\\")
                                        .replace("`", "\\`")
                                        .replace("$", "\\$")
                                    view?.evaluateJavascript("window.loadScore(`$escaped`);", null)
                                }
                            }
                        }
                        webChromeClient = WebChromeClient()
                        loadUrl("file:///android_asset/notation/viewer.html")
                        webViewInstance = this
                    }
                },
                update = { webView ->
                    if (isLoaded && xmlContent.isNotBlank()) {
                        // Permanent SVG note coloring for accepted notes
                        if (noteStatuses.isNotEmpty()) {
                            noteStatuses.forEach { (idx, status) ->
                                val color = when (status) {
                                    NoteStatus.CORRECT -> "#10b981"
                                    NoteStatus.WRONG_DURATION -> "#f59e0b"
                                    else -> null
                                }
                                if (color != null && completedColors[idx] != color) {
                                    completedColors[idx] = color
                                    webView.evaluateJavascript("window.setNoteColor($idx, '$color');", null)
                                }
                            }
                        } else if (currentNoteIndex > prevIndex) {
                            for (idx in prevIndex until currentNoteIndex) {
                                val color = if (prevStatus == NoteStatus.WRONG_DURATION) "#f59e0b" else "#10b981"
                                if (completedColors[idx] != color) {
                                    completedColors[idx] = color
                                    webView.evaluateJavascript("window.setNoteColor($idx, '$color');", null)
                                }
                            }
                        } else if (currentNoteIndex == 0 && prevIndex > 0) {
                            completedColors.clear()
                            webView.evaluateJavascript("window.resetNoteColors();", null)
                        }
                        prevIndex = currentNoteIndex
                        prevStatus = currentStatus

                        val cursorColorHex = when (currentStatus) {
                            NoteStatus.CORRECT -> "#10b981"
                            NoteStatus.WRONG_PITCH -> "#ef4444"
                            NoteStatus.WRONG_DURATION -> "#f59e0b"
                            else -> "#2563eb"
                        }
                        webView.evaluateJavascript(
                            "window.setCursor($currentNoteIndex, '$cursorColorHex');",
                            null
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    // When XML changes, reload score and clear cached note colors
    LaunchedEffect(xmlContent, isLoaded) {
        if (isLoaded && xmlContent.isNotBlank() && webViewInstance != null) {
            completedColors.clear()
            val escaped = xmlContent
                .replace("\\", "\\\\")
                .replace("`", "\\`")
                .replace("$", "\\$")
            webViewInstance?.evaluateJavascript("window.loadScore(`$escaped`);", null)
        }
    }
}
