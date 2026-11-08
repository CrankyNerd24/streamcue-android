package com.streamcueapp.streamcue.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.streamcueapp.streamcue.StreamCueApp
import com.streamcueapp.streamcue.model.StreamingService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ServicesViewModel(application: Application) : AndroidViewModel(application) {
    private val store = (application as StreamCueApp).subscriptionsStore

    val services: StateFlow<List<StreamingService>> = store.services
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
