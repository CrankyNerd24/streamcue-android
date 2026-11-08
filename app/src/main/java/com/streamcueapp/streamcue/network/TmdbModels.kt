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
    @SerialName("vote_average") val voteAverage: Double? = null,
) {
    val year: String?
        get() = firstAirDate?.takeIf { it.length >= 4 }?.substring(0, 4)
}

@Serializable
data class TmdbSearchResponse(
    @SerialName("results") val results: List<TmdbShowSearchResult> = emptyList(),
)

@Serializable
data class TmdbGenre(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String = "",
)

@Serializable
data class TmdbExternalIds(
    @SerialName("imdb_id") val imdbId: String? = null,
)

@Serializable
data class TmdbEpisode(
    @SerialName("id") val id: Int? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("overview") val overview: String? = null,
    @SerialName("air_date") val airDate: String? = null,
    @SerialName("season_number") val seasonNumber: Int? = null,
    @SerialName("episode_number") val episodeNumber: Int? = null,
) {
    /** "S03E07 · Episode title" — same format as iOS's TVEpisode.label. */
    val label: String?
        get() {
            if (seasonNumber == null || episodeNumber == null) return name
            val number = "S%02dE%02d".format(seasonNumber, episodeNumber)
            return if (name.isNullOrEmpty()) number else "$number · $name"
        }
}

// The detail endpoint returns a `genres` array ({id, name}), not the
// `genre_ids` the search/discover endpoints use — a genuinely different
// shape, not just a naming choice.
@Serializable
data class TmdbShowDetail(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("status") val status: String = "",
    @SerialName("overview") val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("genres") val genres: List<TmdbGenre> = emptyList(),
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("vote_count") val voteCount: Int? = null,
    @SerialName("next_episode_to_air") val nextEpisodeToAir: TmdbEpisode? = null,
    @SerialName("last_episode_to_air") val lastEpisodeToAir: TmdbEpisode? = null,
    @SerialName("external_ids") val externalIds: TmdbExternalIds? = null,
)

@Serializable
data class TmdbMovieDetail(
    @SerialName("id") val id: Int,
    @SerialName("title") val title: String,
    @SerialName("overview") val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("status") val status: String = "",
    @SerialName("runtime") val runtime: Int? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("genres") val genres: List<TmdbGenre> = emptyList(),
    @SerialName("external_ids") val externalIds: TmdbExternalIds? = null,
)

@Serializable
data class TmdbMovieSearchResult(
    @SerialName("id") val id: Int,
    @SerialName("title") val title: String,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("overview") val overview: String = "",
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
) {
    val year: String?
        get() = releaseDate?.takeIf { it.length >= 4 }?.substring(0, 4)
}

@Serializable
data class TmdbMovieSearchResponse(
    @SerialName("results") val results: List<TmdbMovieSearchResult> = emptyList(),
)

@Serializable
data class TmdbSeasonDetail(
    @SerialName("episodes") val episodes: List<TmdbEpisode> = emptyList(),
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

@Serializable
data class TmdbWatchProvider(
    @SerialName("provider_id") val providerId: Int,
    @SerialName("provider_name") val providerName: String,
)

@Serializable
data class TmdbRegionProviders(
    @SerialName("link") val link: String? = null,
    @SerialName("flatrate") val flatrate: List<TmdbWatchProvider>? = null,
    @SerialName("free") val free: List<TmdbWatchProvider>? = null,
    @SerialName("ads") val ads: List<TmdbWatchProvider>? = null,
    @SerialName("rent") val rent: List<TmdbWatchProvider>? = null,
    @SerialName("buy") val buy: List<TmdbWatchProvider>? = null,
)

@Serializable
data class TmdbWatchProviderResponse(
    @SerialName("results") val results: Map<String, TmdbRegionProviders> = emptyMap(),
)
