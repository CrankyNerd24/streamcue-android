package com.streamcueapp.streamcue.network

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApi {
    @GET("search/tv")
    suspend fun searchShows(@Query("query") query: String): TmdbSearchResponse

    @GET("tv/{id}")
    suspend fun showDetail(@Path("id") id: Int): TmdbShowDetail
}
