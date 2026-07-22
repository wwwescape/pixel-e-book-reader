package com.wwwescape.pixelebookreader.ui.screens.stats

import java.time.LocalDate

/** One day's total reading time, in minutes — [dailyHistory] always covers a fixed trailing
 * window (see [StatsViewModel]) so the sparkline has a consistent x-axis even on days with zero
 * reading. */
data class DailyReadingPoint(val date: LocalDate, val minutes: Float)

data class StatsUiState(
    val totalReadingTimeMs: Long = 0L,
    val booksFinished: Int = 0,
    val chaptersRead: Int = 0,
    val currentStreakDays: Int = 0,
    val dailyHistory: List<DailyReadingPoint> = emptyList(),
    val hasActivity: Boolean = false,
)
