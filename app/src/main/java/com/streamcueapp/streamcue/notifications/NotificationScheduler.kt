package com.streamcueapp.streamcue.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.streamcueapp.streamcue.model.TrackedShow
import com.streamcueapp.streamcue.work.EpisodeAlertWorker
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Schedules one `EpisodeAlertWorker` run per show with a known air date,
 * fired that day at `alertHour`. Rebuilt from scratch on every call
 * (cancel everything, reschedule everything) rather than diffed — same
 * strategy as iOS's `Notifications.reschedule`, and the show list is
 * small enough that it's cheap. Called reactively from `StreamCueApp`
 * whenever the tracked-shows table changes.
 *
 * Uses WorkManager rather than `AlarmManager`'s exact alarms: Android
 * reserves the `SCHEDULE_EXACT_ALARM` permission for apps whose core
 * purpose is alarms/calendaring, and Play's policy is strict about
 * granting it elsewhere. A show airing "today" doesn't need to-the-minute
 * precision, so an inexact, battery-friendly one-time work request is the
 * better fit — delivery can lag behind `alertHour` under Doze, unlike
 * iOS's exact `UNCalendarNotificationTrigger`.
 */
object NotificationScheduler {
    private const val workTag = "episode_alert"

    // No per-show override or settings screen yet (see CLAUDE.md) — fixed
    // at iOS's own default notification hour.
    private const val alertHour = 18

    fun reschedule(context: Context, shows: List<TrackedShow>) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag(workTag)

        val zone = ZoneId.systemDefault()
        val now = Instant.now()
        for (show in shows) {
            val epochDay = show.effectiveAirDateEpochDay ?: continue
            val fireInstant = LocalDate.ofEpochDay(epochDay).atTime(alertHour, 0).atZone(zone).toInstant()
            if (!fireInstant.isAfter(now)) continue

            val data = Data.Builder()
                .putInt(EpisodeAlertWorker.keyTmdbId, show.tmdbId)
                .putLong(EpisodeAlertWorker.keyEpochDay, epochDay)
                .build()
            val request = OneTimeWorkRequestBuilder<EpisodeAlertWorker>()
                .setInitialDelay(fireInstant.toEpochMilli() - now.toEpochMilli(), TimeUnit.MILLISECONDS)
                .addTag(workTag)
                .setInputData(data)
                .build()
            workManager.enqueueUniqueWork(
                "episode_alert_${show.tmdbId}",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
