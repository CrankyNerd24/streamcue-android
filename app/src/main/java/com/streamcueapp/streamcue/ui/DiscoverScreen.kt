package com.streamcueapp.streamcue.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.streamcueapp.streamcue.network.TmdbImage

/**
 * Stands in for iOS's DiscoverView: For you / Trending / Popular, each for
 * Shows and Movies. Tapping a poster never adds anything, same rule as
 * iOS — it opens TitlePreviewScreen instead; the tile's own add icon is a
 * separate, explicit affordance that still adds directly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    onPreview: (DiscoverItem) -> Unit,
    viewModel: DiscoverViewModel = viewModel()
) {
    val kind by viewModel.kind.collectAsState()
    val feed by viewModel.feed.collectAsState()
    val items by viewModel.items.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val trackedShowIds by viewModel.trackedShowIds.collectAsState()
    val trackedMovieIds by viewModel.trackedMovieIds.collectAsState()
    val ignoredShowIds by viewModel.ignoredShowIds.collectAsState()
    val ignoredMovieIds by viewModel.ignoredMovieIds.collectAsState()
    val trackedIds = if (kind == DiscoverKind.TV) trackedShowIds else trackedMovieIds
    val ignoredIds = if (kind == DiscoverKind.TV) ignoredShowIds else ignoredMovieIds
    val hasNoSeeds = feed == DiscoverFeed.FOR_YOU && trackedIds.isEmpty()
    // Already-tracked and "Not interested" titles never show here, same as
    // iOS's visibleShows/visibleMovies filtering out owned + hidden.
    val visibleItems = items.filterNot { it.id in trackedIds || it.id in ignoredIds }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Discover") }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ToggleButton("Shows", kind == DiscoverKind.TV) { viewModel.setKind(DiscoverKind.TV) }
                ToggleButton("Movies", kind == DiscoverKind.MOVIES) { viewModel.setKind(DiscoverKind.MOVIES) }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ToggleButton("For you", feed == DiscoverFeed.FOR_YOU) { viewModel.setFeed(DiscoverFeed.FOR_YOU) }
                ToggleButton("Trending", feed == DiscoverFeed.TRENDING) { viewModel.setFeed(DiscoverFeed.TRENDING) }
                ToggleButton("Popular", feed == DiscoverFeed.POPULAR) { viewModel.setFeed(DiscoverFeed.POPULAR) }
            }

            when {
                isLoading -> CenteredBox { CircularProgressIndicator() }
                error != null -> CenteredBox {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                }
                hasNoSeeds -> CenteredBox {
                    Text(
                        if (kind == DiscoverKind.TV) {
                            "Track a few shows and this fills up with things like them."
                        } else {
                            "Add a few films and this fills up with things like them."
                        },
                        modifier = Modifier.padding(horizontal = 32.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                visibleItems.isEmpty() -> CenteredBox { Text("Nothing here right now.") }
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(104.dp),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(visibleItems, key = { "${it.kind}-${it.id}" }) { item ->
                        DiscoverTile(
                            item = item,
                            onAdd = { viewModel.add(item) },
                            onClick = { onPreview(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        content()
    }
}

@Composable
private fun ToggleButton(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick) { Text(label) }
    }
}

@Composable
private fun DiscoverTile(
    item: DiscoverItem,
    onAdd: () -> Unit,
    onClick: () -> Unit
) {
    Column {
        Box(modifier = Modifier.clickable(onClick = onClick)) {
            AsyncImage(
                model = TmdbImage.poster(item.posterPath, width = 185),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(MaterialTheme.shapes.small)
            )
            // Tapping Add removes this tile from visibleItems on the next
            // recomposition (trackedIds picks it up), so there's no tracked
            // state to show here — it simply won't be here anymore.
            IconButton(onClick = onAdd, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
        Text(
            item.title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
