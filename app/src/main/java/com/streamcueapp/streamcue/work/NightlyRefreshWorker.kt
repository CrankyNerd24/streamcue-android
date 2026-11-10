package com.streamcueapp.streamcue.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.streamcueapp.streamcue.StreamCueApp
import com.streamcueapp.streamcue.data.EpisodeSync
import com.streamcueapp.streamcue.data.Refresh
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * The Android analog of iOS's BGTaskScheduler nightly refresh — walks
 * every tracked show and movie and re-fetches it from TMDB. One failure
 * never aborts the batch; a single bad network blip for one title
 * shouldn't skip refreshing everything else.
 */
class NightlyRefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as StreamCueApp
        val showDao = app.database.trackedShowDao()
        val movieDao = app.database.trackedMovieDao()
        val episodeDao = app.database.pendingEpisodeDao()

        showDao.observeAll().first().forEach { show ->
            runCatching {
                Refresh.show(showDao, show)
                val refreshed = showDao.find(show.tmdbId) ?: return@runCatching
                EpisodeSync.sync(episodeDao, showDao, refreshed)
            }
        }
        EpisodeSync.prune(episodeDao)
        movieDao.observeAll().first().forEach { movie ->
            runCatching { Refresh.movie(movieDao, movie) }
        }

        return Result.success()
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "nightly_refresh"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<NightlyRefreshWorker>(24, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
