package com.streamcueapp.streamcue.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

/**
 * The app's two notification channels — created once at launch, same spot
 * as `NightlyRefreshWorker.schedule`. Android has no direct equivalent to
 * iOS's silent app-icon badge (`UNUserNotificationCenter.setBadgeCount`):
 * a badge count here has to ride an actual posted notification, so Ready
 * to watch gets its own low-importance channel, separate from the louder
 * per-show air-date alerts.
 */
object NotificationChannels {
    const val readyToWatch = "ready_to_watch"
    const val episodeAlerts = "episode_alerts"

    fun create(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                readyToWatch,
                "Ready to watch",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "A running count of aired episodes you haven't watched yet."
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                episodeAlerts,
                "Episode alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "An alert on the day a tracked show airs."
            }
        )
    }
}
