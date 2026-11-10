package com.streamcueapp.streamcue.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.streamcueapp.streamcue.model.PendingEpisode
import com.streamcueapp.streamcue.model.TrackedShow
import com.streamcueapp.streamcue.network.TmdbImage
import com.streamcueapp.streamcue.ui.theme.StreamCueUnwatched
import kotlinx.coroutines.launch
import java.time.LocalDate

private val endedStatuses = setOf("Ended", "Canceled", "Cancelled")

private data class ShowSection(val title: String, val shows: List<TrackedShow>)

/**
 * Stands in for ContentView.swift's Shows tab, grouped the same way:
 * Airing today (accented), Ready to watch (amber, actionable — persisted
 * per-episode watched/dismissed state via PendingEpisode/EpisodeSync, see
 * CLAUDE.md), Airing next, No date announced, Finished — each a
 * collapsible SectionHeader, collapse state persisted the same way iOS
 * persists collapsedShowSections/readyExpanded. Pull to refresh re-fetches
 * every show and syncs episodes, same as iOS's refreshable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddShow: () -> Unit,
    onShowClick: (Int) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val shows by viewModel.shows.collectAsState()
    val pendingEpisodes by viewModel.pendingEpisodes.collectAsState()
    val actionedEpisodeKeys by viewModel.actionedEpisodeKeys.collectAsState()
    val collapsedSections by viewModel.collapsedSections.collectAsState()
    val readyExpanded by viewModel.readyExpanded.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun actOnEpisode(episode: PendingEpisode, watched: Boolean) {
        scope.launch {
            val caughtUp = if (watched) viewModel.markWatched(episode) else viewModel.dismiss(episode)
            val result = snackbarHostState.showSnackbar(
                message = if (watched) "Marked watched" else "Dismissed",
                actionLabel = "Undo"
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoEpisodeAction(episode, caughtUp)
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Shows") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddShow) {
                Icon(Icons.Default.Add, contentDescription = "Add a show")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
            val readyShowIds = pendingEpisodes.map { it.showId }.toSet()
            val groups = groupShows(shows, readyShowIds, actionedEpisodeKeys)
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = viewModel::refreshAll,
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    // Fixed order, same as iOS: Airing today, Ready to
                    // watch, Airing next, No date announced, Finished —
                    // each independently shown only when non-empty.
                    if (groups.airingToday.isNotEmpty()) {
                        item(key = "header-Airing today") {
                            SectionHeader(
                                title = "Airing today",
                                count = groups.airingToday.size,
                                expanded = "Airing today" !in collapsedSections,
                                accented = true,
                                onClick = { viewModel.toggleSection("Airing today") }
                            )
                        }
                        if ("Airing today" !in collapsedSections) {
                            items(groups.airingToday, key = { it.tmdbId }) { show ->
                                ShowRow(show, onClick = { onShowClick(show.tmdbId) })
                            }
                        }
                    }

                    if (pendingEpisodes.isNotEmpty()) {
                        item(key = "header-ready") {
                            SectionHeader(
                                title = "Ready to watch",
                                count = pendingEpisodes.size,
                                expanded = readyExpanded,
                                accented = true,
                                accentColor = StreamCueUnwatched,
                                onClick = viewModel::toggleReadyExpanded
                            )
                        }
                        if (readyExpanded) {
                            items(pendingEpisodes, key = { "ready-${it.key}" }) { episode ->
                                PendingEpisodeCard(
                                    episode = episode,
                                    onClick = { onShowClick(episode.showId) },
                                    onWatched = { actOnEpisode(episode, watched = true) },
                                    onDismiss = { actOnEpisode(episode, watched = false) }
                                )
                            }
                        }
                    }

                    listOf(
                        ShowSection("Airing next", groups.upcoming),
                        ShowSection("No date announced", groups.waiting),
                        ShowSection("Finished", groups.finished),
                    ).forEach { section ->
                        if (section.shows.isEmpty()) return@forEach
                        val expanded = section.title !in collapsedSections
                        item(key = "header-${section.title}") {
                            SectionHeader(
                                title = section.title,
                                count = section.shows.size,
                                expanded = expanded,
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
}

/** Mirrors ContentView.swift's dated/airingToday/upcoming/waiting/finished.
 * A show with a confirmed air date is dropped from "dated" once it's
 * already fully represented in Ready to watch — either generically (any
 * outstanding episode) or because its specific next episode was already
 * recorded and actioned — so it isn't listed twice. No such exclusion for
 * undated shows: Ready to watch and "No date announced"/"Finished" track
 * different things and can legitimately both list the same show. */
private data class ShowGroups(
    val airingToday: List<TrackedShow>,
    val upcoming: List<TrackedShow>,
    val waiting: List<TrackedShow>,
    val finished: List<TrackedShow>,
)

private fun groupShows(
    shows: List<TrackedShow>,
    readyShowIds: Set<Int>,
    actionedEpisodeKeys: Set<String>
): ShowGroups {
    val today = LocalDate.now().toEpochDay()
    val dated = shows
        .filter { show ->
            if (show.effectiveAirDateEpochDay == null) return@filter false
            if (show.tmdbId in readyShowIds) return@filter false
            val next = show.nextEpisodeSeasonAndNumber ?: return@filter true
            PendingEpisode.key(show.tmdbId, next.first, next.second) !in actionedEpisodeKeys
        }
        .sortedBy { it.effectiveAirDateEpochDay }
    val airingToday = dated.filter { it.effectiveAirDateEpochDay == today }
    val airingTodayIds = airingToday.map { it.tmdbId }.toSet()
    val upcoming = dated.filter { it.tmdbId !in airingTodayIds }
    val undated = shows.filter { it.effectiveAirDateEpochDay == null }
    val finished = undated.filter { it.status in endedStatuses }
    val waiting = undated.filter { it.status !in endedStatuses }

    return ShowGroups(airingToday, upcoming, waiting, finished)
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

/**
 * One outstanding episode: tapping the poster/text opens the show (same
 * split as everywhere else), while the checkmark and X act directly.
 * Mirrors iOS's PendingEpisodeCard, minus the Watch now shortcut (rides
 * Premium/StreamingServices, not wired on Android yet).
 */
@Composable
private fun PendingEpisodeCard(
    episode: PendingEpisode,
    onClick: () -> Unit,
    onWatched: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = TmdbImage.poster(episode.posterPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 44.dp, height = 66.dp)
                    .clip(MaterialTheme.shapes.small)
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(episode.showName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    episode.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = StreamCueUnwatched,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    episode.airedSummary,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(onClick = onWatched) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Mark watched",
                tint = MaterialTheme.colorScheme.secondary
            )
        }
        IconButton(onClick = onDismiss) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Dismiss",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
