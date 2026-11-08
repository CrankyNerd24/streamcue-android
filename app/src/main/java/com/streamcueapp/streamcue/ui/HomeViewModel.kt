package com.streamcueapp.streamcue.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.streamcueapp.streamcue.StreamCueApp
import com.streamcueapp.streamcue.model.TrackedShow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as StreamCueApp
    private val dao = app.database.trackedShowDao()
    private val collapseStore = app.sectionCollapseStore

    val shows: StateFlow<List<TrackedShow>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val collapsedSections: StateFlow<Set<String>> = collapseStore.collapsed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun toggleSection(title: String) {
        viewModelScope.launch {
            collapseStore.setCollapsed(title, title !in collapsedSections.value)
        }
    }
}
