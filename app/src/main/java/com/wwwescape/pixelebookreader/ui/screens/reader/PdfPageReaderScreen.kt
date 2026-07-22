package com.wwwescape.pixelebookreader.ui.screens.reader

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.NavigateBefore
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FindInPage
import androidx.compose.material.icons.automirrored.rounded.LastPage
import androidx.compose.material.icons.rounded.FirstPage
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wwwescape.pixelebookreader.R
import com.wwwescape.pixelebookreader.data.bookmark.Bookmark
import com.wwwescape.pixelebookreader.data.parser.pdf.PdfOutlineEntry
import com.wwwescape.pixelebookreader.ui.components.EmptyStateNotice
import kotlin.math.abs
import kotlinx.coroutines.launch

/** The page-mode reader for PDFs — a sibling to [ReaderScreen],
 * not a variant of it: pages are rendered as bitmaps and navigated via [HorizontalPager], with no
 * `List<ReaderText>`/continuous scroll involved at all. See [PdfPageReaderViewModel]'s doc for why
 * progress/bookmarks still reuse the same `Book`/`Bookmark` fields as the continuous reader. */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PdfPageReaderScreen(
    bookId: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PdfPageReaderViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    LaunchedEffect(bookId) { viewModel.open(bookId) }
    KeepScreenOn()

    if (uiState.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    if (uiState.isError || uiState.book == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyStateNotice(
                title = stringResource(R.string.reader_error_title),
                body = stringResource(R.string.reader_error_body),
                icon = Icons.AutoMirrored.Rounded.MenuBook,
                modifier = Modifier.padding(16.dp),
            )
        }
        return
    }

    val book = uiState.book!!
    val settings = uiState.settings
    OrientationLockEffect(settings.orientationLock)
    CustomBrightnessEffect(settings.customBrightnessEnabled, settings.customBrightness)
    ReadingSessionEffect(onResume = viewModel::resumeSession, onPause = viewModel::pauseSession)

    val pagerState = rememberPagerState(
        initialPage = book.scrollIndex.coerceIn(0, (uiState.pageCount - 1).coerceAtLeast(0)),
    ) { uiState.pageCount }

    var showSettingsSheet by remember { mutableStateOf(false) }
    var showJumpToPageDialog by remember { mutableStateOf(false) }
    var isCurrentPageZoomed by remember { mutableStateOf(false) }
    val barsVisible = !uiState.isImmersive
    val isCurrentBookmarked = uiState.bookmarks.any { it.contentIndex == pagerState.currentPage }
    val bookmarkPageSnippet = stringResource(R.string.bookmark_page_snippet, pagerState.currentPage + 1)

    // Saves progress off the *settled* page only — mirrors ReaderScreen's own scroll-save fix:
    // `currentPage` can tick forward mid-fling before the pager actually stops, so saving off
    // that would risk persisting a page the user only flew past.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page -> viewModel.saveProgress(page) }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                PdfDrawerContent(
                    outline = uiState.outline,
                    bookmarks = uiState.bookmarks,
                    currentPage = pagerState.currentPage,
                    totalPages = uiState.pageCount,
                    onJumpToPage = { page ->
                        scope.launch {
                            drawerState.close()
                            pagerState.animateScrollToPage(page)
                        }
                    },
                    onDeleteBookmark = viewModel::deleteBookmark,
                )
            }
        },
    ) {
        Surface(modifier = modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(state = pagerState, userScrollEnabled = !isCurrentPageZoomed, modifier = Modifier.fillMaxSize()) { page ->
                    PdfPageContent(
                        viewModel = viewModel,
                        page = page,
                        pageOffset = pagerState.getOffsetDistanceInPages(page),
                        pageTurnEffectEnabled = settings.pageTurnEffectEnabled,
                        onZoomChanged = { zoomed -> if (page == pagerState.currentPage) isCurrentPageZoomed = zoomed },
                    )
                }

                AnimatedVisibility(visible = barsVisible, modifier = Modifier.align(Alignment.TopCenter)) {
                    PdfReaderTopBar(
                        title = book.title,
                        isImmersive = uiState.isImmersive,
                        isBookmarked = isCurrentBookmarked,
                        onBack = onBack,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onToggleImmersive = viewModel::toggleImmersive,
                        onOpenSettings = { showSettingsSheet = true },
                        onToggleBookmark = { viewModel.toggleBookmarkAtCurrentPosition(pagerState.currentPage, bookmarkPageSnippet) },
                        onJumpToPage = { showJumpToPageDialog = true },
                    )
                }

                AnimatedVisibility(visible = barsVisible, modifier = Modifier.align(Alignment.BottomCenter)) {
                    PdfReaderNavigationBar(
                        currentPage = pagerState.currentPage,
                        totalPages = uiState.pageCount,
                        onFirst = { scope.launch { pagerState.animateScrollToPage(0) } },
                        onPrevious = { scope.launch { pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0)) } },
                        onNext = { scope.launch { pagerState.animateScrollToPage((pagerState.currentPage + 1).coerceAtMost((uiState.pageCount - 1).coerceAtLeast(0))) } },
                        onLast = { scope.launch { pagerState.animateScrollToPage((uiState.pageCount - 1).coerceAtLeast(0)) } },
                        onTapPageLabel = { showJumpToPageDialog = true },
                    )
                }
            }
        }
    }

    if (showJumpToPageDialog) {
        JumpToPageDialog(
            totalPages = uiState.pageCount,
            onDismiss = { showJumpToPageDialog = false },
            onJump = { page ->
                showJumpToPageDialog = false
                scope.launch { pagerState.animateScrollToPage(page) }
            },
        )
    }

    if (showSettingsSheet) {
        PdfReaderSettingsSheet(
            pageTurnEffectEnabled = settings.pageTurnEffectEnabled,
            onDismiss = { showSettingsSheet = false },
            onToggle = { enabled -> viewModel.updateSettings { it.copy(pageTurnEffectEnabled = enabled) } },
        )
    }
}

/** Renders one page bitmap, with pinch-to-zoom/pan (reset by double-tap or on page change) and,
 * when [pageTurnEffectEnabled], a stylized "page curl" approximation: a gentle rotation + shrink
 * + a directional shadow gradient near the edge that's lifting away. This is deliberately not a
 * physical paper-curl simulation (no bent mesh/visible page underside) — that fuller effect
 * was scoped out in favor of this lighter one. */
@Composable
private fun PdfPageContent(
    viewModel: PdfPageReaderViewModel,
    page: Int,
    pageOffset: Float,
    pageTurnEffectEnabled: Boolean,
    onZoomChanged: (Boolean) -> Unit,
) {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current.density
    val targetWidthPx = windowInfo.containerSize.width
    val bitmap by produceState<Bitmap?>(initialValue = null, page, targetWidthPx) {
        value = if (targetWidthPx > 0) viewModel.renderPage(page, targetWidthPx) else null
    }
    var scale by remember(page) { mutableStateOf(1f) }
    var panOffset by remember(page) { mutableStateOf(Offset.Zero) }
    LaunchedEffect(scale) { onZoomChanged(scale > 1.01f) }

    val turnFraction = pageOffset.coerceIn(-1f, 1f)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                if (pageTurnEffectEnabled && turnFraction != 0f) {
                    rotationY = turnFraction * 50f
                    cameraDistance = 16f * density
                    transformOrigin = TransformOrigin(if (turnFraction < 0f) 1f else 0f, 0.5f)
                    val shrink = 1f - abs(turnFraction) * 0.08f
                    scaleX = shrink
                    scaleY = shrink
                    alpha = 1f - abs(turnFraction) * 0.35f
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val currentBitmap = bitmap
        if (currentBitmap != null) {
            Image(
                bitmap = currentBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = panOffset.x
                        translationY = panOffset.y
                    }
                    // Only reacts to genuine multi-touch (2+ pointers) — unlike
                    // `detectTransformGestures`, which also claims single-finger drags as "pan"
                    // and would otherwise swallow the swipe gesture `HorizontalPager` needs to
                    // turn pages, since consumption happens whether or not the resulting scale
                    // actually changes.
                    .pointerInput(page) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            do {
                                val event = awaitPointerEvent()
                                if (event.changes.size > 1) {
                                    val newScale = (scale * event.calculateZoom()).coerceIn(1f, 5f)
                                    panOffset = if (newScale <= 1f) Offset.Zero else panOffset + event.calculatePan()
                                    scale = newScale
                                    event.changes.forEach { it.consume() }
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    }
                    .pointerInput(page) {
                        detectTapGestures(onDoubleTap = {
                            scale = 1f
                            panOffset = Offset.Zero
                        })
                    },
            )
            if (pageTurnEffectEnabled && turnFraction != 0f) {
                val shadowAlpha = abs(turnFraction) * 0.4f
                val shadowColors = if (turnFraction < 0f) {
                    listOf(Color.Black.copy(alpha = shadowAlpha), Color.Transparent)
                } else {
                    listOf(Color.Transparent, Color.Black.copy(alpha = shadowAlpha))
                }
                Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(shadowColors)))
            }
        } else {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun PdfReaderTopBar(
    title: String,
    isImmersive: Boolean,
    isBookmarked: Boolean,
    onBack: () -> Unit,
    onOpenDrawer: () -> Unit,
    onToggleImmersive: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleBookmark: () -> Unit,
    onJumpToPage: () -> Unit,
) {
    Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().systemBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
            Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            IconButton(onClick = onToggleBookmark) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    contentDescription = stringResource(if (isBookmarked) R.string.action_remove_bookmark else R.string.action_add_bookmark),
                )
            }
            IconButton(onClick = onJumpToPage) {
                Icon(imageVector = Icons.Rounded.FindInPage, contentDescription = stringResource(R.string.action_jump_to_page))
            }
            IconButton(onClick = onOpenDrawer) {
                Icon(imageVector = Icons.AutoMirrored.Rounded.MenuBook, contentDescription = stringResource(R.string.action_chapters))
            }
            IconButton(onClick = onOpenSettings) {
                Icon(imageVector = Icons.Rounded.Tune, contentDescription = stringResource(R.string.action_reader_settings))
            }
            IconButton(onClick = onToggleImmersive) {
                Icon(
                    imageVector = if (isImmersive) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                    contentDescription = stringResource(R.string.action_toggle_immersive),
                )
            }
        }
    }
}

@Composable
private fun PdfReaderNavigationBar(
    currentPage: Int,
    totalPages: Int,
    onFirst: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onLast: () -> Unit,
    onTapPageLabel: () -> Unit,
) {
    Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.systemBarsPadding().padding(horizontal = 8.dp, vertical = 4.dp)) {
            val progress = if (totalPages == 0) 0f else (currentPage + 1).toFloat() / totalPages
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onFirst, enabled = currentPage > 0) {
                    Icon(imageVector = Icons.Rounded.FirstPage, contentDescription = stringResource(R.string.action_first_page))
                }
                IconButton(onClick = onPrevious, enabled = currentPage > 0) {
                    Icon(imageVector = Icons.AutoMirrored.Rounded.NavigateBefore, contentDescription = stringResource(R.string.action_previous_page))
                }
                Text(
                    text = stringResource(R.string.reader_progress_count, (currentPage + 1).coerceAtMost(totalPages.coerceAtLeast(1)), totalPages),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable(onClick = onTapPageLabel).padding(horizontal = 8.dp, vertical = 8.dp),
                )
                IconButton(onClick = onNext, enabled = currentPage < totalPages - 1) {
                    Icon(imageVector = Icons.AutoMirrored.Rounded.NavigateNext, contentDescription = stringResource(R.string.action_next_page))
                }
                IconButton(onClick = onLast, enabled = currentPage < totalPages - 1) {
                    Icon(imageVector = Icons.AutoMirrored.Rounded.LastPage, contentDescription = stringResource(R.string.action_last_page))
                }
            }
        }
    }
}

@Composable
private fun JumpToPageDialog(totalPages: Int, onDismiss: () -> Unit, onJump: (Int) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_jump_to_page)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter(Char::isDigit) },
                label = { Text(stringResource(R.string.jump_to_page_hint, totalPages)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val page = text.toIntOrNull()
                if (page != null && page in 1..totalPages) onJump(page - 1) else onDismiss()
            }) { Text(stringResource(R.string.action_go)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PdfReaderSettingsSheet(pageTurnEffectEnabled: Boolean, onDismiss: () -> Unit, onToggle: (Boolean) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Text(stringResource(R.string.title_pdf_reader_settings), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            SwitchRow(stringResource(R.string.setting_page_turn_effect), pageTurnEffectEnabled, onToggle)
        }
    }
}

private enum class PdfDrawerTab { CHAPTERS, BOOKMARKS }

@Composable
private fun PdfDrawerContent(
    outline: List<PdfOutlineEntry>,
    bookmarks: List<Bookmark>,
    currentPage: Int,
    totalPages: Int,
    onJumpToPage: (Int) -> Unit,
    onDeleteBookmark: (Bookmark) -> Unit,
) {
    var selectedTab by remember { mutableStateOf(PdfDrawerTab.CHAPTERS) }
    Column(modifier = Modifier.padding(top = 12.dp)) {
        TabRow(selectedTabIndex = selectedTab.ordinal) {
            Tab(
                selected = selectedTab == PdfDrawerTab.CHAPTERS,
                onClick = { selectedTab = PdfDrawerTab.CHAPTERS },
                text = { Text(stringResource(R.string.action_chapters)) },
            )
            Tab(
                selected = selectedTab == PdfDrawerTab.BOOKMARKS,
                onClick = { selectedTab = PdfDrawerTab.BOOKMARKS },
                text = { Text(stringResource(R.string.tab_bookmarks)) },
            )
        }
        when (selectedTab) {
            PdfDrawerTab.CHAPTERS -> PdfOutlineTabContent(outline, currentPage, totalPages, onJumpToPage)
            PdfDrawerTab.BOOKMARKS -> PdfBookmarksTabContent(bookmarks, onJumpToPage, onDeleteBookmark)
        }
    }
}

@Composable
private fun PdfOutlineTabContent(outline: List<PdfOutlineEntry>, currentPage: Int, totalPages: Int, onJumpToPage: (Int) -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        if (outline.isEmpty()) {
            Text(
                text = stringResource(R.string.reader_no_chapters),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
        outline.forEachIndexed { entryIndex, entry ->
            val start = entry.pageIndex
            val end = outline.getOrNull(entryIndex + 1)?.pageIndex ?: totalPages
            val isCurrent = currentPage in start until end
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onJumpToPage(start) }
                    .padding(start = if (entry.depth > 0) 24.dp else 0.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = entry.title,
                    style = if (isCurrent) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.reader_progress_count, start + 1, totalPages),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PdfBookmarksTabContent(bookmarks: List<Bookmark>, onJumpToPage: (Int) -> Unit, onDelete: (Bookmark) -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        if (bookmarks.isEmpty()) {
            Text(
                text = stringResource(R.string.reader_no_bookmarks),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
        bookmarks.forEach { bookmark ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onJumpToPage(bookmark.contentIndex) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = bookmark.snippet, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = { onDelete(bookmark) }) {
                    Icon(imageVector = Icons.Rounded.Delete, contentDescription = stringResource(R.string.action_delete))
                }
            }
        }
    }
}
