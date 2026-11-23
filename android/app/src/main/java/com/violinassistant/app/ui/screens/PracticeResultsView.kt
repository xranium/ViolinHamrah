package com.violinassistant.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.violinassistant.app.localization.Fa
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

    val stars = when {
        score >= 90 -> "⭐⭐⭐"
        score >= 70 -> "⭐⭐"
        score >= 50 -> "⭐"
        else -> "🎯"
    }

    val feedbackText = when {
        score >= 85 -> "اجرای بسیار دقیق و کم‌نظیر"
        score >= 65 -> "پیشرفت خوب، به تمرین ادامه دهید"
        else -> "نیاز به تمرین بیشتر روی ریتم و کوک"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.bgCard)
            .border(1.dp, colors.borderColor, RoundedCornerShape(20.dp))
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Circular Achievement Score Ring
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(scoreColor.copy(alpha = 0.12f))
                .border(3.dp, scoreColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${Formatters.toPersianDigits(score)}٪",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = scoreColor
                )
                Text(text = stars, fontSize = 12.sp)
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = Fa.SESSION_FINISHED,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = feedbackText,
                fontSize = 12.sp,
                color = colors.textSecondary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    tint = if (colors.isDark) Color.Black else Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = Fa.RETRY,
                    fontWeight = FontWeight.Bold,
                    color = if (colors.isDark) Color.Black else Color.White
                )
            }

            OutlinedButton(
                onClick = onBackToLibrary,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderColor)
            ) {
                Text("کتابخانه", fontWeight = FontWeight.Bold, color = colors.textPrimary)
            }
        }
    }
}
