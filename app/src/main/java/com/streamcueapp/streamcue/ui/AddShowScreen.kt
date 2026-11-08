package com.streamcueapp.streamcue.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.streamcueapp.streamcue.network.TmdbShowSearchResult

/** The Android analog of iOS's LookupView, scoped to TV search for now. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddShowScreen(
    onDone: () -> Unit,
    onPreview: (Int) -> Unit,
    viewModel: AddShowViewModel = viewModel()
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val error by viewModel.error.collectAsState()
    val trackedIds by viewModel.trackedIds.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add a show") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search TV shows") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            when {
                isSearching -> CenteredMessage { CircularProgressIndicator() }
                error != null -> CenteredMessage {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                }
                results.isEmpty() && query.isNotBlank() -> CenteredMessage {
                    Text("No shows found.")
                }
                else -> LazyColumn {
                    items(results, key = { it.id }) { result ->
                        SearchResultRow(
                            result = result,
                            alreadyTracked = result.id in trackedIds,
                            onAdd = { viewModel.addShow(result) },
                            onClick = { onPreview(result.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CenteredMessage(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        content()
    }
}

@Composable
private fun SearchResultRow(
    result: TmdbShowSearchResult,
    alreadyTracked: Boolean,
    onAdd: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = TmdbImage.poster(result.posterPath),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 46.dp, height = 69.dp)
                .clip(MaterialTheme.shapes.small)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(result.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            result.year?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (alreadyTracked) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Already tracked",
                tint = MaterialTheme.colorScheme.secondary
            )
        } else {
            IconButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    }
}
