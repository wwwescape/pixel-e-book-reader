package com.wwwescape.pixelebookreader.ui.screens.history

import com.wwwescape.pixelebookreader.data.history.HistoryWithBook
import java.time.LocalDate

data class HistorySection(val date: LocalDate, val entries: List<HistoryWithBook>)

data class HistoryUiState(
    val sections: List<HistorySection> = emptyList(),
    val searchQuery: String = "",
    val isEmpty: Boolean = true,
)
