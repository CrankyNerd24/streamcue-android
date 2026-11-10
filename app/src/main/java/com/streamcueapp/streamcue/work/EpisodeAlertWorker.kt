package com.streamcueapp.streamcue.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.streamcueapp.streamcue.StreamCueApp
import com.streamcueapp.streamcue.notifications.EpisodeAlertNotifier

/**
 * Fires one show's "airs today" alert, scheduled by `NotificationScheduler`.
 * Re-reads the show from Room rather than trusting the data it was
 * scheduled with — if a refresh moved its air date, or it was removed,
 * since this was queued, `expectedEpochDay` won't match (or the show
 * won't exist) and this quietly does nothing instead of firing a stale
 * alert.
 */
class EpisodeAlertWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val tmdbId = inputData.getInt(keyTmdbId, -1)
        val expectedEpochDay = inputData.getLong(keyEpochDay, -1)
        if (tmdbId == -1) return Result.success()

        val app = applicationContext as StreamCueApp
        val show = app.database.trackedShowDao().find(tmdbId) ?: return Result.success()
        if (show.effectiveAirDateEpochDay != expectedEpochDay) return Result.success()

        EpisodeAlertNotifier.post(applicationContext, show)
        return Result.success()
    }

    companion object {
        const val keyTmdbId = "tmdbId"
        const val keyEpochDay = "epochDay"
    }
}
