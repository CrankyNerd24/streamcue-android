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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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

/** The Movies-tab analog of ShowDetailScreen — see it for the shared layout reasoning. */
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
            .padding(16.dp)
    ) {
        Row {
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
                if (movie.subtitle.isNotEmpty()) {
                    Text(movie.subtitle, style = MaterialTheme.typography.bodyMedium)
                }
                movie.headlineRating?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

        if (movie.freeOn.isNotEmpty() || movie.subscriptionOn.isNotEmpty() || movie.rentOrBuyOn.isNotEmpty()) {
            Text(
                "Where to watch",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
            )
            LazyRow {
                items(movie.freeOn) { ProviderChip(it, isFree = true) }
                items(movie.subscriptionOn) { ProviderChip(it, isFree = false) }
                items(movie.rentOrBuyOn) { ProviderChip(it, isFree = false) }
            }
        }

        movie.watchLink?.let { link ->
            Button(
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) },
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text("Watch now")
            }
        }

        if (movie.overview.isNotEmpty()) {
            Text(
                movie.overview,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 20.dp)
            )
        }
    }
}

@Composable
private fun ProviderChip(name: String, isFree: Boolean) {
    AssistChip(
        onClick = {},
        label = { Text(name) },
        modifier = Modifier.padding(end = 8.dp),
        colors = if (isFree) {
            AssistChipDefaults.assistChipColors(labelColor = MaterialTheme.colorScheme.secondary)
        } else {
            AssistChipDefaults.assistChipColors()
        }
    )
}
