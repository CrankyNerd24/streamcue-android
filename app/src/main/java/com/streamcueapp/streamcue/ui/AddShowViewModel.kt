package com.streamcueapp.streamcue.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.streamcueapp.streamcue.StreamCueApp
import com.streamcueapp.streamcue.model.TrackedShow
import com.streamcueapp.streamcue.network.TmdbClient
import com.streamcueapp.streamcue.network.TmdbShowSearchResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backs the Add Show search screen — the Android analog of iOS's
 * LookupView, trimmed to TV search only for now. Adding a show goes
 * through TrackedShowDao.insert's dedup-on-tmdbId path, the same guarantee
 * iOS's Library.addShow gives.
 */
class AddShowViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as StreamCueApp).database.trackedShowDao()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _results = MutableStateFlow<List<TmdbShowSearchResult>>(emptyList())
    val results: StateFlow<List<TmdbShowSearchResult>> = _results

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    val trackedIds: StateFlow<Set<Int>> = dao.observeAll()
        .map { shows -> shows.map { it.tmdbId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private var searchJob: Job? = null

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        searchJob?.cancel()
        if (newQuery.isBlank()) {
            _results.value = emptyList()
            _error.value = null
            return
        }
        searchJob = viewModelScope.launch {
            delay(400)
            runSearch(newQuery)
        }
    }

    private suspend fun runSearch(query: String) {
        _isSearching.value = true
        _error.value = null
        try {
            _results.value = TmdbClient.api.searchShows(query).results
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _results.value = emptyList()
            _error.value = "Couldn't reach TMDB. Check your connection."
        } finally {
            _isSearching.value = false
        }
    }

    fun addShow(result: TmdbShowSearchResult) {
        viewModelScope.launch {
            dao.insert(
                TrackedShow(
                    tmdbId = result.id,
                    name = result.name,
                    posterPath = result.posterPath,
                    overview = result.overview,
                    genreIds = result.genreIds,
                )
            )
        }
    }
}
