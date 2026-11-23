package com.violinassistant.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.violinassistant.app.data.PracticeSessionEntity
import com.violinassistant.app.data.ScoreEntity
import com.violinassistant.app.localization.Fa
import com.violinassistant.app.ui.theme.AppTheme
import com.violinassistant.app.util.Formatters

@Composable
fun HomeScreen(
    scores: List<ScoreEntity>,
    sessions: List<PracticeSessionEntity>,
    onStartPractice: () -> Unit,
    onSelectScore: (ScoreEntity) -> Unit,
    onOpenThemeDialog: () -> Unit = {}
) {
    val colors = AppTheme.colors
    val totalMins = sessions.sumOf { it.durationSec } / 60
    val avgScore = if (sessions.isNotEmpty()) sessions.map { it.score }.average().toInt() else 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgPrimary)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top App Bar with Theme Switcher
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🎻", fontSize = 18.sp)
                    }
                    Text(
                        text = Fa.APP_TITLE,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }

                IconButton(
                    onClick = onOpenThemeDialog,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(colors.bgCard)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "تغییر پوسته",
                        tint = colors.primary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }

        // Hero Card with Bento Stats
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(colors.bgCardHover, colors.bgCard)
                        )
                    )
                    .border(1.dp, colors.borderColor, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "تمرین هوشمند ویولن",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "تشخیص زنده فرکانس و نت‌ها",
                                fontSize = 11.sp,
                                color = colors.textSecondary
                            )
                        }
                    }

                    // Bento Metric Strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BentoPill(
                            icon = "🎼",
                            label = "${Formatters.toPersianDigits(scores.size)} قطعه",
                            modifier = Modifier.weight(1f)
                        )
                        BentoPill(
                            icon = "⏱️",
                            label = "${Formatters.toPersianDigits(totalMins)} دقیقه",
                            modifier = Modifier.weight(1f)
                        )
                        BentoPill(
                            icon = "🎯",
                            label = if (avgScore > 0) "${Formatters.toPersianDigits(avgScore)}٪" else "—",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Button(
                        onClick = onStartPractice,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = if (colors.isDark) Color.Black else Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = Fa.START_PRACTICE,
                            fontWeight = FontWeight.Bold,
                            color = if (colors.isDark) Color.Black else Color.White
                        )
                    }
                }
            }
        }

        // Featured Scores
        item {
            Text(
                text = "قطعات پیشنهادی",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
        }

        items(scores.take(2)) { score ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.bgCard)
                    .border(1.dp, colors.borderColor, RoundedCornerShape(14.dp))
                    .clickable { onSelectScore(score) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(text = score.title, fontWeight = FontWeight.Bold, color = colors.textPrimary, fontSize = 14.sp)
                        Text(text = score.composer, fontSize = 11.sp, color = colors.textSecondary)
                    }
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colors.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Recent Sessions
        item {
            Text(
                text = Fa.RECENT_SESSIONS,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
        }

        if (sessions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.bgCard)
                        .border(1.dp, colors.borderColor, RoundedCornerShape(14.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = Fa.NO_RECENT_SESSIONS, color = colors.textSecondary, fontSize = 12.sp)
                }
            }
        } else {
            items(sessions.take(3)) { session ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.bgCard)
                        .border(1.dp, colors.borderColor, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = session.scoreTitle, fontWeight = FontWeight.Bold, color = colors.textPrimary, fontSize = 13.sp)
                        Text(text = Formatters.formatToJalali(session.date), fontSize = 11.sp, color = colors.textSecondary)
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
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BentoPill(icon: String, label: String, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(colors.bgPrimary)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(text = icon, fontSize = 12.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
    }
}
