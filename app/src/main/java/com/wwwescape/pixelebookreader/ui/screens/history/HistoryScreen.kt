package com.wwwescape.pixelebookreader.ui.screens.history

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wwwescape.pixelebookreader.R
import com.wwwescape.pixelebookreader.data.history.HistoryWithBook
import com.wwwescape.pixelebookreader.ui.components.BookCoverImage
import com.wwwescape.pixelebookreader.ui.components.EmptyStateNotice
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onOpenBook: (Int) -> Unit,
    onOpenBookInfo: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = viewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val lastDeleted by viewModel.lastDeleted.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showClearAllConfirm by remember { mutableStateOf(false) }

    val deletedMessage = stringResource(R.string.history_entry_deleted)
    val undoLabel = stringResource(R.string.action_undo)
    LaunchedEffect(lastDeleted) {
        if (lastDeleted == null) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(message = deletedMessage, actionLabel = undoLabel, duration = SnackbarDuration.Short)
        if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete() else viewModel.consumeUndoState()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    placeholder = { Text(stringResource(R.string.action_search)) },
                    leadingIcon = { Icon(imageVector = Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { showClearAllConfirm = true }, enabled = !uiState.isEmpty) {
                    Icon(imageVector = Icons.Rounded.DeleteSweep, contentDescription = stringResource(R.string.action_clear_history))
                }
            }

            if (uiState.sections.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyStateNotice(
                        title = stringResource(R.string.history_empty_title),
                        body = stringResource(R.string.history_empty_body),
                        icon = Icons.Rounded.History,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    uiState.sections.forEach { section ->
                        item(key = "header_${section.date}") {
                            Text(
                                text = historyDateLabel(section.date),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
                            )
                        }
                        items(section.entries, key = { it.historyId }) { entry ->
                            HistoryRow(
                                entry = entry,
                                onClick = { onOpenBook(entry.bookId) },
                                onInfoClick = { onOpenBookInfo(entry.bookId) },
                                onDelete = { viewModel.deleteEntry(entry) },
                            )
                        }
                    }
                }
            }
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            title = { Text(stringResource(R.string.history_clear_title)) },
            text = { Text(stringResource(R.string.history_clear_body)) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteAll(); showClearAllConfirm = false }) {
                    Text(stringResource(R.string.action_clear_history))
                }
            },
            dismissButton = { TextButton(onClick = { showClearAllConfirm = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun HistoryRow(entry: HistoryWithBook, onClick: () -> Unit, onInfoClick: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BookCoverImage(coverPath = entry.coverImagePath, modifier = Modifier.size(48.dp, 64.dp).clip(RoundedCornerShape(6.dp)))
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(text = entry.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            entry.author?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                text = DateFormat.getTimeFormat(context).format(Date(entry.openedAt)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onInfoClick) {
            Icon(imageVector = Icons.Rounded.Info, contentDescription = stringResource(R.string.title_book_info))
        }
        IconButton(onClick = onDelete) {
            Icon(imageVector = Icons.Rounded.Delete, contentDescription = stringResource(R.string.action_delete))
        }
    }
}

@Composable
private fun historyDateLabel(date: LocalDate): String {
    val today = remember { LocalDate.now() }
    return when (date) {
        today -> stringResource(R.string.history_today)
        today.minusDays(1) -> stringResource(R.string.history_yesterday)
        else -> {
            val context = LocalContext.current
            remember(date) {
                DateFormat.getMediumDateFormat(context).format(Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()))
            }
        }
    }
}
