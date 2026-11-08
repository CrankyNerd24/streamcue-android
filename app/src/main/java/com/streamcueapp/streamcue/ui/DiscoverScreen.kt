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
 * Stands in for iOS's DiscoverView — TV/film × for-you/trending/popular
 * lands here once TMDB's discover/trending endpoints are wired up.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen() {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Discover") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Trending and recommendations are coming soon.")
        }
    }
}
