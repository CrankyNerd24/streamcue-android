package com.streamcueapp.streamcue.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.streamcueapp.streamcue.model.TrackedShow
import com.streamcueapp.streamcue.network.TmdbImage
import java.time.LocalDate

private val endedStatuses = setOf("Ended", "Canceled", "Cancelled")

private data class ShowSection(val title: String, val shows: List<TrackedShow>, val accented: Boolean = false)

/**
 * Stands in for ContentView.swift's Shows tab, grouped the same way:
 * Airing today (accented, same colour rule as iOS's Theme.tonight),
 * Airing next (dated, soonest first), No date announced, Finished — each a
 * collapsible SectionHeader, collapse state persisted the same way iOS
 * persists collapsedShowSections. No Ready to watch section yet — that's
 * iOS's EpisodeSync/PendingEpisode, a separate sync subsystem not ported
 * (see CLAUDE.md), and no Watch Now shortcut either.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddShow: () -> Unit,
    onShowClick: (Int) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val shows by viewModel.shows.collectAsState()
    val collapsedSections by viewModel.collapsedSections.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Shows") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddShow) {
                Icon(Icons.Default.Add, contentDescription = "Add a show")
            }
        }
    ) { padding ->
        if (shows.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No shows tracked yet.")
            }
        } else {
            val sections = groupShows(shows)
            LazyColumn(modifier = Modifier.padding(padding)) {
                sections.forEach { section ->
                    val expanded = section.title !in collapsedSections
                    item(key = "header-${section.title}") {
                        SectionHeader(
                            title = section.title,
                            count = section.shows.size,
                            expanded = expanded,
                            accented = section.accented,
                            onClick = { viewModel.toggleSection(section.title) }
                        )
                    }
                    if (expanded) {
                        items(section.shows, key = { it.tmdbId }) { show ->
                            ShowRow(show, onClick = { onShowClick(show.tmdbId) })
                        }
                    }
                }
            }
        }
    }
}

/** Mirrors ContentView.swift's dated/airingToday/upcoming/waiting/finished,
 * minus the Ready-to-watch exclusion (no such section exists here yet). */
private fun groupShows(shows: List<TrackedShow>): List<ShowSection> {
    val today = LocalDate.now().toEpochDay()
    val dated = shows
        .filter { it.effectiveAirDateEpochDay != null }
        .sortedBy { it.effectiveAirDateEpochDay }
    val airingToday = dated.filter { it.effectiveAirDateEpochDay == today }
    val airingTodayIds = airingToday.map { it.tmdbId }.toSet()
    val upcoming = dated.filter { it.tmdbId !in airingTodayIds }
    val undated = shows.filter { it.effectiveAirDateEpochDay == null }
    val finished = undated.filter { it.status in endedStatuses }
    val waiting = undated.filter { it.status !in endedStatuses }

    return listOfNotNull(
        airingToday.takeIf { it.isNotEmpty() }?.let { ShowSection("Airing today", it, accented = true) },
        upcoming.takeIf { it.isNotEmpty() }?.let { ShowSection("Airing next", it) },
        waiting.takeIf { it.isNotEmpty() }?.let { ShowSection("No date announced", it) },
        finished.takeIf { it.isNotEmpty() }?.let { ShowSection("Finished", it) },
    )
}

@Composable
private fun ShowRow(show: TrackedShow, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = TmdbImage.poster(show.posterPath),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 46.dp, height = 69.dp)
                .clip(MaterialTheme.shapes.small)
        )
        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            Text(show.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                show.scheduleSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
