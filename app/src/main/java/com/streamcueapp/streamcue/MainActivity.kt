package com.streamcueapp.streamcue

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.streamcueapp.streamcue.ui.HomeScreen
import com.streamcueapp.streamcue.ui.theme.StreamCueTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StreamCueTheme {
                StreamCueRoot()
            }
        }
    }
}

@Composable
private fun StreamCueRoot() {
    Surface(modifier = Modifier.fillMaxSize()) {
        // A real NavHost (Shows / Movies / Services / Discover, matching
        // RootView's TabView on iOS) replaces this once more than one
        // screen exists — see CLAUDE.md's roadmap.
        HomeScreen()
    }
}
