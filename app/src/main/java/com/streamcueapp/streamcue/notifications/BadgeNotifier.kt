package com.streamcueapp.streamcue.notifications

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.streamcueapp.streamcue.MainActivity
import com.streamcueapp.streamcue.R

/**
 * The Android analog of iOS's app-icon badge count
 * (`UNUserNotificationCenter.setBadgeCount`). Android has no API for an
 * arbitrary badge number detached from a real notification, so this posts
 * one low-importance, sound-free notification showing the Ready to watch
 * count, updated in place (`setOnlyAlertOnce`) as that count changes, and
 * cancelled outright once it reaches zero. Whether it actually shows as a
 * number on the launcher icon (vs. just a dot, or nothing) is up to the
 * launcher — `setNumber` is honoured by some (Samsung's OneUI), ignored by
 * others (stock Pixel), same way iOS's badge is the OS's call, not ours.
 */
object BadgeNotifier {
    private const val notificationId = 1

    @SuppressLint("MissingPermission") // guarded by hasNotificationPermission below
    fun update(context: Context, outstandingCount: Int) {
        val manager = NotificationManagerCompat.from(context)
        if (outstandingCount <= 0) {
            manager.cancel(notificationId)
            return
        }
        if (!hasNotificationPermission(context)) return

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, NotificationChannels.readyToWatch)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Ready to watch")
            .setContentText(
                if (outstandingCount == 1) "1 episode ready to watch" else "$outstandingCount episodes ready to watch"
            )
            .setNumber(outstandingCount)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .setContentIntent(openApp)
            .build()
        manager.notify(notificationId, notification)
    }
}
