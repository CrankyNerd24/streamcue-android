package com.streamcueapp.streamcue.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * POST_NOTIFICATIONS is only a runtime permission from API 33 — this app's
 * minSdk is 31, where posting is allowed outright. Without it, every
 * notifier here silently no-ops, same as iOS's "without permission, iOS
 * silently ignores the badge."
 */
fun hasNotificationPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
}
