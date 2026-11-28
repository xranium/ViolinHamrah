package com.violinassistant.app.ui.screens.practice

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.violinassistant.app.ui.theme.AppTheme

private data class NoteColorGuideItem(
    val noteColor: Color,
    val showBlueCursor: Boolean,
    val title: String,
    val description: String
)

private val NOTE_COLOR_GUIDE = listOf(
    NoteColorGuideItem(Color(0xFF111827), true, "نشانگر آبی + نت مشکی", "نت فعلی است که باید اکنون اجرا شود."),
    NoteColorGuideItem(Color(0xFF10B981), false, "نت سبز — اجرای صحیح", "کوک و زمان‌بندی نت در محدوده استاندارد است."),
    NoteColorGuideItem(Color(0xFFF59E0B), false, "نت نارنجی — خطای زمان‌بندی", "کوک نت درست بوده اما کشش آن کوتاه یا نامتناسب است."),
    NoteColorGuideItem(Color(0xFFEF4444), false, "نت قرمز — نت یا کوک اشتباه", "نت اشتباه است یا بیشتر زمان نت خارج از محدوده کوک اجرا شده.")
)

@Composable
private fun MiniStaffNotePreview(
    noteColor: Color,
    showBlueCursor: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    Canvas(
        modifier = modifier
            .size(width = 56.dp, height = 38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFFAF9F5))
            .border(1.dp, colors.borderColor, RoundedCornerShape(8.dp))
    ) {
        val leftPad = 6.dp.toPx()
        val rightPad = size.width - 6.dp.toPx()
        val topY = 8.dp.toPx()
        val stepY = (size.height - 16.dp.toPx()) / 4f
        val lineColor = Color(0xFF1E293B).copy(alpha = 0.28f)

        // 5 horizontal staff lines
        for (i in 0..4) {
            val y = topY + i * stepY
            drawLine(lineColor, Offset(leftPad, y), Offset(rightPad, y), strokeWidth = 1.dp.toPx())
        }

        // Notehead on 2nd line from bottom + upward stem
        val noteCenter = Offset(size.width * 0.56f, topY + 3f * stepY)
        val headW = 8.5.dp.toPx()
        val headH = 6.dp.toPx()
        rotate(degrees = -20f, pivot = noteCenter) {
            drawOval(
                color = noteColor,
                topLeft = Offset(noteCenter.x - headW / 2f, noteCenter.y - headH / 2f),
                size = Size(headW, headH)
            )
        }
        val stemX = noteCenter.x + headW * 0.42f
        drawLine(
            color = noteColor,
            start = Offset(stemX, noteCenter.y),
            end = Offset(stemX, topY - 2.dp.toPx()),
            strokeWidth = 1.6.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Active blue vertical cursor beside the black note
        if (showBlueCursor) {
            val cursorX = noteCenter.x - headW * 0.95f
            val cursorBlue = Color(0xFF2563EB)
            drawLine(
                color = cursorBlue.copy(alpha = 0.28f),
                start = Offset(cursorX, 4.dp.toPx()),
                end = Offset(cursorX, size.height - 4.dp.toPx()),
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = cursorBlue,
                start = Offset(cursorX, 4.dp.toPx()),
                end = Offset(cursorX, size.height - 4.dp.toPx()),
                strokeWidth = 2.2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

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
                    MiniStaffNotePreview(
                        noteColor = item.noteColor,
                        showBlueCursor = item.showBlueCursor
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
