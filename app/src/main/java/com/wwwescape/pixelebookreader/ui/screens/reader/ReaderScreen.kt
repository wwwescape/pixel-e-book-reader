package com.wwwescape.pixelebookreader.ui.screens.reader

import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.BitmapFactory
import android.text.format.DateFormat
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wwwescape.pixelebookreader.R
import com.wwwescape.pixelebookreader.data.bookmark.Bookmark
import com.wwwescape.pixelebookreader.data.highlight.Highlight
import com.wwwescape.pixelebookreader.data.parser.ReaderText
import com.wwwescape.pixelebookreader.data.reader.OrientationLock
import com.wwwescape.pixelebookreader.data.reader.ProgressDisplayMode
import com.wwwescape.pixelebookreader.data.reader.ReaderSettings
import com.wwwescape.pixelebookreader.data.reader.SwipeGestureMode
import com.wwwescape.pixelebookreader.ui.components.EmptyStateNotice
import java.util.Date
import kotlin.math.abs
import kotlinx.coroutines.launch

/** Highlighter-marker swatches offered from [ReaderTopBar]'s Highlight action — soft/translucent
 * tones distinct from `ReaderSettingsPanel`'s reading background/font color presets. */
private val HIGHLIGHT_SWATCHES = listOf(
    0xFFFFF59DL, 0xFFA5D6A7L, 0xFF90CAF9L, 0xFFF48FB1L, 0xFFFFCC80L,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReaderScreen(
    bookId: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReaderViewModel = viewModel(),
) {
    val context = LocalContext.current
    @Suppress("DEPRECATION") // see ReaderTopBar's Highlight action doc for why the clipboard is
    // read this way rather than via the newer suspend-based LocalClipboard.
    val clipboard = LocalClipboardManager.current
    val highlightCopyFirstMessage = stringResource(R.string.highlight_copy_first_message)
    val highlightDeclinedMessage = stringResource(R.string.highlight_declined_message)
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

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

    val activePreset = uiState.activeColorPreset
    val backgroundColor = activePreset?.let { Color(it.backgroundColor) } ?: MaterialTheme.colorScheme.background
    val contentColor = activePreset?.let { Color(it.fontColor) } ?: MaterialTheme.colorScheme.onBackground

    val density = LocalDensity.current
    val indentSp = with(density) { settings.paragraphIndentationDp.dp.toSp() }
    val bodyStyle = remember(settings) { readerTextStyle(settings, forChapterTitle = false, indent = indentSp) }
    val chapterStyle = remember(settings) { readerTextStyle(settings, forChapterTitle = true) }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = book.scrollIndex.coerceIn(0, (uiState.content.size - 1).coerceAtLeast(0)),
        initialFirstVisibleItemScrollOffset = book.scrollOffset,
    )
    var showSettingsPanel by remember { mutableStateOf(false) }
    val barsVisible = !uiState.isImmersive
    val currentIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }

    // Saves progress whenever scrolling settles. Deliberately a *separate* snapshotFlow from the
    // one above: that one only tracks firstVisibleItemIndex/Offset, so it doesn't re-emit purely
    // because isScrollInProgress flips to false — a fling's last position-changing frame happens
    // *before* it settles, so gating the save on that flow (as this used to) meant saveProgress
    // was never called for the position a fling actually ends at. Tracking isScrollInProgress
    // itself here, and reading the position fresh at that moment, fixes it.
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { inProgress ->
            if (!inProgress) {
                viewModel.saveProgress(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
            }
        }
    }

    // Horizontal swipe gesture mode (Off/On/Inverse): drags translate the reader surface and
    // fade it (the "pull"/alpha-fade animations share one mechanism — both are just this same
    // gesture-tracked offset), and on release, a big-enough drag triggers a page-sized
    // programmatic scroll rather than moving the drag distance itself.
    val swipeOffset = remember { Animatable(0f) }
    val windowInfo = LocalWindowInfo.current
    val pageHeightPx = windowInfo.containerSize.height * settings.swipeScrollAmount.coerceIn(0.2f, 3f)
    val swipeThresholdPx = with(density) { 48.dp.toPx() } / settings.swipeSensitivity.coerceIn(0.2f, 5f)
    val swipeModifier = if (settings.swipeGestureMode != SwipeGestureMode.OFF) {
        Modifier.pointerInput(settings.swipeGestureMode, settings.swipeSensitivity) {
            detectHorizontalDragGestures(
                onDragEnd = {
                    val offset = swipeOffset.value
                    scope.launch {
                        if (abs(offset) > swipeThresholdPx) {
                            val movesForward = if (settings.swipeGestureMode == SwipeGestureMode.INVERSE) offset > 0 else offset < 0
                            listState.animateScrollBy(if (movesForward) pageHeightPx else -pageHeightPx)
                        }
                        swipeOffset.animateTo(0f)
                    }
                },
                onHorizontalDrag = { change, dragAmount ->
                    change.consume()
                    scope.launch { swipeOffset.snapTo(swipeOffset.value + dragAmount) }
                },
            )
        }
    } else {
        Modifier
    }
    val swipeAlpha = if (settings.swipeAlphaAnimation) 1f - (abs(swipeOffset.value) / (swipeThresholdPx * 3)).coerceIn(0f, 0.4f) else 1f
    val swipeTranslation = if (settings.swipePullAnimation) swipeOffset.value else 0f
    val disableNormalScroll = settings.swipeGestureMode != SwipeGestureMode.OFF && settings.swipeDisableNormalScroll

    val highlightsByIndex = remember(uiState.highlights) { uiState.highlights.groupBy { it.contentIndex } }
    val isCurrentBookmarked = uiState.bookmarks.any { it.contentIndex == currentIndex }
    fun jumpTo(contentIndex: Int) {
        viewModel.pushCheckpoint(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
        scope.launch {
            drawerState.close()
            listState.scrollToItem(contentIndex)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                ReaderDrawerContent(
                    chapters = uiState.chapters,
                    bookmarks = uiState.bookmarks,
                    highlights = uiState.highlights,
                    totalItems = uiState.content.size,
                    currentIndex = currentIndex,
                    onSelectChapter = ::jumpTo,
                    onJumpTo = ::jumpTo,
                    onDeleteBookmark = viewModel::deleteBookmark,
                    onDeleteHighlight = viewModel::deleteHighlight,
                )
            }
        },
    ) {
        Surface(color = backgroundColor, contentColor = contentColor, modifier = modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                SelectionContainer {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(swipeModifier)
                            .pointerInput(Unit) { detectTapGestures(onTap = { viewModel.toggleImmersive() }) }
                            .graphicsLayer { translationX = swipeTranslation; alpha = swipeAlpha },
                    ) {
                        LazyColumn(state = listState, userScrollEnabled = !disableNormalScroll, modifier = Modifier.fillMaxSize()) {
                            itemsIndexed(uiState.content, key = { index, _ -> index }) { index, item ->
                                ReaderTextItem(
                                    item = item,
                                    settings = settings,
                                    bodyStyle = bodyStyle,
                                    chapterStyle = chapterStyle,
                                    isCurrent = settings.highlightedReadingEnabled && index == currentIndex,
                                    highlights = highlightsByIndex[index] ?: emptyList(),
                                )
                            }
                            item { Spacer(modifier = Modifier.height(64.dp)) }
                        }
                        if (settings.limiterEnabled) HorizontalLimiterOverlay(settings)
                        if (settings.perceptionExpanderEnabled) PerceptionExpanderOverlay(settings)
                    }
                }

                AnimatedVisibility(visible = barsVisible, modifier = Modifier.align(Alignment.TopCenter)) {
                    ReaderTopBar(
                        title = book.title,
                        canUndoCheckpoint = uiState.canUndoCheckpoint,
                        isImmersive = uiState.isImmersive,
                        showPresetCycle = settings.fastColorPresetSwitch && uiState.colorPresets.isNotEmpty(),
                        isBookmarked = isCurrentBookmarked,
                        onBack = onBack,
                        onOpenChapters = { scope.launch { drawerState.open() } },
                        onUndoCheckpoint = {
                            viewModel.popCheckpoint()?.let { (index, offset) ->
                                scope.launch { listState.scrollToItem(index, offset) }
                            }
                        },
                        onToggleImmersive = viewModel::toggleImmersive,
                        onCyclePreset = viewModel::cycleColorPreset,
                        onOpenSettings = { showSettingsPanel = true },
                        onToggleBookmark = { viewModel.toggleBookmarkAtCurrentPosition(currentIndex) },
                        onHighlight = { color ->
                            val copiedText = clipboard.getText()?.text
                            if (copiedText.isNullOrBlank()) {
                                Toast.makeText(context, highlightCopyFirstMessage, Toast.LENGTH_SHORT).show()
                            } else if (!viewModel.addHighlight(currentIndex, copiedText, color)) {
                                Toast.makeText(context, highlightDeclinedMessage, Toast.LENGTH_SHORT).show()
                            }
                        },
                    )
                }

                if (settings.showProgressBar) {
                    AnimatedVisibility(visible = barsVisible, modifier = Modifier.align(Alignment.BottomCenter)) {
                        val progress = if (uiState.content.isEmpty()) 0f else currentIndex.toFloat() / uiState.content.size
                        ReaderProgressBar(progress = progress, currentIndex = currentIndex, totalItems = uiState.content.size, settings = settings)
                    }
                }
            }
        }
    }

    if (showSettingsPanel) {
        ReaderSettingsPanel(
            uiState = uiState,
            onDismiss = { showSettingsPanel = false },
            onUpdateSettings = viewModel::updateSettings,
            onAddColorPreset = viewModel::addColorPreset,
            onSelectColorPreset = viewModel::selectColorPreset,
            onDeleteColorPreset = viewModel::deleteColorPreset,
            onMoveColorPresetUp = viewModel::moveColorPresetUp,
            onMoveColorPresetDown = viewModel::moveColorPresetDown,
        )
    }
}

/** Pauses/resumes the reading-time clock ([ReaderViewModel.pauseSession]/[ReaderViewModel.resumeSession])
 * across app backgrounding — Compose's own composition lifecycle only covers "left the Reader
 * screen" (handled by `ReaderViewModel.onCleared`), not "app backgrounded while still on it",
 * which needs the Activity's `ON_PAUSE`/`ON_RESUME` instead. Not `private`: also reused as-is by
 * [PdfPageReaderScreen], which needs the exact same lifecycle-driven pause/resume behavior. */
@Composable
fun ReadingSessionEffect(onResume: () -> Unit, onPause: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> onResume()
                Lifecycle.Event.ON_PAUSE -> onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

/** Not `private`: also reused as-is by [PdfPageReaderScreen]. */
@Composable
fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        val activity = view.context as? Activity
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

/** Not `private`: also reused as-is by [PdfPageReaderScreen]. */
@Composable
fun OrientationLockEffect(lock: OrientationLock) {
    val view = LocalView.current
    DisposableEffect(lock) {
        val activity = view.context as? Activity
        val original = activity?.requestedOrientation
        activity?.requestedOrientation = when (lock) {
            OrientationLock.DEFAULT -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            OrientationLock.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            OrientationLock.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        onDispose {
            activity?.requestedOrientation = original ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
}

/** Not `private`: also reused as-is by [PdfPageReaderScreen]. */
@Composable
fun CustomBrightnessEffect(enabled: Boolean, brightness: Float) {
    val view = LocalView.current
    DisposableEffect(enabled, brightness) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            val params = window.attributes
            params.screenBrightness = if (enabled) brightness.coerceIn(0.01f, 1f) else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            window.attributes = params
        }
        onDispose {
            if (window != null) {
                val params = window.attributes
                params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = params
            }
        }
    }
}

@Composable
private fun HorizontalLimiterOverlay(settings: ReaderSettings) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val bandHeight = settings.limiterHeightDp.dp.coerceAtMost(maxHeight)
        val bandTop = ((maxHeight - bandHeight) / 2 + settings.limiterVerticalOffsetDp.dp).coerceIn(0.dp, maxHeight - bandHeight)
        val bandBottom = bandTop + bandHeight
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(bandTop)
                .align(Alignment.TopStart)
                .background(Color.Black.copy(alpha = settings.limiterDimming)),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(maxHeight - bandBottom)
                .align(Alignment.BottomStart)
                .background(Color.Black.copy(alpha = settings.limiterDimming)),
        )
        if (settings.limiterRulerEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(settings.limiterRulerThicknessDp.dp)
                    .offset(y = bandTop)
                    .background(MaterialTheme.colorScheme.primary),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(settings.limiterRulerThicknessDp.dp)
                    .offset(y = bandBottom - settings.limiterRulerThicknessDp.dp)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Composable
private fun PerceptionExpanderOverlay(settings: ReaderSettings) {
    Row(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.width(settings.perceptionExpanderPaddingDp.dp))
        Box(modifier = Modifier.width(settings.perceptionExpanderThicknessDp.dp).fillMaxHeight().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)))
        Spacer(modifier = Modifier.weight(1f))
        Box(modifier = Modifier.width(settings.perceptionExpanderThicknessDp.dp).fillMaxHeight().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)))
        Spacer(modifier = Modifier.width(settings.perceptionExpanderPaddingDp.dp))
    }
}

@Composable
private fun ReaderTopBar(
    title: String,
    canUndoCheckpoint: Boolean,
    isImmersive: Boolean,
    showPresetCycle: Boolean,
    isBookmarked: Boolean,
    onBack: () -> Unit,
    onOpenChapters: () -> Unit,
    onUndoCheckpoint: () -> Unit,
    onToggleImmersive: () -> Unit,
    onCyclePreset: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleBookmark: () -> Unit,
    onHighlight: (color: Long) -> Unit,
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
            if (canUndoCheckpoint) {
                IconButton(onClick = onUndoCheckpoint) {
                    Icon(imageVector = Icons.AutoMirrored.Rounded.Undo, contentDescription = stringResource(R.string.action_undo_checkpoint))
                }
            }
            if (showPresetCycle) {
                IconButton(onClick = onCyclePreset) {
                    Icon(imageVector = Icons.Rounded.Palette, contentDescription = stringResource(R.string.action_cycle_color_preset))
                }
            }
            IconButton(onClick = onToggleBookmark) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    contentDescription = stringResource(if (isBookmarked) R.string.action_remove_bookmark else R.string.action_add_bookmark),
                )
            }
            var showHighlightSwatches by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { showHighlightSwatches = true }) {
                    Icon(painter = painterResource(R.drawable.ic_ink_highlighter), contentDescription = stringResource(R.string.action_highlight))
                }
                DropdownMenu(expanded = showHighlightSwatches, onDismissRequest = { showHighlightSwatches = false }) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                        HIGHLIGHT_SWATCHES.forEach { swatch ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color(swatch), CircleShape)
                                    .clickable {
                                        showHighlightSwatches = false
                                        onHighlight(swatch)
                                    },
                            )
                        }
                    }
                }
            }
            IconButton(onClick = onOpenChapters) {
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
private fun ReaderProgressBar(progress: Float, currentIndex: Int, totalItems: Int, settings: ReaderSettings) {
    Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.systemBarsPadding().padding(horizontal = settings.progressBarPaddingDp.dp, vertical = 8.dp),
            horizontalAlignment = imageAlignmentToHorizontal(settings.progressBarAlignment),
        ) {
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            val label = when (settings.progressDisplayMode) {
                ProgressDisplayMode.PERCENTAGE -> stringResource(R.string.reader_progress_percent, (progress * 100).toInt())
                ProgressDisplayMode.COUNT -> stringResource(R.string.reader_progress_count, (currentIndex + 1).coerceAtMost(totalItems.coerceAtLeast(1)), totalItems)
            }
            Text(
                text = label,
                fontSize = settings.progressBarFontSizeSp.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

private enum class DrawerTab { CHAPTERS, BOOKMARKS, HIGHLIGHTS }

@Composable
private fun ReaderDrawerContent(
    chapters: List<ChapterEntry>,
    bookmarks: List<Bookmark>,
    highlights: List<Highlight>,
    totalItems: Int,
    currentIndex: Int,
    onSelectChapter: (Int) -> Unit,
    onJumpTo: (Int) -> Unit,
    onDeleteBookmark: (Bookmark) -> Unit,
    onDeleteHighlight: (Highlight) -> Unit,
) {
    var selectedTab by remember { mutableStateOf(DrawerTab.CHAPTERS) }
    Column(modifier = Modifier.padding(top = 12.dp)) {
        TabRow(selectedTabIndex = selectedTab.ordinal) {
            Tab(
                selected = selectedTab == DrawerTab.CHAPTERS,
                onClick = { selectedTab = DrawerTab.CHAPTERS },
                text = { Text(stringResource(R.string.action_chapters)) },
            )
            Tab(
                selected = selectedTab == DrawerTab.BOOKMARKS,
                onClick = { selectedTab = DrawerTab.BOOKMARKS },
                text = { Text(stringResource(R.string.tab_bookmarks)) },
            )
            Tab(
                selected = selectedTab == DrawerTab.HIGHLIGHTS,
                onClick = { selectedTab = DrawerTab.HIGHLIGHTS },
                text = { Text(stringResource(R.string.tab_highlights)) },
            )
        }
        when (selectedTab) {
            DrawerTab.CHAPTERS -> ChaptersTabContent(chapters, totalItems, currentIndex, onSelectChapter)
            DrawerTab.BOOKMARKS -> BookmarksTabContent(bookmarks, onJumpTo, onDeleteBookmark)
            DrawerTab.HIGHLIGHTS -> HighlightsTabContent(highlights, onJumpTo, onDeleteHighlight)
        }
    }
}

@Composable
private fun ChaptersTabContent(chapters: List<ChapterEntry>, totalItems: Int, currentIndex: Int, onSelectChapter: (Int) -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        if (chapters.isEmpty()) {
            Text(
                text = stringResource(R.string.reader_no_chapters),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
        chapters.forEachIndexed { entryIndex, entry ->
            val start = entry.contentIndex
            val end = chapters.getOrNull(entryIndex + 1)?.contentIndex ?: totalItems
            val chapterProgress = when {
                currentIndex < start -> 0f
                currentIndex >= end -> 1f
                else -> (currentIndex - start).toFloat() / (end - start).coerceAtLeast(1)
            }
            val isCurrent = currentIndex in start until end
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectChapter(start) }
                    .padding(start = if (entry.chapter.nested) 24.dp else 0.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = entry.chapter.title,
                    style = if (isCurrent) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.reader_progress_percent, (chapterProgress * 100).toInt()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BookmarksTabContent(bookmarks: List<Bookmark>, onJumpTo: (Int) -> Unit, onDelete: (Bookmark) -> Unit) {
    val context = LocalContext.current
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
                    .clickable { onJumpTo(bookmark.contentIndex) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = bookmark.snippet, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = DateFormat.getMediumDateFormat(context).format(Date(bookmark.createdAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { onDelete(bookmark) }) {
                    Icon(imageVector = Icons.Rounded.Delete, contentDescription = stringResource(R.string.action_delete))
                }
            }
        }
    }
}

@Composable
private fun HighlightsTabContent(highlights: List<Highlight>, onJumpTo: (Int) -> Unit, onDelete: (Highlight) -> Unit) {
    val context = LocalContext.current
    Column(modifier = Modifier.padding(16.dp)) {
        if (highlights.isEmpty()) {
            Text(
                text = stringResource(R.string.reader_no_highlights),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
        highlights.forEach { highlight ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onJumpTo(highlight.contentIndex) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.size(16.dp).background(Color(highlight.color), MaterialTheme.shapes.extraSmall))
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(text = highlight.text, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = DateFormat.getMediumDateFormat(context).format(Date(highlight.createdAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { onDelete(highlight) }) {
                    Icon(imageVector = Icons.Rounded.Delete, contentDescription = stringResource(R.string.action_delete))
                }
            }
        }
    }
}

@Composable
private fun ReaderTextItem(item: ReaderText, settings: ReaderSettings, bodyStyle: TextStyle, chapterStyle: TextStyle, isCurrent: Boolean, highlights: List<Highlight>) {
    when (item) {
        is ReaderText.Chapter -> Text(
            text = item.title,
            style = chapterStyle,
            modifier = Modifier.fillMaxWidth().padding(
                start = settings.sidePaddingDp.dp,
                end = settings.sidePaddingDp.dp,
                top = settings.paragraphHeightDp.dp * 2,
                bottom = settings.paragraphHeightDp.dp,
            ),
        )
        is ReaderText.Text -> ReaderParagraph(text = item.line, settings = settings, style = bodyStyle, isCurrent = isCurrent, highlights = highlights)
        ReaderText.Separator -> HorizontalDivider(modifier = Modifier.padding(horizontal = settings.sidePaddingDp.dp * 2, vertical = 16.dp))
        is ReaderText.Image -> ReaderImage(image = item, settings = settings)
    }
}

@Composable
private fun ReaderParagraph(text: String, settings: ReaderSettings, style: TextStyle, isCurrent: Boolean, highlights: List<Highlight>) {
    val context = LocalContext.current
    var layoutResult by remember(text) { mutableStateOf<TextLayoutResult?>(null) }
    val highlightModifier = if (isCurrent) {
        Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = (settings.highlightedReadingThicknessDp / 20f).coerceIn(0.05f, 0.3f)))
    } else {
        Modifier
    }
    val displayText = remember(text, highlights) {
        if (highlights.isEmpty()) {
            AnnotatedString(text)
        } else {
            buildAnnotatedString {
                append(text)
                highlights.forEach { highlight ->
                    val start = highlight.startOffset.coerceIn(0, text.length)
                    val end = highlight.endOffset.coerceIn(start, text.length)
                    addStyle(SpanStyle(background = Color(highlight.color)), start, end)
                }
            }
        }
    }
    Text(
        text = displayText,
        onTextLayout = { layoutResult = it },
        style = style,
        modifier = Modifier
            .fillMaxWidth()
            .then(highlightModifier)
            .padding(
                start = settings.sidePaddingDp.dp,
                end = settings.sidePaddingDp.dp,
                top = settings.paragraphHeightDp.dp,
                bottom = settings.verticalPaddingDp.dp,
            )
            .let { base ->
                if (!settings.doubleTapTranslateEnabled) return@let base
                base.pointerInput(text) {
                    detectTapGestures(onDoubleTap = { position ->
                        if (text.isEmpty()) return@detectTapGestures
                        val layout = layoutResult ?: return@detectTapGestures
                        val charIndex = layout.getOffsetForPosition(position).coerceIn(0, text.length - 1)
                        val wordRange = layout.getWordBoundary(charIndex)
                        val word = text.substring(wordRange.start, wordRange.end).trim()
                        if (word.isNotEmpty()) translateWord(context, word)
                    })
                }
            },
    )
}

@Composable
private fun ReaderImage(image: ReaderText.Image, settings: ReaderSettings) {
    if (!settings.showImages) return
    val bitmap = remember(image) { runCatching { BitmapFactory.decodeByteArray(image.data, 0, image.data.size) }.getOrNull() } ?: return
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalAlignment = imageAlignmentToHorizontal(settings.imageAlignment),
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = image.caption,
            contentScale = ContentScale.FillWidth,
            colorFilter = imageColorFilter(settings.imageColorEffect),
            modifier = Modifier
                .fillMaxWidth(settings.imageWidthPercent.coerceIn(0.1f, 1f))
                .clip(RoundedCornerShape(settings.imageCornerRoundnessDp.dp)),
        )
        if (settings.showImageCaptions && !image.caption.isNullOrBlank()) {
            Text(
                text = image.caption,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(settings.imageWidthPercent.coerceIn(0.1f, 1f)).padding(top = 4.dp),
            )
        }
    }
}
