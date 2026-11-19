package com.violinassistant.app.ui.screens.practice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.violinassistant.app.localization.Fa
import com.violinassistant.app.ui.theme.ErrorRed
import kotlinx.coroutines.delay

/**
 * Transient red error bubble displaying "نت اشتباه!" upon pitch mismatch (Requirement R3).
 * Auto-dismisses after [dismissDelayMs] (default 900ms).
 */
@Composable
fun WrongNoteBubble(
    isWrong: Boolean,
    triggerKey: Any? = null,
    modifier: Modifier = Modifier,
    dismissDelayMs: Long = 900L
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(isWrong, triggerKey) {
        if (isWrong) {
            isVisible = true
            delay(dismissDelayMs)
            isVisible = false
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.85f),
        exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.85f),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(ErrorRed)
                .padding(horizontal = 18.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = Fa.WRONG_NOTE_BUBBLE,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
