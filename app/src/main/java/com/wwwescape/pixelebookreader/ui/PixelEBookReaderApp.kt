package com.wwwescape.pixelebookreader.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wwwescape.pixelebookreader.R
import com.wwwescape.pixelebookreader.data.book.BookRepository
import com.wwwescape.pixelebookreader.data.parser.BookParsers
import com.wwwescape.pixelebookreader.ui.components.CenteredCollapsingTopBar
import com.wwwescape.pixelebookreader.ui.navigation.Destination
import com.wwwescape.pixelebookreader.ui.screens.bookinfo.BookInfoScreen
import com.wwwescape.pixelebookreader.ui.screens.browse.BrowseScreen
import com.wwwescape.pixelebookreader.ui.screens.categories.CategoryManagementScreen
import com.wwwescape.pixelebookreader.ui.screens.history.HistoryScreen
import com.wwwescape.pixelebookreader.ui.screens.library.LibraryScreen
import com.wwwescape.pixelebookreader.ui.screens.reader.PdfPageReaderScreen
import com.wwwescape.pixelebookreader.ui.screens.reader.ReaderScreen
import com.wwwescape.pixelebookreader.ui.screens.settings.LicensesScreen
import com.wwwescape.pixelebookreader.ui.screens.settings.SettingsScreen
import com.wwwescape.pixelebookreader.ui.screens.stats.StatisticsScreen
import kotlinx.coroutines.flow.first

private const val MANAGE_CATEGORIES_ROUTE = "manage_categories"
private const val LICENSES_ROUTE = "licenses"
private const val STATISTICS_ROUTE = "statistics"
private const val BOOK_INFO_ARG = "bookId"
private const val BOOK_INFO_ROUTE = "book_info/{$BOOK_INFO_ARG}"
private fun bookInfoRoute(bookId: Int) = "book_info/$bookId"
private const val READER_ARG = "bookId"
private const val READER_ROUTE = "reader/{$READER_ARG}"
private fun readerRoute(bookId: Int) = "reader/$bookId"

private const val DOUBLE_PRESS_BACK_WINDOW_MS = 2000L

/**
 * Unlike the sibling apps' single-hub-plus-drill-down shape, this app has three peer top-level
 * destinations a reader switches between constantly (Library/Browse/History), so — while the
 * Scaffold/top-bar/theme/navigation-compose backbone below matches Pixel Info and Pixel Photo
 * Slideshow exactly — a Material 3 [NavigationBar] is added for those three tabs. Settings
 * remains a pushed destination reached via the same top-bar gear icon convention as the
 * siblings, not a fourth tab. Book Info, Manage Categories, and About are further pushed
 * destinations, matching how the siblings push their own secondary screens (e.g. Licenses) on
 * top of Settings. The Reader is the one exception to all of the above — it's full-bleed with
 * its own internal top bar/progress bar (shown by default, toggled by tapping the reading area
 * or the immersive-mode icon), so the shared Scaffold chrome is suppressed entirely on that
 * route rather than layered on top.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PixelEBookReaderApp(navController: NavHostController = rememberNavController(), doublePressBackToExit: Boolean = false) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentDestination = Destination.fromRoute(currentRoute)
    val isTopLevel = currentDestination in Destination.bottomNavDestinations && currentRoute == currentDestination.route
    val isLibrary = currentRoute == Destination.Library.route
    val isReader = currentRoute == READER_ROUTE

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val titleText = when {
        currentRoute == MANAGE_CATEGORIES_ROUTE -> stringResource(R.string.title_manage_categories)
        currentRoute == BOOK_INFO_ROUTE -> stringResource(R.string.title_book_info)
        currentRoute == LICENSES_ROUTE -> stringResource(R.string.section_open_source_licenses)
        currentRoute == STATISTICS_ROUTE -> stringResource(R.string.title_statistics)
        else -> stringResource(currentDestination.titleRes)
    }

    // Only intercepts back on the Library ("home") route, and only when the user opted in —
    // otherwise back falls through to the platform default (exit immediately from home, pop
    // the nav stack elsewhere).
    val context = LocalContext.current
    var lastBackPressAt by remember { mutableLongStateOf(0L) }
    val exitWarning = stringResource(R.string.press_back_again_to_exit)
    BackHandler(enabled = isLibrary && doublePressBackToExit) {
        val now = System.currentTimeMillis()
        if (now - lastBackPressAt < DOUBLE_PRESS_BACK_WINDOW_MS) {
            (context as? Activity)?.finish()
        } else {
            lastBackPressAt = now
            Toast.makeText(context, exitWarning, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (!isReader) {
                CenteredCollapsingTopBar(
                    title = titleText,
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        if (isLibrary) {
                            Image(
                                painter = painterResource(R.drawable.ic_logo_mark),
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(start = 12.dp)
                                    .size(28.dp),
                            )
                        } else if (!isTopLevel) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = stringResource(R.string.action_back),
                                )
                            }
                        }
                    },
                    actions = {
                        if (isTopLevel) {
                            IconButton(onClick = { navController.navigate(Destination.Settings.route) }) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = stringResource(R.string.title_settings),
                                )
                            }
                        }
                    },
                )
            }
        },
        bottomBar = {
            if (isTopLevel) {
                NavigationBar {
                    Destination.bottomNavDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentDestination == destination,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(Destination.Library.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(imageVector = destination.icon, contentDescription = null) },
                            label = { Text(stringResource(destination.titleRes)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Library.route,
            modifier = if (isReader) Modifier else Modifier.padding(innerPadding),
        ) {
            composable(Destination.Library.route) {
                LibraryScreen(
                    onOpenBook = { bookId -> navController.navigate(readerRoute(bookId)) { launchSingleTop = true } },
                    onOpenBookInfo = { bookId -> navController.navigate(bookInfoRoute(bookId)) },
                    onManageCategories = { navController.navigate(MANAGE_CATEGORIES_ROUTE) },
                )
            }
            composable(Destination.Browse.route) {
                BrowseScreen(onOpenBook = { bookId -> navController.navigate(readerRoute(bookId)) { launchSingleTop = true } })
            }
            composable(Destination.History.route) {
                HistoryScreen(
                    onOpenBook = { bookId -> navController.navigate(readerRoute(bookId)) { launchSingleTop = true } },
                    onOpenBookInfo = { bookId -> navController.navigate(bookInfoRoute(bookId)) },
                )
            }
            composable(Destination.Settings.route) {
                SettingsScreen(
                    onNavigateToLicenses = { navController.navigate(LICENSES_ROUTE) },
                    onNavigateToStatistics = { navController.navigate(STATISTICS_ROUTE) },
                )
            }
            composable(MANAGE_CATEGORIES_ROUTE) { CategoryManagementScreen() }
            composable(LICENSES_ROUTE) { LicensesScreen() }
            composable(STATISTICS_ROUTE) { StatisticsScreen() }
            composable(
                route = BOOK_INFO_ROUTE,
                arguments = listOf(navArgument(BOOK_INFO_ARG) { type = NavType.IntType }),
            ) { entry ->
                val bookId = entry.arguments?.getInt(BOOK_INFO_ARG) ?: return@composable
                BookInfoScreen(bookId = bookId, onDeleted = { navController.popBackStack() }, onSaved = { navController.popBackStack() })
            }
            composable(
                route = READER_ROUTE,
                arguments = listOf(navArgument(READER_ARG) { type = NavType.IntType }),
            ) { entry ->
                val bookId = entry.arguments?.getInt(READER_ARG) ?: return@composable
                ReaderRouteScreen(bookId = bookId, onBack = { navController.popBackStack() })
            }
        }
    }
}

/** Picks the right reader for a book's file format — [PdfPageReaderScreen] for PDFs (the only
 * format with genuine fixed pages), [ReaderScreen] for everything else — so `onOpenBook` call
 * sites (Library/Browse/History) only ever need a `bookId`, not the file's extension. */
@Composable
private fun ReaderRouteScreen(bookId: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    var hasLoaded by remember(bookId) { mutableStateOf(false) }
    var fileName by remember(bookId) { mutableStateOf<String?>(null) }
    LaunchedEffect(bookId) {
        fileName = BookRepository.getBook(context, bookId).first()?.fileName
        hasLoaded = true
    }
    when {
        !hasLoaded -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        fileName != null && BookParsers.isPagedFormat(fileName!!) -> PdfPageReaderScreen(bookId = bookId, onBack = onBack)
        else -> ReaderScreen(bookId = bookId, onBack = onBack)
    }
}
