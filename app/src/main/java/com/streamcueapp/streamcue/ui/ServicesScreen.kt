package com.streamcueapp.streamcue.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.streamcueapp.streamcue.model.StreamingService
import com.streamcueapp.streamcue.network.TmdbImage

/**
 * Stands in for iOS's SubscriptionsView — the service picker, plus a
 * "popular on your services" strip per service (Shows and Movies,
 * narrowed to genres you already track). Trimmed vs. iOS: no free-tier
 * cap on selections (Premium/Billing doesn't exist on Android yet).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(
    onChooseServices: () -> Unit,
    onPreview: (DiscoverItem) -> Unit,
    viewModel: ServicesViewModel = viewModel()
) {
    val services by viewModel.services.collectAsState()
    val showPicks by viewModel.showPicks.collectAsState()
    val moviePicks by viewModel.moviePicks.collectAsState()
    val isLoadingPicks by viewModel.isLoadingPicks.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Services") },
                actions = {
                    IconButton(onClick = onChooseServices) {
                        Icon(Icons.Default.Checklist, contentDescription = "Choose services")
                    }
                }
            )
        }
    ) { padding ->
        if (services.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No services selected yet.")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(services, key = { it.id }) { service ->
                    Column {
                        ServiceRow(service)
                        val shows = showPicks[service.id].orEmpty()
                        val movies = moviePicks[service.id].orEmpty()
                        if (isLoadingPicks && shows.isEmpty() && movies.isEmpty()) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(start = 16.dp, bottom = 8.dp)
                                    .size(16.dp),
                                strokeWidth = 2.dp
                            )
                        }
                        if (shows.isNotEmpty()) {
                            SuggestionLabel("Shows")
                            SuggestionStrip(shows, onPreview)
                        }
                        if (movies.isNotEmpty()) {
                            SuggestionLabel("Movies")
                            SuggestionStrip(movies, onPreview)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceRow(service: StreamingService) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = TmdbImage.poster(service.logoPath, width = 92),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(28.dp)
                .clip(MaterialTheme.shapes.small)
        )
        Text(service.name, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun SuggestionLabel(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun SuggestionStrip(posters: List<DiscoverItem>, onPreview: (DiscoverItem) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 12.dp)
    ) {
        items(posters, key = { "${it.kind}-${it.id}" }) { item ->
            SuggestionPoster(item, onClick = { onPreview(item) })
        }
    }
}

@Composable
private fun SuggestionPoster(item: DiscoverItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .padding(end = 10.dp)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = TmdbImage.poster(item.posterPath, width = 185),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 80.dp, height = 120.dp)
                .clip(MaterialTheme.shapes.small)
        )
        Text(
            item.title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
