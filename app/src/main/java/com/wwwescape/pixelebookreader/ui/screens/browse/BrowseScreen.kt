package com.wwwescape.pixelebookreader.ui.screens.browse

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wwwescape.pixelebookreader.R
import com.wwwescape.pixelebookreader.data.browse.BrowseSource
import com.wwwescape.pixelebookreader.data.filesystem.BrowsableFile
import com.wwwescape.pixelebookreader.data.filesystem.FileSystemRepository
import com.wwwescape.pixelebookreader.ui.components.EmptyStateNotice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(onOpenBook: (Int) -> Unit, modifier: Modifier = Modifier, viewModel: BrowseViewModel = viewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val importResult by viewModel.importResult.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(enabled = !uiState.atRoot) { viewModel.navigateUp() }

    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { viewModel.addSource(it) }
    }
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val name = FileSystemRepository.documentFor(context, uri)?.name ?: (uri.lastPathSegment ?: "book")
            viewModel.importLooseFile(uri, name)
        }
    }

    val importSuccessTemplate = stringResource(R.string.browse_import_success)
    val importFailureTemplate = stringResource(R.string.browse_import_failure)
    val openActionLabel = stringResource(R.string.action_open)
    LaunchedEffect(importResult) {
        val result = importResult ?: return@LaunchedEffect
        when (result) {
            is ImportOutcome.Success -> {
                val message = String.format(importSuccessTemplate, result.title)
                val snackbarResult = snackbarHostState.showSnackbar(message, actionLabel = openActionLabel)
                if (snackbarResult == SnackbarResult.ActionPerformed) onOpenBook(result.bookId)
            }
            is ImportOutcome.Failure -> {
                snackbarHostState.showSnackbar(String.format(importFailureTemplate, result.fileName))
            }
        }
        viewModel.consumeImportResult()
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (uiState.atRoot) {
            SourcesList(
                sources = uiState.sources,
                onOpenSource = viewModel::openSource,
                onRemoveSource = viewModel::removeSource,
                onAddFolder = { pickFolder.launch(null) },
                onAddFile = { pickFile.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            FolderBrowser(
                uiState = uiState,
                onUp = { viewModel.navigateUp() },
                onOpenFolder = viewModel::openFolder,
                onOpenFile = viewModel::importFile,
                onQueryChange = viewModel::setQuery,
                onSortOrderChange = viewModel::setSortOrder,
                onToggleSortDescending = viewModel::toggleSortDescending,
                onToggleLayout = {
                    viewModel.setLayout(if (uiState.layout == BrowseLayout.LIST) BrowseLayout.GRID else BrowseLayout.LIST)
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (uiState.importingFileName != null) {
            Snackbar(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
                Text(stringResource(R.string.browse_importing, uiState.importingFileName ?: ""))
            }
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

/**
 * Folders are the primary content here — a persistent place you can revisit, whose contents
 * (including new books dropped in later) Browse can list indefinitely. Importing a single file
 * is a one-shot action with no ongoing place to revisit — the book just lands in the Library —
 * so it's deliberately a lighter-weight, secondary action pinned below the folder content rather
 * than a peer of "Add Folder" (this replaced an earlier layout with two equal-weight "Add
 * Folder"/"Add Book File" cards, which implied a symmetry between the two that doesn't
 * actually exist).
 */
@Composable
private fun SourcesList(
    sources: List<BrowseSource>,
    onOpenSource: (BrowseSource) -> Unit,
    onRemoveSource: (BrowseSource) -> Unit,
    onAddFolder: () -> Unit,
    onAddFile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.browse_section_folders), style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = onAddFolder) {
                Icon(imageVector = Icons.Rounded.CreateNewFolder, contentDescription = stringResource(R.string.action_add_folder))
            }
        }

        if (sources.isEmpty()) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                EmptyStateNotice(
                    title = stringResource(R.string.browse_empty_title),
                    body = stringResource(R.string.browse_empty_body),
                    icon = Icons.Rounded.FolderOpen,
                )
                Button(onClick = onAddFolder, modifier = Modifier.padding(top = 16.dp)) {
                    Icon(imageVector = Icons.Rounded.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.action_add_folder), modifier = Modifier.padding(start = 8.dp))
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
                items(sources, key = { it.uri.toString() }) { source ->
                    ListItem(
                        headlineContent = { Text(source.displayName) },
                        leadingContent = { Icon(imageVector = Icons.Rounded.Folder, contentDescription = null) },
                        trailingContent = {
                            IconButton(onClick = { onRemoveSource(source) }) {
                                Icon(imageVector = Icons.Rounded.Close, contentDescription = stringResource(R.string.action_remove))
                            }
                        },
                        modifier = Modifier.clickable { onOpenSource(source) },
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        TextButton(onClick = onAddFile, modifier = Modifier.fillMaxWidth()) {
            Icon(imageVector = Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.action_add_file), modifier = Modifier.padding(start = 8.dp))
        }
        Text(
            text = stringResource(R.string.browse_single_file_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
        )
    }
}

@Composable
private fun FolderBrowser(
    uiState: BrowseUiState,
    onUp: () -> Unit,
    onOpenFolder: (BrowsableFile) -> Unit,
    onOpenFile: (BrowsableFile) -> Unit,
    onQueryChange: (String) -> Unit,
    onSortOrderChange: (BrowseSortOrder) -> Unit,
    onToggleSortDescending: () -> Unit,
    onToggleLayout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onUp) {
                Icon(imageVector = Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
            Text(
                text = uiState.breadcrumbs.lastOrNull().orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onToggleLayout) {
                Icon(
                    imageVector = if (uiState.layout == BrowseLayout.LIST) Icons.Rounded.GridView else Icons.AutoMirrored.Rounded.ViewList,
                    contentDescription = stringResource(R.string.action_toggle_layout),
                )
            }
            Box {
                IconButton(onClick = { sortMenuExpanded = true }) {
                    Icon(imageVector = Icons.AutoMirrored.Rounded.Sort, contentDescription = stringResource(R.string.action_sort))
                }
                DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                    BrowseSortOrder.entries.forEach { order ->
                        DropdownMenuItem(
                            text = { Text(browseSortOrderLabel(order)) },
                            onClick = {
                                if (uiState.sortOrder == order) onToggleSortDescending() else onSortOrderChange(order)
                                sortMenuExpanded = false
                            },
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = uiState.query,
            onValueChange = onQueryChange,
            placeholder = { Text(stringResource(R.string.action_search)) },
            leadingIcon = { Icon(imageVector = Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        )

        if (uiState.entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.browse_folder_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else if (uiState.layout == BrowseLayout.LIST) {
            LazyColumn {
                items(uiState.entries, key = { it.uri.toString() }) { entry ->
                    BrowseEntryRow(entry = entry, onClick = { if (entry.isDirectory) onOpenFolder(entry) else onOpenFile(entry) })
                }
            }
        } else {
            LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 96.dp), modifier = Modifier.padding(8.dp)) {
                items(uiState.entries, key = { it.uri.toString() }) { entry ->
                    BrowseEntryTile(entry = entry, onClick = { if (entry.isDirectory) onOpenFolder(entry) else onOpenFile(entry) })
                }
            }
        }
    }
}

@Composable
private fun BrowseEntryRow(entry: BrowsableFile, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(entry.name) },
        leadingContent = {
            Icon(
                imageVector = if (entry.isDirectory) Icons.Rounded.Folder else Icons.AutoMirrored.Rounded.InsertDriveFile,
                contentDescription = null,
            )
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun BrowseEntryTile(entry: BrowsableFile, onClick: () -> Unit) {
    Card(
        modifier = Modifier.padding(4.dp).aspectRatio(0.85f).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = if (entry.isDirectory) Icons.Rounded.Folder else Icons.AutoMirrored.Rounded.InsertDriveFile,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
            )
            Text(
                text = entry.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun browseSortOrderLabel(order: BrowseSortOrder): String = when (order) {
    BrowseSortOrder.NAME -> stringResource(R.string.sort_name)
    BrowseSortOrder.DATE -> stringResource(R.string.sort_date)
    BrowseSortOrder.SIZE -> stringResource(R.string.sort_size)
}
