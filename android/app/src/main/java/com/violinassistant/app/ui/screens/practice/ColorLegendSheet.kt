package com.violinassistant.app.ui.screens.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.violinassistant.app.ui.theme.AppTheme

private data class NoteColorGuideItem(
    val color: Color,
    val title: String,
    val description: String
)

private val NOTE_COLOR_GUIDE = listOf(
    NoteColorGuideItem(Color(0xFF2563EB), "آبی — نت فعال", "نشانگر نت فعلی که باید اجرا شود."),
    NoteColorGuideItem(Color(0xFF10B981), "سبز — اجرای صحیح", "کوک و دیرند نت در محدوده استاندارد است."),
    NoteColorGuideItem(Color(0xFFF59E0B), "نارنجی — خطای زمان‌بندی", "کوک نت درست بوده اما کشش آن کوتاه یا نامتناسب است."),
    NoteColorGuideItem(Color(0xFFEF4444), "قرمز — نت یا کوک اشتباه", "نت اشتباه است یا بیشتر زمان نت خارج از محدوده کوک اجرا شده.")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorLegendSheet(onDismiss: () -> Unit) {
    val colors = AppTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.bgCard,
        contentColor = colors.textPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "راهنمای رنگ نت‌ها",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            NOTE_COLOR_GUIDE.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(item.color)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.textPrimary
                        )
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}
