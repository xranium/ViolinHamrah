package com.violinassistant.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.violinassistant.app.data.PracticeSessionEntity
import com.violinassistant.app.localization.Fa
import com.violinassistant.app.ui.components.PracticeTrendSparkline
import com.violinassistant.app.ui.components.SessionListRow
import com.violinassistant.app.ui.components.ViolinEmblem
import com.violinassistant.app.ui.theme.AppTheme
import com.violinassistant.app.util.Formatters

@Composable
fun HistoryScreen(
    sessions: List<PracticeSessionEntity>,
    onBrowseLibrary: () -> Unit = {}
) {
    val colors = AppTheme.colors
    val totalMins = sessions.sumOf { it.durationSec } / 60
    val bestScore = sessions.maxOfOrNull { it.score } ?: 0

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.bgPrimary),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = Fa.HISTORY_TITLE, style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
                if (sessions.isNotEmpty()) {
                    Text(
                        text = "${Formatters.toPersianDigits(sessions.size)} جلسه",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary
                    )
                }
            }
        }

        if (sessions.isEmpty()) {
            item {
                Surface(color = colors.bgCard, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ViolinEmblem(
                            strokeColor = colors.textMuted,
                            accentColor = colors.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(text = Fa.NO_HISTORY, style = MaterialTheme.typography.bodyLarge, color = colors.textSecondary)
                        Button(
                            onClick = onBrowseLibrary,
                            modifier = Modifier.height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary)
                        ) {
                            Text(text = "مشاهده کتابخانه نتها", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        } else {
            item {
                Surface(color = colors.bgCard, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            Column {
                                Text(text = "زمان کل", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                Text(
                                    text = "${Formatters.toPersianDigits(totalMins)} دقیقه",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                            }
                            Column {
                                Text(text = "بهترین امتیاز", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                Text(
                                    text = "${Formatters.toPersianDigits(bestScore)}٪",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.accent
                                )
                            }
                        }

                        PracticeTrendSparkline(
                            recentScores = sessions.map { it.score },
                            barColor = colors.primary.copy(alpha = 0.35f),
                            highlightColor = colors.primary,
                            modifier = Modifier.width(88.dp).height(40.dp)
                        )
                    }
                }
            }

            itemsIndexed(sessions, key = { _, item -> item.id }) { idx, session ->
                Surface(
                    color = colors.bgCard,
                    shape = when {
                        sessions.size == 1 -> RoundedCornerShape(16.dp)
                        idx == 0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                        idx == sessions.lastIndex -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                        else -> RoundedCornerShape(0.dp)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        SessionListRow(session = session, showDuration = true)
                        if (idx < sessions.lastIndex) {
                            HorizontalDivider(color = colors.borderColor.copy(alpha = 0.65f), modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }
}
