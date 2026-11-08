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
import com.streamcueapp.streamcue.model.TrackedMovie
import com.streamcueapp.streamcue.network.TmdbImage

private data class MovieSection(val title: String, val movies: List<TrackedMovie>, val accented: Boolean = false)

/**
 * Stands in for iOS's WatchlistView (Movies tab), grouped the same way:
 * Free to watch (accented, same colour rule as iOS), On a subscription,
 * Rent or buy, Not streaming yet — each from the unwatched list, in that
 * fixed order (iOS's `MovieAvailability.allCases`) — then Watched last,
 * collapsed by default (iOS's isWatchedExpanded starts false, unlike the
 * other sections). No suggestions strip on this screen yet — that's
 * Discover's and Services' job, not duplicated here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoviesScreen(
    onAddMovie: () -> Unit,
    onMovieClick: (Int) -> Unit,
    viewModel: MoviesViewModel = viewModel()
) {
    val movies by viewModel.movies.collectAsState()
    val collapsedSections by viewModel.collapsedSections.collectAsState()
    val watchedExpanded by viewModel.watchedExpanded.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Movies") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddMovie) {
                Icon(Icons.Default.Add, contentDescription = "Add a movie")
            }
        }
    ) { padding ->
        if (movies.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No movies tracked yet.")
            }
        } else {
            val unwatched = movies.filter { !it.watched }
            val watched = movies.filter { it.watched }
            val sections = groupMovies(unwatched)

            LazyColumn(modifier = Modifier.padding(padding)) {
                sections.forEach { section ->
                    val expanded = section.title !in collapsedSections
                    item(key = "header-${section.title}") {
                        SectionHeader(
                            title = section.title,
                            count = section.movies.size,
                            expanded = expanded,
                            accented = section.accented,
                            onClick = { viewModel.toggleSection(section.title) }
                        )
                    }
                    if (expanded) {
                        items(section.movies, key = { it.tmdbId }) { movie ->
                            MovieRow(movie, onClick = { onMovieClick(movie.tmdbId) })
                        }
                    }
                }
                if (watched.isNotEmpty()) {
                    item(key = "header-watched") {
                        SectionHeader(
                            title = "Watched",
                            count = watched.size,
                            expanded = watchedExpanded,
                            onClick = { viewModel.toggleWatchedExpanded() }
                        )
                    }
                    if (watchedExpanded) {
                        items(watched, key = { it.tmdbId }) { movie ->
                            MovieRow(movie, onClick = { onMovieClick(movie.tmdbId) })
                        }
                    }
                }
            }
        }
    }
}

/** Mirrors WatchlistView.swift's group(_:) over MovieAvailability.allCases. */
private fun groupMovies(unwatched: List<TrackedMovie>): List<MovieSection> {
    val free = unwatched.filter { it.freeOn.isNotEmpty() }
    val subscription = unwatched.filter { it.freeOn.isEmpty() && it.subscriptionOn.isNotEmpty() }
    val rentOrBuy = unwatched.filter {
        it.freeOn.isEmpty() && it.subscriptionOn.isEmpty() && it.rentOrBuyOn.isNotEmpty()
    }
    val unavailable = unwatched.filter {
        it.freeOn.isEmpty() && it.subscriptionOn.isEmpty() && it.rentOrBuyOn.isEmpty()
    }

    return listOfNotNull(
        free.takeIf { it.isNotEmpty() }?.let { MovieSection("Free to watch", it, accented = true) },
        subscription.takeIf { it.isNotEmpty() }?.let { MovieSection("On a subscription", it) },
        rentOrBuy.takeIf { it.isNotEmpty() }?.let { MovieSection("Rent or buy", it) },
        unavailable.takeIf { it.isNotEmpty() }?.let { MovieSection("Not streaming yet", it) },
    )
}

@Composable
private fun MovieRow(movie: TrackedMovie, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = TmdbImage.poster(movie.posterPath),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 46.dp, height = 69.dp)
                .clip(MaterialTheme.shapes.small)
        )
        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            Text(movie.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (movie.subtitle.isNotEmpty()) {
                Text(
                    movie.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
