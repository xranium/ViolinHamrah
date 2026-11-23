package com.violinassistant.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.violinassistant.app.data.PracticeSessionEntity
import com.violinassistant.app.localization.Fa
import com.violinassistant.app.ui.theme.AppTheme
import com.violinassistant.app.util.Formatters

@Composable
fun HistoryScreen(
    sessions: List<PracticeSessionEntity>,
    onOpenThemeDialog: () -> Unit = {}
) {
    val colors = AppTheme.colors
    val totalMins = sessions.sumOf { it.durationSec } / 60
    val bestScore = sessions.maxOfOrNull { it.score } ?: 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgPrimary)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = Fa.HISTORY_TITLE,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.primary.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${Formatters.toPersianDigits(sessions.size)} تمرین",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary
                        )
                    }
                }

                IconButton(
                    onClick = onOpenThemeDialog,
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(colors.bgCard)
                ) {
                    Icon(Icons.Default.Palette, contentDescription = "پوسته", tint = colors.primary, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Summary Bento Strip
        if (sessions.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HistoryStatCard(
                        icon = "⏱️",
                        title = "زمان کل",
                        value = "${Formatters.toPersianDigits(totalMins)} دقیقه",
                        modifier = Modifier.weight(1f)
                    )
                    HistoryStatCard(
                        icon = "🏆",
                        title = "بهترین امتیاز",
                        value = "${Formatters.toPersianDigits(bestScore)}٪",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (sessions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.bgCard)
                        .border(1.dp, colors.borderColor, RoundedCornerShape(14.dp))
                        .padding(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = Fa.NO_HISTORY, color = colors.textSecondary, fontSize = 13.sp)
                }
            }
        } else {
            items(sessions) { session ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.bgCard)
                        .border(1.dp, colors.borderColor, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text(text = session.scoreTitle, fontWeight = FontWeight.Bold, color = colors.textPrimary, fontSize = 13.sp)
                            Text(
                                text = "${Formatters.formatToJalali(session.date)} • ${Formatters.formatDurationHuman(session.durationSec)}",
                                fontSize = 11.sp,
                                color = colors.textSecondary
                            )
                        }
                    }

                    val badgeColor = if (session.score >= 80) colors.accent else colors.warning
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${Formatters.toPersianDigits(session.score)}٪",
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryStatCard(icon: String, title: String, value: String, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.bgCard)
            .border(1.dp, colors.borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = icon, fontSize = 18.sp)
        Column {
            Text(text = title, fontSize = 10.sp, color = colors.textSecondary)
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        }
    }
}
