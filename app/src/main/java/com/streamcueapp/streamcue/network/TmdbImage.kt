package com.streamcueapp.streamcue.network

/** Poster URLs — same `image.tmdb.org/t/p/wNNN` scheme as iOS's TMDB.swift. */
object TmdbImage {
    private const val base = "https://image.tmdb.org/t/p/"

    fun poster(path: String?, width: Int = 185): String? =
        path?.let { "${base}w$width$it" }
}
