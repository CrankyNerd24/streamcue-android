package com.streamcueapp.streamcue.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.streamcueapp.streamcue.StreamCueApp
import com.streamcueapp.streamcue.model.TrackedMovie
import com.streamcueapp.streamcue.model.TrackedShow
import com.streamcueapp.streamcue.network.TmdbClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DiscoverKind { TV, MOVIES }

// "For you" from iOS's DiscoverFeed is assembled from the user's own
// tracked-title genres rather than a single endpoint — not ported yet, see
// CLAUDE.md. Trending/Popular are both one TMDB call each.
enum class DiscoverFeed { TRENDING, POPULAR }

data class DiscoverItem(
    val id: Int,
    val title: String,
    val posterPath: String?,
    val overview: String,
    val genreIds: List<Int>,
    val kind: DiscoverKind,
)

class DiscoverViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as StreamCueApp
    private val showDao = app.database.trackedShowDao()
    private val movieDao = app.database.trackedMovieDao()

    private val _kind = MutableStateFlow(DiscoverKind.TV)
    val kind: StateFlow<DiscoverKind> = _kind

    private val _feed = MutableStateFlow(DiscoverFeed.TRENDING)
    val feed: StateFlow<DiscoverFeed> = _feed

    private val _items = MutableStateFlow<List<DiscoverItem>>(emptyList())
    val items: StateFlow<List<DiscoverItem>> = _items

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    val trackedShowIds: StateFlow<Set<Int>> = showDao.observeAll()
        .map { shows -> shows.map { it.tmdbId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val trackedMovieIds: StateFlow<Set<Int>> = movieDao.observeAll()
        .map { movies -> movies.map { it.tmdbId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    init {
        load()
    }

    fun setKind(newKind: DiscoverKind) {
        if (newKind == _kind.value) return
        _kind.value = newKind
        load()
    }

    fun setFeed(newFeed: DiscoverFeed) {
        if (newFeed == _feed.value) return
        _feed.value = newFeed
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _items.value = when (_kind.value) {
                    DiscoverKind.TV -> {
                        val response = when (_feed.value) {
                            DiscoverFeed.TRENDING -> TmdbClient.api.trendingShows()
                            DiscoverFeed.POPULAR -> TmdbClient.api.popularShows()
                        }
                        response.results.map {
                            DiscoverItem(it.id, it.name, it.posterPath, it.overview, it.genreIds, DiscoverKind.TV)
                        }
                    }
                    DiscoverKind.MOVIES -> {
                        val response = when (_feed.value) {
                            DiscoverFeed.TRENDING -> TmdbClient.api.trendingMovies()
                            DiscoverFeed.POPULAR -> TmdbClient.api.popularMovies()
                        }
                        response.results.map {
                            DiscoverItem(it.id, it.title, it.posterPath, it.overview, it.genreIds, DiscoverKind.MOVIES)
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _items.value = emptyList()
                _error.value = "Couldn't reach TMDB. Check your connection."
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Tapping a poster never adds anything, same rule as iOS — this is only
    // called from the grid's explicit add button.
    fun add(item: DiscoverItem) {
        viewModelScope.launch {
            when (item.kind) {
                DiscoverKind.TV -> showDao.insert(
                    TrackedShow(
                        tmdbId = item.id,
                        name = item.title,
                        posterPath = item.posterPath,
                        overview = item.overview,
                        genreIds = item.genreIds,
                    )
                )
                DiscoverKind.MOVIES -> movieDao.insert(
                    TrackedMovie(
                        tmdbId = item.id,
                        title = item.title,
                        posterPath = item.posterPath,
                        overview = item.overview,
                        genreIds = item.genreIds,
                    )
                )
            }
        }
    }
}
