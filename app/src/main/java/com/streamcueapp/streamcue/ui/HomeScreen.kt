package com.streamcueapp.streamcue.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.streamcueapp.streamcue.model.TrackedShow

/**
 * Stands in for ContentView.swift's Shows tab. No sections, no Ready to
 * watch, no Watch Now yet — the empty/populated list split is the first
 * real vertical slice; everything else in CLAUDE.md's roadmap builds on
 * this screen and HomeViewModel's StateFlow.
 */
@Composable
fun HomeScreen(onAddShow: () -> Unit, viewModel: HomeViewModel = viewModel()) {
    val shows by viewModel.shows.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Shows") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddShow) {
                Icon(Icons.Default.Add, contentDescription = "Add a show")
            }
        }
    ) { padding ->
        if (shows.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No shows tracked yet.")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(shows, key = { it.tmdbId }) { show ->
                    ShowRow(show)
                }
            }
        }
    }
}

@Composable
private fun ShowRow(show: TrackedShow) {
    Text(
        text = show.name,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}
