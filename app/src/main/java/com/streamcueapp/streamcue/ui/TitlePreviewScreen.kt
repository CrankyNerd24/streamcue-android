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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.streamcueapp.streamcue.network.TmdbImage

/**
 * Unsaved-title preview, the Android analog of iOS's TitlePreviewSheet.
 * Add, Watched, and Not interested each navigate back after acting, same
 * as iOS dismissing the sheet after any of its three actions — Watched
 * and Not interested only show for something not already tracked. Reached
 * by tapping a Discover tile's poster or a Services suggestion; a tile's
 * own explicit Add icon still adds directly without opening this screen,
 * same split AddShowScreen/AddMovieScreen rows already use.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitlePreviewScreen(
    kind: DiscoverKind,
    tmdbId: Int,
    onBack: () -> Unit,
    viewModel: TitlePreviewViewModel = viewModel()
) {
    LaunchedEffect(kind, tmdbId) { viewModel.load(kind, tmdbId) }
    val preview by viewModel.preview.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isTracked by viewModel.isTracked.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(preview?.title.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val current = preview
        when {
            isLoading -> CenteredBox(padding) { CircularProgressIndicator() }
            error != null -> CenteredBox(padding) {
                Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
            }
            current != null -> TitlePreviewBody(
                preview = current,
                isTracked = isTracked,
                modifier = Modifier.padding(padding),
                onAdd = { viewModel.add(); onBack() },
                onMarkWatched = { viewModel.add(watched = true); onBack() },
                onNotInterested = { viewModel.ignore(); onBack() }
            )
        }
    }
}

@Composable
private fun CenteredBox(
    padding: androidx.compose.foundation.layout.PaddingValues,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun TitlePreviewBody(
    preview: TitlePreview,
    isTracked: Boolean,
    modifier: Modifier = Modifier,
    onAdd: () -> Unit,
    onMarkWatched: () -> Unit,
    onNotInterested: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row {
            AsyncImage(
                model = TmdbImage.poster(preview.posterPath, width = 342),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(120.dp)
                    .aspectRatio(2f / 3f)
                    .clip(MaterialTheme.shapes.medium)
            )
            Column(modifier = Modifier.padding(start = 16.dp)) {
                if (preview.scheduleOrSubtitle.isNotEmpty()) {
                    Text(preview.scheduleOrSubtitle, style = MaterialTheme.typography.bodyMedium)
                }
                preview.headlineRating?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isTracked) {
                    Row(
                        modifier = Modifier.padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text("Tracked", modifier = Modifier.padding(start = 4.dp))
                    }
                } else {
                    Button(onClick = onAdd, modifier = Modifier.padding(top = 12.dp)) {
                        Text("Add")
                    }
                }
            }
        }

        // Same as iOS's TitlePreviewSheet: only offered for something not
        // already tracked — once it's on your list, Watched lives on its
        // detail screen, and Not interested no longer means anything.
        if (!isTracked) {
            Row(modifier = Modifier.padding(top = 12.dp)) {
                TextButton(onClick = onMarkWatched) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text("Watched", color = MaterialTheme.colorScheme.secondary)
                }
                TextButton(onClick = onNotInterested) {
                    Icon(
                        Icons.Default.ThumbDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text("Not interested", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (preview.freeOn.isNotEmpty() || preview.subscriptionOn.isNotEmpty() || preview.rentOrBuyOn.isNotEmpty()) {
            Text(
                "Where to watch",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
            )
            LazyRow {
                items(preview.freeOn) { ProviderChip(it, isFree = true) }
                items(preview.subscriptionOn) { ProviderChip(it, isFree = false) }
                items(preview.rentOrBuyOn) { ProviderChip(it, isFree = false) }
            }
        }

        preview.watchLink?.let { link ->
            Button(
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) },
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text("Watch now")
            }
        }

        if (preview.overview.isNotEmpty()) {
            Text(
                preview.overview,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 20.dp)
            )
        }
    }
}
