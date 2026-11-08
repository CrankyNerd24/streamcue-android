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
    @SerialName("first_air_date") val firstAirDate: String? = null,
) {
    val year: String?
        get() = firstAirDate?.takeIf { it.length >= 4 }?.substring(0, 4)
}

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

@Serializable
data class TmdbMovieSearchResult(
    @SerialName("id") val id: Int,
    @SerialName("title") val title: String,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("overview") val overview: String = "",
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
    @SerialName("release_date") val releaseDate: String? = null,
) {
    val year: String?
        get() = releaseDate?.takeIf { it.length >= 4 }?.substring(0, 4)
}

@Serializable
data class TmdbMovieSearchResponse(
    @SerialName("results") val results: List<TmdbMovieSearchResult> = emptyList(),
)

@Serializable
data class TmdbProviderInfo(
    @SerialName("provider_id") val providerId: Int,
    @SerialName("provider_name") val providerName: String,
    @SerialName("logo_path") val logoPath: String? = null,
    @SerialName("display_priority") val displayPriority: Int = 0,
)

@Serializable
data class TmdbProviderListResponse(
    @SerialName("results") val results: List<TmdbProviderInfo> = emptyList(),
)
