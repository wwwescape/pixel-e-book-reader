package com.wwwescape.pixelebookreader.ui.screens.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.LabelImportant
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wwwescape.pixelebookreader.R
import com.wwwescape.pixelebookreader.data.book.Book
import com.wwwescape.pixelebookreader.data.category.Category
import com.wwwescape.pixelebookreader.data.category.LibrarySortOrder
import com.wwwescape.pixelebookreader.data.library.LibraryLayout
import com.wwwescape.pixelebookreader.data.library.TitlePosition
import com.wwwescape.pixelebookreader.ui.components.BookCoverImage
import com.wwwescape.pixelebookreader.ui.components.EmptyStateNotice

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    onOpenBook: (Int) -> Unit,
    onOpenBookInfo: (Int) -> Unit,
    onManageCategories: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDisplayOptions by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        if (uiState.isSelectionMode) {
            SelectionBar(
                selectedCount = uiState.selectedBookIds.size,
                categories = uiState.categories,
                onAddToCategory = viewModel::addSelectedToCategory,
                onDelete = viewModel::deleteSelected,
                onClose = viewModel::clearSelection,
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    placeholder = { Text(stringResource(R.string.action_search)) },
                    leadingIcon = { Icon(imageVector = Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { showDisplayOptions = true }) {
                    Icon(imageVector = Icons.Rounded.Tune, contentDescription = stringResource(R.string.action_display_options))
                }
            }

            CategoryTabs(
                categories = uiState.categories,
                selectedCategoryId = uiState.selectedCategoryId,
                onSelectCategory = viewModel::selectCategory,
                onManageCategories = onManageCategories,
            )

            if (uiState.showBookCount) {
                Text(
                    text = pluralStringResource(R.plurals.library_book_count, uiState.books.size, uiState.books.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }

        if (uiState.books.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyStateNotice(
                    title = stringResource(R.string.library_empty_title),
                    body = stringResource(R.string.library_empty_body),
                    icon = Icons.AutoMirrored.Rounded.LibraryBooks,
                    modifier = Modifier.padding(16.dp),
                )
            }
        } else if (uiState.layout == LibraryLayout.GRID) {
            LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 120.dp), modifier = Modifier.padding(8.dp)) {
                items(uiState.books, key = { it.id }) { book ->
                    BookGridItem(
                        book = book,
                        titlePosition = uiState.titlePosition,
                        showProgress = uiState.showProgress,
                        isSelected = book.id in uiState.selectedBookIds,
                        isSelectionMode = uiState.isSelectionMode,
                        onClick = { if (uiState.isSelectionMode) viewModel.toggleBookSelection(book.id) else onOpenBook(book.id) },
                        onLongClick = { viewModel.toggleBookSelection(book.id) },
                        onInfoClick = { onOpenBookInfo(book.id) },
                    )
                }
            }
        } else {
            LazyColumn {
                items(uiState.books, key = { it.id }) { book ->
                    BookListItem(
                        book = book,
                        showProgress = uiState.showProgress,
                        isSelected = book.id in uiState.selectedBookIds,
                        isSelectionMode = uiState.isSelectionMode,
                        onClick = { if (uiState.isSelectionMode) viewModel.toggleBookSelection(book.id) else onOpenBook(book.id) },
                        onLongClick = { viewModel.toggleBookSelection(book.id) },
                        onInfoClick = { onOpenBookInfo(book.id) },
                    )
                }
            }
        }
    }

    if (showDisplayOptions) {
        DisplayOptionsSheet(
            uiState = uiState,
            onDismiss = { showDisplayOptions = false },
            onLayoutChange = viewModel::setLayout,
            onTitlePositionChange = viewModel::setTitlePosition,
            onShowProgressChange = viewModel::setShowProgress,
            onShowBookCountChange = viewModel::setShowBookCount,
            onSortOrderChange = viewModel::setSortOrder,
            onToggleSortDescending = viewModel::toggleSortDescending,
            onTagSelected = viewModel::setSelectedTag,
            onMinRatingSelected = viewModel::setMinRating,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryTabs(
    categories: List<Category>,
    selectedCategoryId: Int?,
    onSelectCategory: (Int?) -> Unit,
    onManageCategories: () -> Unit,
) {
    val selectedIndex = if (selectedCategoryId == null) 0 else categories.indexOfFirst { it.id == selectedCategoryId } + 1
    PrimaryScrollableTabRow(selectedTabIndex = selectedIndex.coerceAtLeast(0)) {
        Tab(selected = selectedCategoryId == null, onClick = { onSelectCategory(null) }, text = { Text(stringResource(R.string.category_all)) })
        categories.forEach { category ->
            Tab(selected = selectedCategoryId == category.id, onClick = { onSelectCategory(category.id) }, text = { Text(category.title) })
        }
        Tab(selected = false, onClick = onManageCategories, icon = {
            Icon(imageVector = Icons.Rounded.CreateNewFolder, contentDescription = stringResource(R.string.title_manage_categories))
        })
    }
}

@Composable
private fun SelectionBar(
    selectedCount: Int,
    categories: List<Category>,
    onAddToCategory: (Int) -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
) {
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(imageVector = Icons.Rounded.Close, contentDescription = stringResource(R.string.action_cancel))
        }
        Text(
            text = pluralStringResource(R.plurals.selection_count, selectedCount, selectedCount),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        Box {
            IconButton(onClick = { categoryMenuExpanded = true }, enabled = categories.isNotEmpty()) {
                Icon(imageVector = Icons.AutoMirrored.Rounded.LabelImportant, contentDescription = stringResource(R.string.action_add_to_category))
            }
            DropdownMenu(expanded = categoryMenuExpanded, onDismissRequest = { categoryMenuExpanded = false }) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.title) },
                        onClick = { onAddToCategory(category.id); categoryMenuExpanded = false },
                    )
                }
            }
        }
        IconButton(onClick = onDelete) {
            Icon(imageVector = Icons.Rounded.Delete, contentDescription = stringResource(R.string.action_delete))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookGridItem(
    book: Book,
    titlePosition: TitlePosition,
    showProgress: Boolean,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onInfoClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .padding(6.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column {
            Box {
                BookCoverImage(coverPath = book.coverImagePath, modifier = Modifier.fillMaxWidth().aspectRatio(0.7f))
                if (isSelectionMode) {
                    SelectionMark(isSelected, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp))
                } else {
                    IconButton(
                        onClick = onInfoClick,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = stringResource(R.string.title_book_info),
                            tint = Color.White,
                        )
                    }
                }
                if (titlePosition == TitlePosition.OVERLAY) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomStart)
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
                            .padding(8.dp),
                    ) {
                        Text(text = book.title, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            if (titlePosition == TitlePosition.BELOW) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.labelMedium,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            if (showProgress) {
                // Always reserved (even at 0%) rather than only when a book has progress, so
                // every tile in the grid is the same height regardless of per-book progress —
                // a book with no progress yet just shows an empty bar instead of leaving a gap.
                LinearProgressIndicator(
                    progress = { book.progress },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookListItem(
    book: Book,
    showProgress: Boolean,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onInfoClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            BookCoverImage(coverPath = book.coverImagePath, modifier = Modifier.size(48.dp, 64.dp).clip(RoundedCornerShape(6.dp)))
            if (isSelectionMode) {
                SelectionMark(isSelected, modifier = Modifier.align(Alignment.TopEnd))
            }
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(text = book.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            book.author?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (showProgress && book.progress > 0f) {
                LinearProgressIndicator(progress = { book.progress }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
            }
        }
        if (!isSelectionMode) {
            IconButton(onClick = onInfoClick) {
                Icon(imageVector = Icons.Rounded.Info, contentDescription = stringResource(R.string.title_book_info))
            }
        }
    }
}

@Composable
private fun SelectionMark(isSelected: Boolean, modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Rounded.CheckCircle,
        contentDescription = null,
        tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DisplayOptionsSheet(
    uiState: LibraryUiState,
    onDismiss: () -> Unit,
    onLayoutChange: (LibraryLayout) -> Unit,
    onTitlePositionChange: (TitlePosition) -> Unit,
    onShowProgressChange: (Boolean) -> Unit,
    onShowBookCountChange: (Boolean) -> Unit,
    onSortOrderChange: (LibrarySortOrder) -> Unit,
    onToggleSortDescending: () -> Unit,
    onTagSelected: (String) -> Unit,
    onMinRatingSelected: (Float) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(stringResource(R.string.action_display_options), style = MaterialTheme.typography.titleMedium)

            OptionRow(stringResource(R.string.option_layout)) {
                IconButton(onClick = { onLayoutChange(LibraryLayout.GRID) }) {
                    Icon(
                        imageVector = Icons.Rounded.GridView,
                        contentDescription = stringResource(R.string.layout_grid),
                        tint = if (uiState.layout == LibraryLayout.GRID) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { onLayoutChange(LibraryLayout.LIST) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ViewList,
                        contentDescription = stringResource(R.string.layout_list),
                        tint = if (uiState.layout == LibraryLayout.LIST) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            OptionRow(stringResource(R.string.option_title_position)) {
                TextRadio(stringResource(R.string.option_title_below), uiState.titlePosition == TitlePosition.BELOW) { onTitlePositionChange(TitlePosition.BELOW) }
                TextRadio(stringResource(R.string.option_title_overlay), uiState.titlePosition == TitlePosition.OVERLAY) { onTitlePositionChange(TitlePosition.OVERLAY) }
            }

            OptionRow(stringResource(R.string.option_show_progress)) {
                Switch(checked = uiState.showProgress, onCheckedChange = onShowProgressChange)
            }
            OptionRow(stringResource(R.string.option_show_book_count)) {
                Switch(checked = uiState.showBookCount, onCheckedChange = onShowBookCountChange)
            }

            Text(stringResource(R.string.option_sort_by), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
            LibrarySortOrder.entries.forEach { order ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = uiState.sortOrder == order,
                        onClick = { if (uiState.sortOrder == order) onToggleSortDescending() else onSortOrderChange(order) },
                    )
                    Text(librarySortOrderLabel(order))
                    if (uiState.sortOrder == order) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Sort,
                            contentDescription = if (uiState.sortDescending) stringResource(R.string.sort_descending) else stringResource(R.string.sort_ascending),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
            if (uiState.allTags.isNotEmpty()) {
                Text(stringResource(R.string.option_filter_by_tag), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    uiState.allTags.forEach { tag ->
                        FilterChip(selected = uiState.selectedTag == tag, onClick = { onTagSelected(tag) }, label = { Text(tag) })
                    }
                }
            }

            Text(stringResource(R.string.option_filter_by_rating), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            Row {
                for (star in 1..5) {
                    IconButton(onClick = { onMinRatingSelected(star.toFloat()) }) {
                        Icon(
                            imageVector = if ((uiState.minRating ?: 0f) >= star) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                            contentDescription = pluralStringResource(R.plurals.min_rating_star_content_description, star, star),
                            tint = if ((uiState.minRating ?: 0f) >= star) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Box(modifier = Modifier.padding(bottom = 24.dp))
        }
    }
}

@Composable
private fun OptionRow(label: String, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Row { content() }
    }
}

@Composable
private fun TextRadio(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(modifier = Modifier.padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label)
    }
}

@Composable
private fun librarySortOrderLabel(order: LibrarySortOrder): String = when (order) {
    LibrarySortOrder.TITLE -> stringResource(R.string.sort_title)
    LibrarySortOrder.AUTHOR -> stringResource(R.string.sort_author)
    LibrarySortOrder.LAST_READ -> stringResource(R.string.sort_last_read)
    LibrarySortOrder.DATE_ADDED -> stringResource(R.string.sort_date_added)
    LibrarySortOrder.SERIES -> stringResource(R.string.sort_series)
    LibrarySortOrder.RATING -> stringResource(R.string.sort_rating)
}
