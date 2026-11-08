package com.streamcueapp.streamcue

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.streamcueapp.streamcue.ui.RootScaffold
import com.streamcueapp.streamcue.ui.theme.StreamCueTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StreamCueTheme {
                RootScaffold()
            }
        }
    }
}
