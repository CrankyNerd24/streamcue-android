package com.streamcueapp.streamcue.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Stands in for iOS's WatchlistView (Movies tab) — grouping, search, detail,
 * and suggestions land here once a TrackedMovie entity exists alongside
 * TrackedShow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoviesScreen() {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Movies") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Movie tracking is coming soon.")
        }
    }
}
