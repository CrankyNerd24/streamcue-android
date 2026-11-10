package com.streamcueapp.streamcue

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.streamcueapp.streamcue.notifications.hasNotificationPermission
import com.streamcueapp.streamcue.ui.RootScaffold
import com.streamcueapp.streamcue.ui.theme.StreamCueTheme

class MainActivity : ComponentActivity() {
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op either way — Ready to watch's badge and episode alerts both silently skip without it. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission(this)) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            StreamCueTheme {
                RootScaffold()
            }
        }
    }
}
