package com.streamcueapp.streamcue.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.streamcueapp.streamcue.StreamCueApp
import com.streamcueapp.streamcue.data.Refresh
import com.streamcueapp.streamcue.model.TrackedMovie
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MovieDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as StreamCueApp).database.trackedMovieDao()

    private val _movie = MutableStateFlow<TrackedMovie?>(null)
    val movie: StateFlow<TrackedMovie?> = _movie

    private var loadedId: Int? = null

    fun load(tmdbId: Int) {
        if (loadedId == tmdbId) return
        loadedId = tmdbId
        viewModelScope.launch {
            dao.observe(tmdbId).collect { current ->
                _movie.value = current
                if (current != null && current.isStale) {
                    runCatching { Refresh.movie(dao, current) }
                }
            }
        }
    }

    fun setWatched(watched: Boolean) {
        val current = _movie.value ?: return
        viewModelScope.launch { dao.update(current.copy(watched = watched)) }
    }

    fun delete() {
        val current = _movie.value ?: return
        viewModelScope.launch { dao.delete(current) }
    }
}
