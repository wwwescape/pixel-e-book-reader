package com.wwwescape.pixelebookreader.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.wwwescape.pixelebookreader.R

/**
 * Every screen reachable from the app's own top-level navigation. Unlike Pixel Info/Pixel
 * Photo Slideshow's single-hub-plus-drill-down shape, this app has three peer destinations a
 * reader switches between constantly (Library, Browse, History) — [bottomNavDestinations]
 * marks those three for the Scaffold's bottom navigation bar; Settings is reached by the same
 * top-bar gear icon convention the sibling apps use.
 */
enum class Destination(
    val route: String,
    val titleRes: Int,
    val icon: ImageVector,
) {
    Library("library", R.string.title_library, Icons.AutoMirrored.Rounded.LibraryBooks),
    Browse("browse", R.string.title_browse, Icons.Rounded.Search),
    History("history", R.string.title_history, Icons.Rounded.History),
    Settings("settings", R.string.title_settings, Icons.Rounded.Settings);

    companion object {
        val bottomNavDestinations = listOf(Library, Browse, History)
        fun fromRoute(route: String?): Destination = entries.find { it.route == route } ?: Library
    }
}
