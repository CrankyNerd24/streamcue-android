package com.streamcueapp.streamcue.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.streamcueapp.streamcue.StreamCueApp
import com.streamcueapp.streamcue.model.TrackedMovie
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MoviesViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as StreamCueApp
    private val dao = app.database.trackedMovieDao()
    private val collapseStore = app.sectionCollapseStore

    val movies: StateFlow<List<TrackedMovie>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val collapsedSections: StateFlow<Set<String>> = collapseStore.collapsed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val watchedExpanded: StateFlow<Boolean> = collapseStore.watchedExpanded
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun toggleSection(title: String) {
        viewModelScope.launch {
            collapseStore.setCollapsed(title, title !in collapsedSections.value)
        }
    }

    fun toggleWatchedExpanded() {
        viewModelScope.launch {
            collapseStore.setWatchedExpanded(!watchedExpanded.value)
        }
    }
}
