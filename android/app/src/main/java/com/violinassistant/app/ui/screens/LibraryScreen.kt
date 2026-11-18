package com.violinassistant.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.violinassistant.app.data.ScoreEntity
import com.violinassistant.app.localization.Fa
import com.violinassistant.app.ui.theme.*
import java.io.InputStream

@Composable
fun LibraryScreen(
    scores: List<ScoreEntity>,
    syncMessage: String?,
    onSelectScore: (ScoreEntity) -> Unit,
    onImportFile: (InputStream, String) -> Unit,
    onSyncGitHub: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment ?: "score.musicxml"
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                onImportFile(inputStream, fileName)
            }
        }
    }

    val filtered = scores.filter {
        it.title.contains(searchQuery, ignoreCase = true) ||
        it.composer.contains(searchQuery, ignoreCase = true)
    }

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
                    text = Fa.LIBRARY_TITLE,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { filePicker.launch(arrayOf("*/*")) },
                        colors = ButtonDefaults.buttonColors(containerColor = BgCardHover),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, tint = TextPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(Fa.IMPORT_CUSTOM, fontSize = 12.sp, color = TextPrimary)
                    }

                    Button(
                        onClick = onSyncGitHub,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = TextPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("همگام‌سازی", fontSize = 12.sp, color = TextPrimary)
                    }
                }
            }
        }

        if (!syncMessage.isNullOrBlank()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(BgCard)
                        .padding(10.dp)
                ) {
                    Text(text = syncMessage, fontSize = 12.sp, color = TextSecondary)
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(Fa.SEARCH_HINT, fontSize = 13.sp, color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = BgCard,
                    unfocusedContainerColor = BgCard,
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        if (filtered.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(Fa.EMPTY_LIBRARY, color = TextSecondary, fontSize = 13.sp)
                }
            }
        } else {
            items(filtered) { score ->
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
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PrimaryBlue.copy(alpha = 0.15f)),
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

                    Button(
                        onClick = { onSelectScore(score) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("مشاهده نت", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
