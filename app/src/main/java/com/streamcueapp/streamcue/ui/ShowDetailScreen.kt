package com.streamcueapp.streamcue.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.streamcueapp.streamcue.model.PendingEpisode
import com.streamcueapp.streamcue.model.TrackedShow
import com.streamcueapp.streamcue.network.TmdbImage

/**
 * Stands in for iOS's ShowDetailView, laid out as the same kind of
 * collapsible, titled groups iOS's grouped List/Section style gives —
 * Episode synopsis and Episodes mirror iOS's actual DisclosureGroup/
 * "Recent episodes" collapse (both default collapsed, same as iOS);
 * Ratings and Where to watch are collapsible too, for a shorter screen,
 * but default open since iOS always shows them. Episodes is now backed by
 * the real PendingEpisode/EpisodeSync system (see data/EpisodeSync.kt),
 * each row with a watched/undo action, same as iOS's EpisodeStatusRow.
 * Still trimmed vs. iOS: no household sharing, no reminders, no cast, no
 * custom Watch-on override — see CLAUDE.md for what's still ahead.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowDetailScreen(
    tmdbId: Int,
    onBack: () -> Unit,
    viewModel: ShowDetailViewModel = viewModel()
) {
    LaunchedEffect(tmdbId) { viewModel.load(tmdbId) }
    val show by viewModel.show.collectAsState()
    val episodes by viewModel.episodes.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(show?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.delete()
                        onBack()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove")
                    }
                }
            )
        }
    ) { padding ->
        val current = show
        if (current == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else {
            ShowDetailBody(
                show = current,
                episodes = episodes,
                modifier = Modifier.padding(padding),
                onWatchedChange = viewModel::setWatched,
                onDayOffsetChange = viewModel::setDayOffset,
                onEpisodeWatchedChange = viewModel::setEpisodeWatched
            )
        }
    }
}

@Composable
private fun ShowDetailBody(
    show: TrackedShow,
    episodes: List<PendingEpisode>,
    modifier: Modifier = Modifier,
    onWatchedChange: (Boolean) -> Unit,
    onDayOffsetChange: (Int) -> Unit,
    onEpisodeWatchedChange: (PendingEpisode, Boolean) -> Unit
) {
    val context = LocalContext.current
    var showDayOffsetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Row(modifier = Modifier.padding(top = 16.dp)) {
            AsyncImage(
                model = TmdbImage.poster(show.posterPath, width = 342),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(120.dp)
                    .aspectRatio(2f / 3f)
                    .clip(MaterialTheme.shapes.medium)
            )
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(show.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    show.status.ifEmpty { "—" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Watched")
                    Switch(
                        checked = show.watched,
                        onCheckedChange = onWatchedChange,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        if (show.overview.isNotEmpty()) {
            DetailSection(title = "Overview") {
                Text(show.overview, style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (show.hasAnyRating) {
            DetailSection(title = "Ratings") {
                Row {
                    show.imdbScore?.let { ScoreBadge("IMDb", it) }
                    show.rottenTomatoesScore?.let { ScoreBadge("Rotten Tomatoes", it) }
                    show.metacriticScore?.let { ScoreBadge("Metacritic", it) }
                    if ((show.tmdbScore ?: 0.0) > 0.0) {
                        ScoreBadge("TMDB", "%.1f".format(show.tmdbScore))
                    }
                }
            }
        }

        DetailSection(title = "Schedule") {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Next", modifier = Modifier.weight(1f))
                Text(
                    show.scheduleSummary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!show.nextEpisodeOverview.isNullOrEmpty()) {
                Text(
                    show.nextEpisodeOverview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Day offset", modifier = Modifier.weight(1f))
                IconButton(onClick = { onDayOffsetChange(show.dayOffset - 1) }) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease day offset")
                }
                Text(
                    show.dayOffsetLabel,
                    color = if (show.dayOffset == 0) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clickable { showDayOffsetDialog = true }
                )
                IconButton(onClick = { onDayOffsetChange(show.dayOffset + 1) }) {
                    Icon(Icons.Default.Add, contentDescription = "Increase day offset")
                }
            }
            Text(
                "Air dates are the original broadcaster's. Use the offset if this reaches you on a different day.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            show.lastEpisodeLabel?.let { last ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Text("Last aired", modifier = Modifier.weight(1f))
                    Text(last, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (episodes.isNotEmpty()) {
            DetailSection(
                title = "Episodes",
                initiallyExpanded = false,
                trailingLabel = episodes.size.toString()
            ) {
                episodes.forEach { episode ->
                    EpisodeStatusRow(
                        episode = episode,
                        onSetWatched = { watched -> onEpisodeWatchedChange(episode, watched) }
                    )
                }
            }
        }

        if (show.freeOn.isNotEmpty() || show.subscriptionOn.isNotEmpty() || show.rentOrBuyOn.isNotEmpty()) {
            DetailSection(title = "Where to watch") {
                LazyRow {
                    items(show.freeOn) { ProviderChip(it, isFree = true) }
                    items(show.subscriptionOn) { ProviderChip(it, isFree = false) }
                    items(show.rentOrBuyOn) { ProviderChip(it, isFree = false) }
                }
                show.watchLink?.let { link ->
                    Button(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Watch now")
                    }
                }
            }
        }

        show.lastRefreshedEpochMillis?.let {
            Text(
                "Updated ${relativeTime(it)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
    }

    if (showDayOffsetDialog) {
        DayOffsetDialog(
            current = show.dayOffset,
            onSave = { onDayOffsetChange(it); showDayOffsetDialog = false },
            onDismiss = { showDayOffsetDialog = false }
        )
    }
}

@Composable
private fun DayOffsetDialog(current: Int, onSave: (Int) -> Unit, onDismiss: () -> Unit) {
    var draft by remember { mutableStateOf(current.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Day offset") },
        text = {
            Column {
                Text(
                    "How many days after the original air date this reaches you. " +
                        "Negative if it airs early.",
                    style = MaterialTheme.typography.bodyMedium
                )
                // Default (text) keyboard, not a numeric pad — Android's
                // Number keyboard usually has no minus key, and negative
                // offsets (airs early) are a real case here.
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = { Text("Days, e.g. 233") },
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                draft.trim().toIntOrNull()?.let(onSave) ?: onDismiss()
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * One recorded episode with its state and the action that flips it: mark
 * an outstanding one watched, or undo a watched or dismissed one. Mirrors
 * iOS's EpisodeStatusRow.
 */
@Composable
private fun EpisodeStatusRow(episode: PendingEpisode, onSetWatched: (Boolean) -> Unit) {
    val isOutstanding = !episode.watched && !episode.dismissed
    val status = when {
        episode.watched -> "Watched"
        episode.dismissed -> "Dismissed"
        else -> episode.airedSummary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(episode.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                status,
                style = MaterialTheme.typography.bodySmall,
                color = if (episode.watched) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        if (isOutstanding) {
            IconButton(onClick = { onSetWatched(true) }) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Mark watched",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        } else {
            TextButton(onClick = { onSetWatched(false) }) {
                Text("Undo")
            }
        }
    }
}
