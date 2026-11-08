package com.streamcueapp.streamcue.data

import com.streamcueapp.streamcue.model.TrackedMovie
import com.streamcueapp.streamcue.model.TrackedShow
import com.streamcueapp.streamcue.network.TmdbClient
import com.streamcueapp.streamcue.network.availability
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * TMDB-only refresh, ported from TrackedShow.refresh()/TrackedMovie.refresh()
 * on iOS — shared by the detail screens (refresh on open, if stale) and the
 * nightly WorkManager job. OMDb ratings aren't ported yet; see CLAUDE.md.
 */
object Refresh {
    suspend fun show(dao: TrackedShowDao, show: TrackedShow) {
        val details = TmdbClient.api.showDetail(show.tmdbId)
        val avail = TmdbClient.api.showWatchProviders(show.tmdbId).availability()

        // TMDB's nextEpisodeToAir is real-world and offset-unaware: the
        // moment an episode actually broadcasts, TMDB moves "next" past it,
        // even though a day offset means it hasn't effectively reached this
        // user yet. Keep showing lastEpisodeToAir as "next" until its own
        // offset-adjusted date arrives — same reasoning as iOS's refresh().
        val lastBroadcastEpochDay = parseEpochDay(details.lastEpisodeToAir?.airDate)
        val lastEffectiveEpochDay = lastBroadcastEpochDay?.plus(show.dayOffset.toLong())
        val todayEpochDay = LocalDate.now().toEpochDay()
        val lastStillPending = lastEffectiveEpochDay != null && lastEffectiveEpochDay >= todayEpochDay

        val nextLabel: String?
        val nextOverview: String?
        val nextAirDateEpochDay: Long?
        if (lastStillPending) {
            nextLabel = details.lastEpisodeToAir?.label
            nextOverview = details.lastEpisodeToAir?.overview
            nextAirDateEpochDay = lastBroadcastEpochDay
        } else {
            nextLabel = details.nextEpisodeToAir?.label
            nextOverview = details.nextEpisodeToAir?.overview
            nextAirDateEpochDay = parseEpochDay(details.nextEpisodeToAir?.airDate)
        }

        // Not just lastEpisodeToAir: nextEpisodeToAir (scheduled, not
        // broadcast-dependent) already reflects a new season before
        // lastEpisodeToAir rolls over.
        val lastAiredSeason = listOfNotNull(
            details.lastEpisodeToAir?.seasonNumber,
            details.nextEpisodeToAir?.seasonNumber
        ).maxOrNull()

        dao.update(
            show.copy(
                name = details.name,
                posterPath = details.posterPath,
                status = details.status,
                tmdbScore = details.voteAverage,
                tmdbVotes = details.voteCount,
                genreIds = details.genres.map { it.id },
                imdbId = details.externalIds?.imdbId,
                overview = details.overview,
                nextEpisodeOverview = nextOverview,
                nextEpisodeLabel = nextLabel,
                nextAirDateEpochDay = nextAirDateEpochDay,
                lastEpisodeLabel = details.lastEpisodeToAir?.label,
                lastAiredSeason = lastAiredSeason,
                subscriptionOn = avail.subscription,
                freeOn = avail.free,
                rentOrBuyOn = avail.rentOrBuy,
                watchLink = avail.link,
                lastRefreshedEpochMillis = System.currentTimeMillis(),
            )
        )
    }

    suspend fun movie(dao: TrackedMovieDao, movie: TrackedMovie) {
        val details = TmdbClient.api.movieDetail(movie.tmdbId)
        val avail = TmdbClient.api.movieWatchProviders(movie.tmdbId).availability()

        dao.update(
            movie.copy(
                title = details.title,
                overview = details.overview,
                posterPath = details.posterPath,
                status = details.status,
                runtime = details.runtime,
                releaseDateEpochDay = parseEpochDay(details.releaseDate),
                tmdbScore = details.voteAverage,
                genreIds = details.genres.map { it.id },
                imdbId = details.externalIds?.imdbId,
                freeOn = avail.free,
                subscriptionOn = avail.subscription,
                rentOrBuyOn = avail.rentOrBuy,
                watchLink = avail.link,
                lastRefreshedEpochMillis = System.currentTimeMillis(),
            )
        )
    }

    // TMDB publishes a date with no time, same as its air dates.
    private fun parseEpochDay(date: String?): Long? =
        date?.let {
            runCatching { LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE).toEpochDay() }
                .getOrNull()
        }
}
