package com.violinassistant.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
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
import com.violinassistant.app.ui.theme.AppTheme
import java.io.InputStream

@Composable
fun LibraryScreen(
    scores: List<ScoreEntity>,
    syncMessage: String?,
    onSelectScore: (ScoreEntity) -> Unit,
    onImportFile: (InputStream, String) -> Unit,
    onSyncGitHub: () -> Unit,
    onOpenThemeDialog: () -> Unit = {}
) {
    val context = LocalContext.current
    val colors = AppTheme.colors
    var searchQuery by remember { mutableStateOf("") }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            var fileName = uri.lastPathSegment ?: "score.musicxml"
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIdx >= 0) fileName = cursor.getString(nameIdx)
                }
            } catch (_: Exception) {}
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) onImportFile(inputStream, fileName)
        }
    }

    val filtered = scores.filter {
        it.title.contains(searchQuery, ignoreCase = true) || it.composer.contains(searchQuery, ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgPrimary)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header with Compact Action Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Fa.LIBRARY_TITLE,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { filePicker.launch(arrayOf("*/*")) },
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(colors.bgCard)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = "فایل", tint = colors.textPrimary, modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = onSyncGitHub,
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(colors.bgCard)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = "همگام‌سازی", tint = colors.primary, modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = onOpenThemeDialog,
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(colors.bgCard)
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = "پوسته", tint = colors.primary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        if (!syncMessage.isNullOrBlank()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.bgCard)
                        .border(1.dp, colors.borderColor, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Text(text = syncMessage, fontSize = 12.sp, color = colors.textSecondary)
                }
            }
        }

        // Modern Rounded Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(Fa.SEARCH_HINT, fontSize = 12.sp, color = colors.textSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(20.dp)) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.bgCard,
                    unfocusedContainerColor = colors.bgCard,
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.borderColor,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                ),
                shape = RoundedCornerShape(14.dp)
            )
        }

        if (filtered.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(Fa.EMPTY_LIBRARY, color = colors.textSecondary, fontSize = 13.sp)
                }
            }
        } else {
            items(filtered) { score ->
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
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
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
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(colors.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "مشاهده نت",
                            tint = colors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
