package com.violinassistant.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.violinassistant.app.data.PracticeSessionEntity
import com.violinassistant.app.data.ScoreEntity
import com.violinassistant.app.localization.Fa
import com.violinassistant.app.ui.components.*
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
    val featured = scores.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.bgPrimary),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ViolinEmblem(
                        strokeColor = colors.primary,
                        accentColor = colors.accent,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(text = Fa.APP_TITLE, style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
                }
                IconButton(onClick = onOpenThemeDialog, modifier = Modifier.size(48.dp)) {
                    Icon(imageVector = Icons.Rounded.Palette, contentDescription = "ظاهر برنامه", tint = colors.textSecondary)
                }
            }
        }

        item {
            Surface(color = colors.bgCard, shape = RoundedCornerShape(20.dp), tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    MusicalStaffWatermark(
                        lineColor = colors.borderColor.copy(alpha = 0.5f),
                        noteColor = colors.primary.copy(alpha = 0.16f),
                        modifier = Modifier.fillMaxWidth().height(92.dp).align(Alignment.TopCenter)
                    )
                    Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "آماده تمرین", style = MaterialTheme.typography.labelMedium, color = colors.primary)
                            Text(
                                text = featured?.title ?: Fa.LIBRARY_TITLE,
                                style = MaterialTheme.typography.titleLarge,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (featured != null) {
                                Text(
                                    text = featured.composer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Button(
                            onClick = onStartPractice,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = Fa.START_PRACTICE, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        item {
            Surface(color = colors.bgCard, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    StatColumn(value = Formatters.toPersianDigits(scores.size), label = "قطعه در دسترس", modifier = Modifier.weight(1f))
                    VerticalDivider(color = colors.borderColor, modifier = Modifier.height(32.dp))
                    StatColumn(value = Formatters.toPersianDigits(totalMins), label = "دقیقه تمرین", modifier = Modifier.weight(1f))
                    VerticalDivider(color = colors.borderColor, modifier = Modifier.height(32.dp))
                    StatColumn(
                        value = if (avgScore > 0) "${Formatters.toPersianDigits(avgScore)}٪" else "—",
                        label = "میانگین دقت",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (scores.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "قطعات برگزیده", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    Surface(color = colors.bgCard, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                        val previewScores = scores.take(3)
                        Column {
                            previewScores.forEachIndexed { idx, score ->
                                ScoreListRow(score = score, onClick = { onSelectScore(score) })
                                if (idx < previewScores.lastIndex) {
                                    HorizontalDivider(color = colors.borderColor.copy(alpha = 0.7f), modifier = Modifier.padding(horizontal = 16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        if (sessions.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = Fa.RECENT_SESSIONS, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    Surface(color = colors.bgCard, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                        val recent = sessions.take(3)
                        Column {
                            recent.forEachIndexed { idx, session ->
                                SessionListRow(session = session)
                                if (idx < recent.lastIndex) {
                                    HorizontalDivider(color = colors.borderColor.copy(alpha = 0.7f), modifier = Modifier.padding(horizontal = 16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatColumn(value: String, label: String, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
    }
}
