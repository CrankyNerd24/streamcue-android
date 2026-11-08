package com.streamcueapp.streamcue.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.streamcueapp.streamcue.StreamCueApp
import com.streamcueapp.streamcue.model.StreamingService
import com.streamcueapp.streamcue.network.TmdbClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ServicesViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as StreamCueApp
    private val store = app.subscriptionsStore
    private val showDao = app.database.trackedShowDao()
    private val movieDao = app.database.trackedMovieDao()

    val services: StateFlow<List<StreamingService>> = store.services
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val trackedShowIds: StateFlow<Set<Int>> = showDao.observeAll()
        .map { shows -> shows.map { it.tmdbId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val trackedMovieIds: StateFlow<Set<Int>> = movieDao.observeAll()
        .map { movies -> movies.map { it.tmdbId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val _showPicks = MutableStateFlow<Map<Int, List<DiscoverItem>>>(emptyMap())
    val showPicks: StateFlow<Map<Int, List<DiscoverItem>>> = _showPicks

    private val _moviePicks = MutableStateFlow<Map<Int, List<DiscoverItem>>>(emptyMap())
    val moviePicks: StateFlow<Map<Int, List<DiscoverItem>>> = _moviePicks

    private val _isLoadingPicks = MutableStateFlow(false)
    val isLoadingPicks: StateFlow<Boolean> = _isLoadingPicks

    init {
        viewModelScope.launch {
            store.services.collect { list -> loadPicks(list) }
        }
    }

    /**
     * Popular shows/movies per service, narrowed to the genres already
     * tracked — ported from iOS's SubscriptionsView.loadPicks via
     * TMDBClient.discoverShows/discoverMovies. Re-fetched whenever the
     * services list changes, not on every render.
     */
    private suspend fun loadPicks(services: List<StreamingService>) {
        if (services.isEmpty()) {
            _showPicks.value = emptyMap()
            _moviePicks.value = emptyMap()
            return
        }
        _isLoadingPicks.value = true
        try {
            val showGenres = topGenres(showDao.observeAll().first()) { it.genreIds }
            val movieGenres = topGenres(movieDao.observeAll().first()) { it.genreIds }
            coroutineScope {
                _showPicks.value = services.map { service ->
                    async {
                        service.id to runCatching {
                            TmdbClient.api.discoverShows(
                                providerId = service.id,
                                genres = showGenres.takeIf { it.isNotEmpty() }?.joinToString("|")
                            ).results.take(10).map {
                                DiscoverItem(
                                    it.id, it.name, it.posterPath, it.overview, it.genreIds, DiscoverKind.TV
                                )
                            }
                        }.getOrDefault(emptyList())
                    }
                }.awaitAll().toMap()

                _moviePicks.value = services.map { service ->
                    async {
                        service.id to runCatching {
                            TmdbClient.api.discoverMovies(
                                providerId = service.id,
                                genres = movieGenres.takeIf { it.isNotEmpty() }?.joinToString("|")
                            ).results.take(10).map {
                                DiscoverItem(
                                    it.id, it.title, it.posterPath, it.overview, it.genreIds, DiscoverKind.MOVIES
                                )
                            }
                        }.getOrDefault(emptyList())
                    }
                }.awaitAll().toMap()
            }
        } finally {
            _isLoadingPicks.value = false
        }
    }

    /** The three genres that show up most across a tracked list. */
    private fun <T> topGenres(tracked: List<T>, genreIds: (T) -> List<Int>): List<Int> {
        val counts = mutableMapOf<Int, Int>()
        for (item in tracked) for (genre in genreIds(item)) counts[genre] = (counts[genre] ?: 0) + 1
        return counts.entries.sortedByDescending { it.value }.take(3).map { it.key }
    }
}
