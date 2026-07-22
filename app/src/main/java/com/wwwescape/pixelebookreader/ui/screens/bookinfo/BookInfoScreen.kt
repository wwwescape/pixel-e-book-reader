package com.wwwescape.pixelebookreader.ui.screens.bookinfo

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wwwescape.pixelebookreader.R
import com.wwwescape.pixelebookreader.data.filesystem.FileSystemRepository
import com.wwwescape.pixelebookreader.ui.components.BookCoverImage

@Composable
fun BookInfoScreen(
    bookId: Int,
    onDeleted: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookInfoViewModel = viewModel(),
) {
    val context = LocalContext.current
    val book by viewModel.book.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val deleted by viewModel.deleted.collectAsState()

    LaunchedEffect(bookId) { viewModel.setBookId(bookId) }
    LaunchedEffect(deleted) { if (deleted) onDeleted() }

    val currentBook = book ?: return

    var title by remember(currentBook.id) { mutableStateOf(currentBook.title) }
    var author by remember(currentBook.id) { mutableStateOf(currentBook.author.orEmpty()) }
    var description by remember(currentBook.id) { mutableStateOf(currentBook.description.orEmpty()) }
    var selectedCategoryIds by remember(currentBook.id) { mutableStateOf(currentBook.categories.toSet()) }
    var series by remember(currentBook.id) { mutableStateOf(currentBook.series.orEmpty()) }
    var seriesNumber by remember(currentBook.id) { mutableStateOf(currentBook.seriesNumber?.let { formatSeriesNumber(it) }.orEmpty()) }
    var tagsText by remember(currentBook.id) { mutableStateOf(currentBook.tags.joinToString(", ")) }
    var rating by remember(currentBook.id) { mutableStateOf(currentBook.rating) }
    var isbn by remember(currentBook.id) { mutableStateOf(currentBook.isbn.orEmpty()) }
    var publishDate by remember(currentBook.id) { mutableStateOf(currentBook.publishDate.orEmpty()) }
    var language by remember(currentBook.id) { mutableStateOf(currentBook.language.orEmpty()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val pickCover = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { picked ->
            FileSystemRepository.readBytes(context, picked)?.let { viewModel.replaceCover(it) }
        }
    }
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val name = FileSystemRepository.documentFor(context, it)?.name ?: (it.lastPathSegment ?: currentBook.fileName)
            viewModel.relink(it, name)
        }
    }

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BookCoverImage(
                coverPath = currentBook.coverImagePath,
                modifier = Modifier.width(120.dp).aspectRatio(0.7f).clip(RoundedCornerShape(12.dp)),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { pickCover.launch("image/*") }) { Text(stringResource(R.string.action_replace_cover)) }
                TextButton(onClick = { viewModel.resetCover() }) { Text(stringResource(R.string.action_reset_cover)) }
                TextButton(onClick = { viewModel.deleteCover() }) { Text(stringResource(R.string.action_delete_cover)) }
            }
        }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(stringResource(R.string.field_title)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )
        OutlinedTextField(
            value = author,
            onValueChange = { author = it },
            label = { Text(stringResource(R.string.field_author)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text(stringResource(R.string.field_description)) },
            minLines = 3,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            OutlinedTextField(
                value = series,
                onValueChange = { series = it },
                label = { Text(stringResource(R.string.field_series)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = seriesNumber,
                onValueChange = { seriesNumber = it },
                label = { Text(stringResource(R.string.field_series_number)) },
                singleLine = true,
                modifier = Modifier.width(110.dp),
            )
        }

        Column(modifier = Modifier.padding(top = 12.dp)) {
            Text(stringResource(R.string.field_rating), style = MaterialTheme.typography.labelMedium)
            RatingStars(rating = rating, onRatingChange = { rating = if (rating == it) null else it })
        }

        OutlinedTextField(
            value = tagsText,
            onValueChange = { tagsText = it },
            label = { Text(stringResource(R.string.field_tags)) },
            placeholder = { Text(stringResource(R.string.field_tags_placeholder)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            OutlinedTextField(
                value = isbn,
                onValueChange = { isbn = it },
                label = { Text(stringResource(R.string.field_isbn)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = publishDate,
                onValueChange = { publishDate = it },
                label = { Text(stringResource(R.string.field_publish_date)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        OutlinedTextField(
            value = language,
            onValueChange = { language = it },
            label = { Text(stringResource(R.string.field_language)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.field_linked_file), style = MaterialTheme.typography.labelMedium)
                Text(currentBook.fileName, style = MaterialTheme.typography.bodyMedium)
            }
            OutlinedButton(onClick = { pickFile.launch(arrayOf("*/*")) }) { Text(stringResource(R.string.action_relink)) }
        }

        if (categories.isNotEmpty()) {
            Text(
                text = stringResource(R.string.field_categories),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 16.dp),
            )
            categories.forEach { category ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(
                        checked = category.id in selectedCategoryIds,
                        onCheckedChange = { checked ->
                            selectedCategoryIds = if (checked) selectedCategoryIds + category.id else selectedCategoryIds - category.id
                        },
                    )
                    Text(category.title)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(imageVector = Icons.Rounded.Delete, contentDescription = stringResource(R.string.action_delete))
            }
            Button(
                onClick = {
                    val tags = tagsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    viewModel.save(title, author, description, selectedCategoryIds, series, seriesNumber, tags, rating, isbn, publishDate, language)
                    onSaved()
                },
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.book_delete_title)) },
            text = { Text(stringResource(R.string.book_delete_body, currentBook.title)) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteBook(); showDeleteConfirm = false }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

/** Whole-star rating only (1..5, tapping the current star clears it) — a deliberate scope cut,
 * same reasoning as the fixed color-preset swatch palette: half-star granularity
 * would need a custom drag-sensitive widget Compose has no built-in equivalent for. */
@Composable
private fun RatingStars(rating: Float?, onRatingChange: (Float) -> Unit) {
    Row {
        for (star in 1..5) {
            IconButton(onClick = { onRatingChange(star.toFloat()) }, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if ((rating ?: 0f) >= star) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription = pluralStringResource(R.plurals.rating_star_content_description, star, star),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private fun formatSeriesNumber(value: Float): String =
    if (value == value.toInt().toFloat()) value.toInt().toString() else value.toString()
