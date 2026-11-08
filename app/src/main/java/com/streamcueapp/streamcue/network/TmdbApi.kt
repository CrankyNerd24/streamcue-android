package com.streamcueapp.streamcue.network

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApi {
    @GET("search/tv")
    suspend fun searchShows(@Query("query") query: String): TmdbSearchResponse

    @GET("tv/{id}")
    suspend fun showDetail(
        @Path("id") id: Int,
        @Query("append_to_response") append: String = "external_ids"
    ): TmdbShowDetail

    @GET("movie/{id}")
    suspend fun movieDetail(
        @Path("id") id: Int,
        @Query("append_to_response") append: String = "external_ids"
    ): TmdbMovieDetail

    @GET("tv/{id}/watch/providers")
    suspend fun showWatchProviders(@Path("id") id: Int): TmdbWatchProviderResponse

    @GET("movie/{id}/watch/providers")
    suspend fun movieWatchProviders(@Path("id") id: Int): TmdbWatchProviderResponse

    @GET("search/movie")
    suspend fun searchMovies(@Query("query") query: String): TmdbMovieSearchResponse

    // Region is hardcoded for now — see AppSettings.region on iOS for the
    // setting this should eventually read from.
    @GET("watch/providers/tv")
    suspend fun watchProviders(
        @Query("watch_region") region: String = "US"
    ): TmdbProviderListResponse

    @GET("trending/tv/week")
    suspend fun trendingShows(): TmdbSearchResponse

    @GET("tv/popular")
    suspend fun popularShows(): TmdbSearchResponse

    @GET("trending/movie/week")
    suspend fun trendingMovies(): TmdbMovieSearchResponse

    @GET("movie/popular")
    suspend fun popularMovies(): TmdbMovieSearchResponse

    @GET("tv/{id}/recommendations")
    suspend fun showRecommendations(@Path("id") id: Int): TmdbSearchResponse

    @GET("movie/{id}/recommendations")
    suspend fun movieRecommendations(@Path("id") id: Int): TmdbMovieSearchResponse

    @GET("tv/{id}/season/{season}")
    suspend fun seasonDetail(
        @Path("id") id: Int,
        @Path("season") season: Int
    ): TmdbSeasonDetail

    // Popular titles on one service, narrowed to genres already tracked —
    // ported from iOS's TMDBClient.discoverShows/discoverMovies. `genres`
    // omits the with_genres param entirely when null (Retrofit drops null
    // @Query params), matching iOS's "if !genres.isEmpty" check.
    @GET("discover/tv")
    suspend fun discoverShows(
        @Query("with_watch_providers") providerId: Int,
        @Query("with_genres") genres: String? = null,
        @Query("watch_region") region: String = "US",
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("vote_count.gte") minVotes: Int = 50
    ): TmdbSearchResponse

    @GET("discover/movie")
    suspend fun discoverMovies(
        @Query("with_watch_providers") providerId: Int,
        @Query("with_genres") genres: String? = null,
        @Query("watch_region") region: String = "US",
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("vote_count.gte") minVotes: Int = 100
    ): TmdbMovieSearchResponse
}
