package com.streamcueapp.streamcue.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Mirrors the iOS app's TrackedShow (SwiftData) model field for field, so
 * the two platforms stay easy to compare. `id` is TMDB's own show id —
 * there is no separate local id, same as iOS's `tmdbID`-keyed dedup in
 * Library.swift.
 */
@Entity(tableName = "tracked_shows")
data class TrackedShow(
    @PrimaryKey val tmdbId: Int,
    val name: String,
    val posterPath: String? = null,
    val status: String = "",
    val watched: Boolean = false,
    val nextEpisodeLabel: String? = null,
    val nextAirDateEpochDay: Long? = null,
    val lastEpisodeLabel: String? = null,
    val subscriptionOn: List<String> = emptyList(),
    val freeOn: List<String> = emptyList(),
    val rentOrBuyOn: List<String> = emptyList(),
    val watchLink: String? = null,
    val watchOnName: String? = null,
    val watchOnLink: String? = null,
    val addedAtEpochMillis: Long = System.currentTimeMillis(),
    val lastRefreshedEpochMillis: Long? = null,
    val genreIds: List<Int> = emptyList(),
    val overview: String = "",
    val nextEpisodeOverview: String? = null,
    val lastAiredSeason: Int? = null,
    // Days to shift effective air dates by — a show that streams in the US
    // well after it airs elsewhere. See StreamCue iOS's TrackedShow.swift
    // for the "never read nextAirDate directly" rule this mirrors.
    val dayOffset: Int = 0,
    val imdbId: String? = null,
    val tmdbScore: Double? = null,
    val tmdbVotes: Int? = null,
    val imdbScore: String? = null,
    val rottenTomatoesScore: String? = null,
    val metacriticScore: String? = null,
) {
    /** The air date as it applies to you — the one thing in this file the
     * class doc's "never read nextAirDate directly" rule exists to protect.
     * Everything that shows a date reads this, not nextAirDateEpochDay. */
    val effectiveAirDateEpochDay: Long?
        get() = nextAirDateEpochDay?.plus(dayOffset.toLong())

    /** Human-readable line for the list row and detail screen. */
    val scheduleSummary: String
        get() {
            effectiveAirDateEpochDay?.let { day ->
                val formatted = java.time.LocalDate.ofEpochDay(day)
                    .format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d"))
                return listOfNotNull(nextEpisodeLabel, formatted).joinToString(" · ")
            }
            return when (status) {
                "Ended" -> "Ended"
                "Canceled", "Cancelled" -> "Canceled"
                "Returning Series" -> "Returning — no date announced"
                else -> status.ifEmpty { "Not loaded yet" }
            }
        }

    /** Best single score to show in a compact space. */
    val headlineRating: String?
        get() = when {
            !imdbScore.isNullOrEmpty() -> "IMDb $imdbScore"
            (tmdbScore ?: 0.0) > 0.0 -> "TMDB " + "%.1f".format(tmdbScore)
            else -> null
        }

    val hasAnyRating: Boolean
        get() = !imdbScore.isNullOrEmpty() || !rottenTomatoesScore.isNullOrEmpty() ||
            !metacriticScore.isNullOrEmpty() || (tmdbScore ?: 0.0) > 0.0

    val isStale: Boolean
        get() = lastRefreshedEpochMillis?.let {
            System.currentTimeMillis() - it > staleAfterMillis
        } ?: true

    /** "+233 days", "−1 day", "None" — same formatting as iOS's offsetLabel. */
    val dayOffsetLabel: String
        get() = when {
            dayOffset == 0 -> "None"
            dayOffset == 1 -> "+1 day"
            dayOffset == -1 -> "−1 day"
            dayOffset > 0 -> "+$dayOffset days"
            else -> "−${-dayOffset} days"
        }

    companion object {
        /** How long data stays good enough to skip on a background refresh. */
        const val staleAfterMillis = 6L * 60 * 60 * 1000

        /** Covers a same-week simulcast delay through a year-later streaming
         * re-release — same range as iOS's dayOffsetRange. */
        const val dayOffsetMin = -7
        const val dayOffsetMax = 365
    }
}
