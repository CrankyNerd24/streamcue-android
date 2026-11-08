package com.streamcueapp.streamcue.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Mirrors the iOS app's TrackedMovie (SwiftData) model field for field —
 * see TrackedShow.kt for the same approach. `id` is TMDB's own movie id.
 */
@Entity(tableName = "tracked_movies")
data class TrackedMovie(
    @PrimaryKey val tmdbId: Int,
    val title: String,
    val posterPath: String? = null,
    val releaseDateEpochDay: Long? = null,
    val status: String = "",
    val runtime: Int? = null,
    val watched: Boolean = false,
    val freeOn: List<String> = emptyList(),
    val subscriptionOn: List<String> = emptyList(),
    val rentOrBuyOn: List<String> = emptyList(),
    val watchLink: String? = null,
    val imdbId: String? = null,
    val tmdbScore: Double? = null,
    val imdbScore: String? = null,
    val rottenTomatoesScore: String? = null,
    val metacriticScore: String? = null,
    val addedAtEpochMillis: Long = System.currentTimeMillis(),
    val lastRefreshedEpochMillis: Long? = null,
    val genreIds: List<Int> = emptyList(),
    val overview: String = "",
) {
    /** "2024 · 1h 47m", the Android analog of iOS's `subtitle`. */
    val subtitle: String
        get() {
            val parts = mutableListOf<String>()
            year?.let { parts.add(it) }
            runtime?.takeIf { it > 0 }?.let {
                parts.add(if (it >= 60) "${it / 60}h ${it % 60}m" else "${it}m")
            }
            when {
                freeOn.isNotEmpty() -> parts.add(freeOn.joinToString(", "))
                subscriptionOn.isNotEmpty() -> parts.add(subscriptionOn.joinToString(", "))
            }
            return parts.joinToString(" · ")
        }

    private val year: String?
        get() = releaseDateEpochDay?.let {
            java.time.LocalDate.ofEpochDay(it).year.toString()
        }

    /** Best single score to show in a compact space. */
    val headlineRating: String?
        get() = when {
            !imdbScore.isNullOrEmpty() -> "IMDb $imdbScore"
            (tmdbScore ?: 0.0) > 0.0 -> "TMDB " + "%.1f".format(tmdbScore)
            else -> null
        }

    val isStale: Boolean
        get() = lastRefreshedEpochMillis?.let {
            System.currentTimeMillis() - it > staleAfterMillis
        } ?: true

    companion object {
        /** How long data stays good enough to skip on a background refresh. */
        const val staleAfterMillis = 6L * 60 * 60 * 1000
    }
}
