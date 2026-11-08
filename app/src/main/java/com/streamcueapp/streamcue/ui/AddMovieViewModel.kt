package com.streamcueapp.streamcue.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.streamcueapp.streamcue.StreamCueApp
import com.streamcueapp.streamcue.model.TrackedMovie
import com.streamcueapp.streamcue.network.TmdbClient
import com.streamcueapp.streamcue.network.TmdbMovieSearchResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Movie-search analog of AddShowViewModel — see it for the shared reasoning. */
class AddMovieViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as StreamCueApp).database.trackedMovieDao()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _results = MutableStateFlow<List<TmdbMovieSearchResult>>(emptyList())
    val results: StateFlow<List<TmdbMovieSearchResult>> = _results

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    val trackedIds: StateFlow<Set<Int>> = dao.observeAll()
        .map { movies -> movies.map { it.tmdbId }.toSet() }
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
            _results.value = TmdbClient.api.searchMovies(query).results
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _results.value = emptyList()
            _error.value = "Couldn't reach TMDB. Check your connection."
        } finally {
            _isSearching.value = false
        }
    }

    fun addMovie(result: TmdbMovieSearchResult) {
        viewModelScope.launch {
            dao.insert(
                TrackedMovie(
                    tmdbId = result.id,
                    title = result.title,
                    posterPath = result.posterPath,
                    overview = result.overview,
                    genreIds = result.genreIds,
                    releaseDateEpochDay = parseEpochDay(result.releaseDate),
                )
            )
        }
    }

    // TMDB publishes a date with no time, same as its air dates — see
    // TrackedShow.kt's dayOffset comment for the iOS-side version of this.
    private fun parseEpochDay(date: String?): Long? =
        date?.let {
            runCatching { LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE).toEpochDay() }
                .getOrNull()
        }
}
