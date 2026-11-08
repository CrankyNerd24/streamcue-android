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
import androidx.compose.material.icons.filled.Checklist
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.streamcueapp.streamcue.model.StreamingService
import com.streamcueapp.streamcue.network.TmdbImage

/**
 * Stands in for iOS's SubscriptionsView — the service picker is here. The
 * per-service "popular on your services" suggestion strips (genre
 * aggregation from tracked shows/movies, plus a discover query per
 * service) are still ahead; this tab only tracks which services are yours.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(onChooseServices: () -> Unit, viewModel: ServicesViewModel = viewModel()) {
    val services by viewModel.services.collectAsState()

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
                    ServiceRow(service)
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
