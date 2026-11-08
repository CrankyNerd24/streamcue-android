package com.streamcueapp.streamcue

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.streamcueapp.streamcue.ui.AddShowScreen
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
    val navController = rememberNavController()
    Surface(modifier = Modifier.fillMaxSize()) {
        // Bottom tabs (Shows / Movies / Services / Discover, matching
        // RootView's TabView on iOS) replace this single-route NavHost
        // once more than one top-level screen exists — see CLAUDE.md's
        // roadmap.
        NavHost(navController = navController, startDestination = "home") {
            composable("home") {
                HomeScreen(onAddShow = { navController.navigate("addShow") })
            }
            composable("addShow") {
                AddShowScreen(onDone = { navController.popBackStack() })
            }
        }
    }
}
