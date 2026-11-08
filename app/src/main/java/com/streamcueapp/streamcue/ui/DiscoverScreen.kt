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
import androidx.compose.material.icons.filled.Check
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
 * Stands in for iOS's DiscoverView, trimmed to Trending/Popular — "For you"
 * is assembled from the user's own tracked-title genres on iOS and isn't
 * ported yet (see CLAUDE.md). Tapping a poster never adds anything, same
 * rule as iOS — it opens TitlePreviewScreen instead; the tile's own add
 * icon is a separate, explicit affordance that still adds directly.
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
    val trackedIds = if (kind == DiscoverKind.TV) trackedShowIds else trackedMovieIds

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
                ToggleButton("Trending", feed == DiscoverFeed.TRENDING) { viewModel.setFeed(DiscoverFeed.TRENDING) }
                ToggleButton("Popular", feed == DiscoverFeed.POPULAR) { viewModel.setFeed(DiscoverFeed.POPULAR) }
            }

            when {
                isLoading -> CenteredBox { CircularProgressIndicator() }
                error != null -> CenteredBox {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                }
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(104.dp),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items, key = { "${it.kind}-${it.id}" }) { item ->
                        DiscoverTile(
                            item = item,
                            alreadyTracked = item.id in trackedIds,
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
    alreadyTracked: Boolean,
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
            IconButton(onClick = onAdd, modifier = Modifier.align(Alignment.TopEnd)) {
                if (alreadyTracked) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Already tracked",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                } else {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
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
