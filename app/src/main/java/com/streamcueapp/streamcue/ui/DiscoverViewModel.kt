package com.streamcueapp.streamcue.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.streamcueapp.streamcue.StreamCueApp
import com.streamcueapp.streamcue.model.TrackedMovie
import com.streamcueapp.streamcue.model.TrackedShow
import com.streamcueapp.streamcue.network.TmdbClient
import com.streamcueapp.streamcue.network.TmdbMovieSearchResult
import com.streamcueapp.streamcue.network.TmdbShowSearchResult
import kotlinx.coroutines.CancellationException
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

enum class DiscoverKind { TV, MOVIES }

enum class DiscoverFeed { FOR_YOU, TRENDING, POPULAR }

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

    private val _feed = MutableStateFlow(DiscoverFeed.FOR_YOU)
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
                    DiscoverKind.TV -> when (_feed.value) {
                        DiscoverFeed.FOR_YOU -> recommendedShows()
                        DiscoverFeed.TRENDING -> TmdbClient.api.trendingShows().results.toDiscoverItems()
                        DiscoverFeed.POPULAR -> TmdbClient.api.popularShows().results.toDiscoverItems()
                    }
                    DiscoverKind.MOVIES -> when (_feed.value) {
                        DiscoverFeed.FOR_YOU -> recommendedMovies()
                        DiscoverFeed.TRENDING -> TmdbClient.api.trendingMovies().results.toDiscoverItems()
                        DiscoverFeed.POPULAR -> TmdbClient.api.popularMovies().results.toDiscoverItems()
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

    private fun List<TmdbShowSearchResult>.toDiscoverItems() = map {
        DiscoverItem(it.id, it.name, it.posterPath, it.overview, it.genreIds, DiscoverKind.TV)
    }

    private fun List<TmdbMovieSearchResult>.toDiscoverItems() = map {
        DiscoverItem(it.id, it.title, it.posterPath, it.overview, it.genreIds, DiscoverKind.MOVIES)
    }

    /**
     * Up to 8 tracked shows as seeds, TMDB's own /recommendations for each
     * fetched concurrently, tallied by how many seeds recommend a title —
     * ported from iOS's DiscoverView.recommendedShows(). Never throws: a
     * seed's recommendations call failing just means it contributes
     * nothing to the tally, same as iOS's `try?`.
     */
    private suspend fun recommendedShows(): List<DiscoverItem> {
        val tracked = showDao.observeAll().first()
        val seeds = tracked.take(8).map { it.tmdbId }
        if (seeds.isEmpty()) return emptyList()
        val alreadyTracked = tracked.map { it.tmdbId }.toSet()

        val tally = mutableMapOf<Int, Pair<TmdbShowSearchResult, Int>>()
        coroutineScope {
            val batches = seeds.map { id ->
                async {
                    runCatching { TmdbClient.api.showRecommendations(id).results }.getOrDefault(emptyList())
                }
            }.awaitAll()
            for (batch in batches) {
                for (item in batch) {
                    if (item.id in alreadyTracked) continue
                    val existing = tally[item.id]
                    tally[item.id] = if (existing != null) {
                        existing.first to existing.second + 1
                    } else {
                        item to 1
                    }
                }
            }
        }

        return tally.values
            .sortedWith(
                compareByDescending<Pair<TmdbShowSearchResult, Int>> { it.second }
                    .thenByDescending { it.first.voteAverage ?: 0.0 }
            )
            .take(40)
            .map { (result, _) ->
                DiscoverItem(result.id, result.name, result.posterPath, result.overview, result.genreIds, DiscoverKind.TV)
            }
    }

    private suspend fun recommendedMovies(): List<DiscoverItem> {
        val tracked = movieDao.observeAll().first()
        val seeds = tracked.take(8).map { it.tmdbId }
        if (seeds.isEmpty()) return emptyList()
        val alreadyTracked = tracked.map { it.tmdbId }.toSet()

        val tally = mutableMapOf<Int, Pair<TmdbMovieSearchResult, Int>>()
        coroutineScope {
            val batches = seeds.map { id ->
                async {
                    runCatching { TmdbClient.api.movieRecommendations(id).results }.getOrDefault(emptyList())
                }
            }.awaitAll()
            for (batch in batches) {
                for (item in batch) {
                    if (item.id in alreadyTracked) continue
                    val existing = tally[item.id]
                    tally[item.id] = if (existing != null) {
                        existing.first to existing.second + 1
                    } else {
                        item to 1
                    }
                }
            }
        }

        return tally.values
            .sortedWith(
                compareByDescending<Pair<TmdbMovieSearchResult, Int>> { it.second }
                    .thenByDescending { it.first.voteAverage ?: 0.0 }
            )
            .take(40)
            .map { (result, _) ->
                DiscoverItem(result.id, result.title, result.posterPath, result.overview, result.genreIds, DiscoverKind.MOVIES)
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
