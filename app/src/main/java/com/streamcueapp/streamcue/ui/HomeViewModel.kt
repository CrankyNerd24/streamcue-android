package com.streamcueapp.streamcue.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.streamcueapp.streamcue.StreamCueApp
import com.streamcueapp.streamcue.model.TrackedShow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as StreamCueApp).database.trackedShowDao()

    val shows: StateFlow<List<TrackedShow>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
