package com.streamcueapp.streamcue.ui

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

/** Stands in for iOS's WatchlistView (Movies tab) — grouping, detail, and
 * suggestions are still ahead; this is the same empty/populated vertical
 * slice HomeScreen started with, for movies instead of shows. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoviesScreen(onAddMovie: () -> Unit, viewModel: MoviesViewModel = viewModel()) {
    val movies by viewModel.movies.collectAsState()

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
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(movies, key = { it.tmdbId }) { movie ->
                    MovieRow(movie)
                }
            }
        }
    }
}

@Composable
private fun MovieRow(movie: TrackedMovie) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
