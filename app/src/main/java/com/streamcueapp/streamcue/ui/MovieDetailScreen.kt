package com.streamcueapp.streamcue.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.streamcueapp.streamcue.model.TrackedMovie
import com.streamcueapp.streamcue.network.TmdbImage

/** The Movies-tab analog of ShowDetailScreen — same collapsible
 * `DetailSection` layout, trimmed to what movies actually have (no
 * schedule, no episodes). See ShowDetailScreen for the shared reasoning. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(
    tmdbId: Int,
    onBack: () -> Unit,
    viewModel: MovieDetailViewModel = viewModel()
) {
    LaunchedEffect(tmdbId) { viewModel.load(tmdbId) }
    val movie by viewModel.movie.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(movie?.title.orEmpty()) },
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
        val current = movie
        if (current == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else {
            MovieDetailBody(
                movie = current,
                modifier = Modifier.padding(padding),
                onWatchedChange = viewModel::setWatched
            )
        }
    }
}

@Composable
private fun MovieDetailBody(
    movie: TrackedMovie,
    modifier: Modifier = Modifier,
    onWatchedChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Row(modifier = Modifier.padding(top = 16.dp)) {
            AsyncImage(
                model = TmdbImage.poster(movie.posterPath, width = 342),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(120.dp)
                    .aspectRatio(2f / 3f)
                    .clip(MaterialTheme.shapes.medium)
            )
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(movie.title, style = MaterialTheme.typography.titleMedium)
                if (movie.subtitle.isNotEmpty()) {
                    Text(
                        movie.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Watched")
                    Switch(
                        checked = movie.watched,
                        onCheckedChange = onWatchedChange,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        if (movie.overview.isNotEmpty()) {
            DetailSection(title = "Overview") {
                Text(movie.overview, style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (movie.hasAnyRating) {
            DetailSection(title = "Ratings") {
                Row {
                    movie.imdbScore?.let { ScoreBadge("IMDb", it) }
                    movie.rottenTomatoesScore?.let { ScoreBadge("Rotten Tomatoes", it) }
                    movie.metacriticScore?.let { ScoreBadge("Metacritic", it) }
                    if ((movie.tmdbScore ?: 0.0) > 0.0) {
                        ScoreBadge("TMDB", "%.1f".format(movie.tmdbScore))
                    }
                }
            }
        }

        if (movie.freeOn.isNotEmpty() || movie.subscriptionOn.isNotEmpty() || movie.rentOrBuyOn.isNotEmpty()) {
            DetailSection(title = "Where to watch") {
                LazyRow {
                    items(movie.freeOn) { ProviderChip(it, isFree = true) }
                    items(movie.subscriptionOn) { ProviderChip(it, isFree = false) }
                    items(movie.rentOrBuyOn) { ProviderChip(it, isFree = false) }
                }
                movie.watchLink?.let { link ->
                    Button(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Watch now")
                    }
                }
            }
        }

        movie.lastRefreshedEpochMillis?.let {
            Text(
                "Updated ${relativeTime(it)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
    }
}
