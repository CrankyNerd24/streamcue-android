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
)
