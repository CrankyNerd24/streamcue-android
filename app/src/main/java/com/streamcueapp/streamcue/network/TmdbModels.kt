package com.streamcueapp.streamcue.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Explicit CodingKeys-style field names throughout, never
// @JsonNamesCompatible/snake-case auto-conversion — the iOS client hit a
// real bug doing that (it lowercases watch-provider country keys). Name
// every field by hand instead.

@Serializable
data class TmdbShowSearchResult(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("overview") val overview: String = "",
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
)

@Serializable
data class TmdbSearchResponse(
    @SerialName("results") val results: List<TmdbShowSearchResult> = emptyList(),
)

@Serializable
data class TmdbShowDetail(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("status") val status: String = "",
    @SerialName("overview") val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("vote_count") val voteCount: Int? = null,
)
