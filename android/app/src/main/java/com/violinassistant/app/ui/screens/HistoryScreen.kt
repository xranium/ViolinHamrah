package com.violinassistant.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
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
import com.violinassistant.app.ui.theme.*
import com.violinassistant.app.util.Formatters

@Composable
fun HistoryScreen(sessions: List<PracticeSessionEntity>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Fa.HISTORY_TITLE,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "${Fa.TOTAL_SESSIONS}: ${Formatters.toPersianDigits(sessions.size)}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        if (sessions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(BgCard)
                        .padding(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = Fa.NO_HISTORY, color = TextSecondary, fontSize = 13.sp)
                }
            }
        } else {
            items(sessions) { session ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(BgCard)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PrimaryBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = PrimaryBlue)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = session.scoreTitle, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(
                                text = "${Formatters.formatToJalali(session.date)} • ${Formatters.formatDurationHuman(session.durationSec)}",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccentGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${Formatters.toPersianDigits(session.score)}/۱۰۰",
                            fontWeight = FontWeight.Bold,
                            color = AccentGreen,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
