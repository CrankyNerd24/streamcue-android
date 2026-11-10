package com.streamcueapp.streamcue.data

import com.streamcueapp.streamcue.model.PendingEpisode
import com.streamcueapp.streamcue.model.TrackedShow
import com.streamcueapp.streamcue.network.TmdbClient
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Pulls a show's current season and records anything that has aired
 * recently, ported from the iOS app's EpisodeSync. TMDB only reports the
 * single most recent episode on the details endpoint, so catching up after
 * a gap needs the season list. Called right after a show is refreshed
 * (`Refresh.show`) — see ShowDetailViewModel, HomeViewModel.refreshAll, and
 * NightlyRefreshWorker.
 */
object EpisodeSync {
    // How far back to look. Stops a newly added show from dumping a whole
    // back catalogue into Ready to watch.
    private const val windowDays = 30L

    // Records older than this are deleted outright, actioned or not. Safe
    // because it's longer than windowDays — nothing this old can be re-added.
    const val expiryDays = 60L

    suspend fun sync(episodeDao: PendingEpisodeDao, showDao: TrackedShowDao, show: TrackedShow) {
        val seasonNumber = show.lastAiredSeason ?: return
        val season = runCatching {
            TmdbClient.api.seasonDetail(show.tmdbId, seasonNumber)
        }.getOrNull() ?: return

        val today = LocalDate.now().toEpochDay()
        val cutoff = today - windowDays
        var insertedNew = false

        for (episode in season.episodes) {
            val number = episode.episodeNumber ?: continue
            val broadcastDay = episode.airDate?.let {
                runCatching { LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE).toEpochDay() }.getOrNull()
            } ?: continue

            // Bake the show's offset in at sync time. Changing the offset
            // later won't retouch episodes already recorded — they age out
            // within expiryDays, which isn't worth a lookup from every
            // episode back to its show.
            val airDay = broadcastDay + show.dayOffset
            if (airDay > today || airDay < cutoff) continue

            val key = PendingEpisode.key(show.tmdbId, seasonNumber, number)
            if (episodeDao.find(key) != null) continue

            episodeDao.insert(
                PendingEpisode(
                    key = key,
                    showId = show.tmdbId,
                    showName = show.name,
                    posterPath = show.posterPath,
                    seasonNumber = seasonNumber,
                    episodeNumber = number,
                    title = episode.name,
                    airDateEpochDay = airDay,
                )
            )
            insertedNew = true
        }

        // A newly aired episode un-catches-up a show — "watched" means
        // caught up as of now, not caught up forever.
        if (insertedNew && show.watched) {
            showDao.update(show.copy(watched = false))
        }
    }

    /** Drops anything that aired more than `expiryDays` ago. */
    suspend fun prune(episodeDao: PendingEpisodeDao) {
        episodeDao.deleteOlderThan(LocalDate.now().toEpochDay() - expiryDays)
    }
}
