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
 * Stands in for iOS's SubscriptionsView (Services tab) — the streaming
 * service picker and per-service recommendations land here once the
 * provider list and selection state exist on Android.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen() {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Services") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Pick your services here soon.")
        }
    }
}
