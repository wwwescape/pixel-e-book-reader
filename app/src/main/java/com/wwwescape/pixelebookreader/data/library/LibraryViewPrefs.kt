package com.wwwescape.pixelebookreader.data.library

import com.wwwescape.pixelebookreader.data.category.LibrarySortOrder

enum class LibraryLayout { GRID, LIST }
enum class TitlePosition { BELOW, OVERLAY }

/** Per-screen view options, not app-wide settings (a deliberate "Settings, two-tier"
 * architecture decision) — reached from a display-options control on the Library screen
 * itself, not the main Settings screen. */
data class LibraryViewPrefs(
    val layout: LibraryLayout = LibraryLayout.GRID,
    val titlePosition: TitlePosition = TitlePosition.BELOW,
    val showProgress: Boolean = true,
    val showBookCount: Boolean = true,
    val defaultSortOrder: LibrarySortOrder = LibrarySortOrder.LAST_READ,
    val defaultSortDescending: Boolean = true,
)
