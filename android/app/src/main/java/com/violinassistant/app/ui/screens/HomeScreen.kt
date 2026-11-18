package com.violinassistant.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
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
import com.violinassistant.app.ui.theme.*
import com.violinassistant.app.util.Formatters

@Composable
fun HomeScreen(
    scores: List<ScoreEntity>,
    sessions: List<PracticeSessionEntity>,
    onStartPractice: () -> Unit,
    onSelectScore: (ScoreEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Welcome Banner Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🎻", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Fa.HOME_WELCOME,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = Fa.HOME_DESC,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onStartPractice,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = Fa.START_PRACTICE, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Featured Scores
        item {
            Text(
                text = "قطعات پیشنهادی",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        items(scores.take(2)) { score ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(BgCard)
                    .clickable { onSelectScore(score) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF3B82F6).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = PrimaryBlue)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = score.title, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(text = score.composer, fontSize = 12.sp, color = TextSecondary)
                    }
                }
                Text(text = "تمرین", color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        // Recent Sessions
        item {
            Text(
                text = Fa.RECENT_SESSIONS,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        if (sessions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(BgCard)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = Fa.NO_RECENT_SESSIONS, color = TextSecondary, fontSize = 13.sp)
                }
            }
        } else {
            items(sessions.take(3)) { session ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(BgCard)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = session.scoreTitle, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(
                            text = Formatters.formatToJalali(session.date),
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccentGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "امتیاز ${Formatters.toPersianDigits(session.score)}/۱۰۰",
                            color = AccentGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
