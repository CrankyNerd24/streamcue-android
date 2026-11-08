package com.streamcueapp.streamcue.network

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApi {
    @GET("search/tv")
    suspend fun searchShows(@Query("query") query: String): TmdbSearchResponse

    @GET("tv/{id}")
    suspend fun showDetail(@Path("id") id: Int): TmdbShowDetail

    @GET("search/movie")
    suspend fun searchMovies(@Query("query") query: String): TmdbMovieSearchResponse

    // Region is hardcoded for now — see AppSettings.region on iOS for the
    // setting this should eventually read from.
    @GET("watch/providers/tv")
    suspend fun watchProviders(
        @Query("watch_region") region: String = "US"
    ): TmdbProviderListResponse
}
