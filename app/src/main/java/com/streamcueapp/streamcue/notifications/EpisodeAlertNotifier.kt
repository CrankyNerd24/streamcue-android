package com.streamcueapp.streamcue.notifications

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.streamcueapp.streamcue.MainActivity
import com.streamcueapp.streamcue.R
import com.streamcueapp.streamcue.model.TrackedShow

/**
 * A visible, one-off alert that a tracked show airs today — the Android
 * analog of iOS's per-show `Notifications.reschedule` alert (minus its
 * "private mode" that collapses every show into one anonymous daily
 * alert; not ported, see CLAUDE.md). Scheduled by `NotificationScheduler`
 * and fired by `EpisodeAlertWorker`, one per show per air date.
 */
object EpisodeAlertNotifier {
    @SuppressLint("MissingPermission") // guarded by hasNotificationPermission below
    fun post(context: Context, show: TrackedShow) {
        if (!hasNotificationPermission(context)) return

        val openApp = PendingIntent.getActivity(
            context,
            show.tmdbId,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, NotificationChannels.episodeAlerts)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(show.name)
            .setContentText(body(show))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()
        NotificationManagerCompat.from(context).notify(alertNotificationId(show.tmdbId), notification)
    }

    private fun alertNotificationId(tmdbId: Int) = 100_000 + tmdbId

    /** "S03E07 · Free on Service" — same shape as iOS's body(for:). */
    private fun body(show: TrackedShow): String {
        val parts = mutableListOf(show.nextEpisodeLabel?.let { "$it airs today" } ?: "Airs today")
        show.freeOn.firstOrNull()?.let { parts.add("Free on $it") }
            ?: show.subscriptionOn.firstOrNull()?.let { parts.add(it) }
        return parts.joinToString(" · ")
    }
}
