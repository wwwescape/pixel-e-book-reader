package com.wwwescape.pixelebookreader.ui.screens.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wwwescape.pixelebookreader.R
import com.wwwescape.pixelebookreader.ui.components.EmptyStateNotice
import com.wwwescape.pixelebookreader.ui.components.SparklineChart
import com.wwwescape.pixelebookreader.ui.components.StatCard
import com.wwwescape.pixelebookreader.ui.components.StatRow

@Composable
fun StatisticsScreen(modifier: Modifier = Modifier, viewModel: StatsViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    if (!uiState.hasActivity) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyStateNotice(
                title = stringResource(R.string.stats_empty_title),
                body = stringResource(R.string.stats_empty_body),
                icon = Icons.AutoMirrored.Rounded.MenuBook,
                modifier = Modifier.padding(16.dp),
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StatCard(title = stringResource(R.string.stats_totals)) {
            StatRow(stringResource(R.string.stats_total_time), readingDurationText(uiState.totalReadingTimeMs))
            StatRow(
                stringResource(R.string.stats_current_streak),
                pluralStringResource(R.plurals.stats_streak_days, uiState.currentStreakDays, uiState.currentStreakDays),
            )
            StatRow(stringResource(R.string.stats_books_finished), uiState.booksFinished.toString())
            StatRow(stringResource(R.string.stats_chapters_read), uiState.chaptersRead.toString())
        }

        StatCard(title = stringResource(R.string.stats_history)) {
            ReadingHistoryChart(uiState.dailyHistory)
        }
    }
}

@Composable
private fun ReadingHistoryChart(history: List<DailyReadingPoint>, modifier: Modifier = Modifier) {
    if (history.size < 2 || history.all { it.minutes <= 0f }) {
        Box(modifier = modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.stats_history_placeholder),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    val maxMinutes = history.maxOf { it.minutes }.coerceAtLeast(1f)
    val description = stringResource(R.string.stats_history_content_description)
    Column(modifier = modifier) {
        SparklineChart(values = history.map { it.minutes / maxMinutes }, contentDescription = description, modifier = Modifier.fillMaxWidth())
        Text(
            text = stringResource(R.string.stats_history_caption),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun readingDurationText(ms: Long): String {
    val totalMinutes = ms / 60_000L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        stringResource(R.string.stats_duration_hours_minutes, hours, minutes)
    } else {
        stringResource(R.string.stats_duration_minutes, minutes)
    }
}
