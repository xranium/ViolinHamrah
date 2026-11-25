package com.violinassistant.app.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.violinassistant.app.data.ScoreEntity
import com.violinassistant.app.localization.Fa
import com.violinassistant.app.ui.components.ScoreListRow
import com.violinassistant.app.ui.theme.AppTheme
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
    val colors = AppTheme.colors
    var searchQuery by remember { mutableStateOf("") }
    var selectedDifficulty by remember { mutableStateOf<String?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = resolvePickedFileName(context, uri)
            context.contentResolver.openInputStream(uri)?.let { onImportFile(it, fileName) }
        }
    }

    val filtered = remember(scores, searchQuery, selectedDifficulty) {
        scores.filter { score ->
            val matchesSearch = score.title.contains(searchQuery, ignoreCase = true) ||
                score.composer.contains(searchQuery, ignoreCase = true)
            val matchesDiff = selectedDifficulty == null || score.difficulty.equals(selectedDifficulty, ignoreCase = true)
            matchesSearch && matchesDiff
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.bgPrimary),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = Fa.LIBRARY_TITLE, style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { filePicker.launch(arrayOf("*/*")) }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.FileUpload, contentDescription = Fa.IMPORT_CUSTOM, tint = colors.textSecondary)
                    }
                    IconButton(onClick = onSyncGitHub, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.CloudSync, contentDescription = Fa.SYNC_GITHUB, tint = colors.primary)
                    }
                }
            }
        }

        if (!syncMessage.isNullOrBlank()) {
            item {
                Surface(color = colors.bgCardHover, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = syncMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }

        item {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(Fa.SEARCH_HINT, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.textSecondary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Rounded.Close, contentDescription = "پاک کردن جستجو", tint = colors.textSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colors.bgCard,
                    unfocusedContainerColor = colors.bgCard,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            val filters = listOf(null to "همه", "BEGINNER" to Fa.DIFFICULTY_BEGINNER, "INTERMEDIATE" to Fa.DIFFICULTY_INTERMEDIATE, "ADVANCED" to Fa.DIFFICULTY_ADVANCED)
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                filters.forEach { (key, label) ->
                    val selected = selectedDifficulty == key
                    FilterChip(
                        selected = selected,
                        onClick = { selectedDifficulty = if (selected && key != null) null else key },
                        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.primary.copy(alpha = 0.16f),
                            selectedLabelColor = colors.primary,
                            containerColor = colors.bgCard,
                            labelColor = colors.textSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = colors.borderColor,
                            selectedBorderColor = colors.primary.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }
        if (filtered.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                    Text(Fa.EMPTY_LIBRARY, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                }
            }
        } else {
            itemsIndexed(filtered, key = { _, item -> item.id }) { idx, score ->
                Surface(
                    color = colors.bgCard,
                    shape = when {
                        filtered.size == 1 -> RoundedCornerShape(16.dp)
                        idx == 0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                        idx == filtered.lastIndex -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                        else -> RoundedCornerShape(0.dp)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        ScoreListRow(
                            score = score,
                            subtitle = "${score.composer} • ${difficultyLabel(score.difficulty)}",
                            onClick = { onSelectScore(score) }
                        )
                        if (idx < filtered.lastIndex) {
                            HorizontalDivider(color = colors.borderColor.copy(alpha = 0.65f), modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun difficultyLabel(diff: String): String = when (diff.uppercase()) {
    "INTERMEDIATE" -> Fa.DIFFICULTY_INTERMEDIATE
    "ADVANCED" -> Fa.DIFFICULTY_ADVANCED
    else -> Fa.DIFFICULTY_BEGINNER
}

private fun resolvePickedFileName(context: Context, uri: Uri): String {
    var name = uri.lastPathSegment ?: "score.musicxml"
    runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (c.moveToFirst() && idx >= 0) name = c.getString(idx)
        }
    }
    return name
}
