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
}
