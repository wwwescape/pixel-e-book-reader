package com.wwwescape.pixelebookreader.ui.screens.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wwwescape.pixelebookreader.data.book.BookRepository
import com.wwwescape.pixelebookreader.data.stats.ReadingSessionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private const val HISTORY_DAYS = 14L

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    val uiState: StateFlow<StatsUiState> = combine(
        BookRepository.getAllBooks(application),
        ReadingSessionRepository.getAllSessions(application),
    ) { books, sessions ->
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val minutesByDay = sessions
            .groupBy { Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() }
            .mapValues { (_, daySessions) -> daySessions.sumOf { it.durationMs }.toFloat() / 60_000f }
        val sessionDays = minutesByDay.keys

        // Counts from today backward (or yesterday backward if today has no session yet, so the
        // streak isn't zeroed out just because the user hasn't opened a book yet today).
        var streak = 0
        var day = if (today in sessionDays) today else today.minusDays(1)
        while (day in sessionDays) {
            streak++
            day = day.minusDays(1)
        }

        val history = ((HISTORY_DAYS - 1) downTo 0).map { offset ->
            val date = today.minusDays(offset)
            DailyReadingPoint(date, minutesByDay[date] ?: 0f)
        }

        StatsUiState(
            totalReadingTimeMs = sessions.sumOf { it.durationMs },
            booksFinished = books.count { it.finishedAt != null },
            chaptersRead = books.sumOf { it.chaptersRead },
            currentStreakDays = streak,
            dailyHistory = history,
            hasActivity = sessions.isNotEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())
}
