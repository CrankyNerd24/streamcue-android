package com.streamcueapp.streamcue.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.streamcueapp.streamcue.network.TmdbImage
import com.streamcueapp.streamcue.network.TmdbProviderInfo

/** The Android analog of iOS's ServicePickerView — no free-tier cap yet
 * since Premium/Billing isn't wired up on this platform at all (see
 * CLAUDE.md); add one alongside that work, not before it exists. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicePickerScreen(onDone: () -> Unit, viewModel: ServicePickerViewModel = viewModel()) {
    val all by viewModel.all.collectAsState()
    val chosen by viewModel.chosen.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your services") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.save(onDone) }) {
                        Text("Done")
                    }
                }
            )
        }
    ) { padding ->
        when {
            isLoading -> CenteredBox(padding) { CircularProgressIndicator() }
            error != null -> CenteredBox(padding) {
                Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
            }
            else -> LazyColumn(modifier = Modifier.padding(padding)) {
                items(all, key = { it.providerId }) { provider ->
                    ProviderRow(
                        provider = provider,
                        selected = provider.providerId in chosen,
                        onToggle = { viewModel.toggle(provider.providerId) }
                    )
                }
            }
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
    ) {
        content()
    }
}

@Composable
private fun ProviderRow(
    provider: TmdbProviderInfo,
    selected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = TmdbImage.poster(provider.logoPath, width = 92),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(28.dp)
                .clip(MaterialTheme.shapes.small)
        )
        Text(
            provider.providerName,
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        )
        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
