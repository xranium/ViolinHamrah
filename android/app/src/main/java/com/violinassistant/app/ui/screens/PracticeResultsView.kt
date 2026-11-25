package com.violinassistant.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.violinassistant.app.localization.Fa
import com.violinassistant.app.ui.components.ScoreArcGauge
import com.violinassistant.app.ui.theme.AppTheme
import com.violinassistant.app.util.Formatters

@Composable
fun PracticeResultsView(
    score: Int,
    onRetry: () -> Unit,
    onBackToLibrary: () -> Unit
) {
    val colors = AppTheme.colors
    val scoreColor = when {
        score >= 80 -> colors.accent
        score >= 60 -> colors.warning
        else -> colors.error
    }

    val starCount = when {
        score >= 90 -> 3
        score >= 70 -> 2
        score >= 40 -> 1
        else -> 0
    }

    val feedbackText = when {
        score >= 85 -> "اجرای دقیق و باثبات"
        score >= 65 -> "روند رو به رشد، ادامه دهید"
        else -> "تمرین آرام‌تر با مترونوم توصیه می‌شود"
    }

    Surface(
        color = colors.bgCard,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(modifier = Modifier.size(112.dp), contentAlignment = Alignment.Center) {
                ScoreArcGauge(
                    score = score,
                    trackColor = colors.borderColor,
                    progressColor = scoreColor,
                    modifier = Modifier.fillMaxSize()
                )
                Text(
                    text = "${Formatters.toPersianDigits(score)}٪",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = scoreColor
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (i in 1..3) {
                    Icon(
                        imageVector = if (i <= starCount) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                        contentDescription = null,
                        tint = if (i <= starCount) colors.primary else colors.textMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = Fa.SESSION_FINISHED, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                Text(text = feedbackText, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = Fa.RETRY, style = MaterialTheme.typography.labelLarge)
                }

                OutlinedButton(
                    onClick = onBackToLibrary,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
                    border = BorderStroke(1.dp, colors.borderColor)
                ) {
                    Text(text = "کتابخانه", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
