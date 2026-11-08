package com.streamcueapp.streamcue.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.streamcueapp.streamcue.StreamCueApp
import com.streamcueapp.streamcue.data.Refresh
import com.streamcueapp.streamcue.model.TrackedShow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ShowDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as StreamCueApp).database.trackedShowDao()

    private val _show = MutableStateFlow<TrackedShow?>(null)
    val show: StateFlow<TrackedShow?> = _show

    private var loadedId: Int? = null

    fun load(tmdbId: Int) {
        if (loadedId == tmdbId) return
        loadedId = tmdbId
        viewModelScope.launch {
            dao.observe(tmdbId).collect { current ->
                _show.value = current
                if (current != null && current.isStale) {
                    runCatching { Refresh.show(dao, current) }
                }
            }
        }
    }

    fun setWatched(watched: Boolean) {
        val current = _show.value ?: return
        viewModelScope.launch { dao.update(current.copy(watched = watched)) }
    }

    fun delete() {
        val current = _show.value ?: return
        viewModelScope.launch { dao.delete(current) }
    }
}
