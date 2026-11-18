package com.violinassistant.app.ui.notation

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.violinassistant.app.model.NoteStatus

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ScoreNotationView(
    xmlContent: String,
    currentNoteIndex: Int,
    currentStatus: NoteStatus,
    modifier: Modifier = Modifier
) {
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isLoaded by remember { mutableStateOf(false) }

    // Musical notation must be strictly LTR
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = modifier
                .shadow(elevation = 3.dp, shape = RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .background(ComposeColor.White)
                .border(1.dp, ComposeColor(0xFFE2E8F0), RoundedCornerShape(10.dp))
        ) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(Color.WHITE)
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
                        val colorHex = when (currentStatus) {
                            NoteStatus.CORRECT -> "#10b981"
                            NoteStatus.WRONG_PITCH -> "#ef4444"
                            NoteStatus.WRONG_DURATION -> "#ef4444"
                            else -> "#2563eb"
                        }
                        webView.evaluateJavascript(
                            "window.setCursor($currentNoteIndex, '$colorHex');",
                            null
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    // When XML changes, reload score
    LaunchedEffect(xmlContent, isLoaded) {
        if (isLoaded && xmlContent.isNotBlank() && webViewInstance != null) {
            val escaped = xmlContent
                .replace("\\", "\\\\")
                .replace("`", "\\`")
                .replace("$", "\\$")
            webViewInstance?.evaluateJavascript("window.loadScore(`$escaped`);", null)
        }
    }
}
