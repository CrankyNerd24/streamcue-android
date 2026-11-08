package com.streamcueapp.streamcue.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.streamcueapp.streamcue.StreamCueApp
import com.streamcueapp.streamcue.model.StreamingService
import com.streamcueapp.streamcue.network.TmdbClient
import com.streamcueapp.streamcue.network.TmdbProviderInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ServicePickerViewModel(application: Application) : AndroidViewModel(application) {
    private val store = (application as StreamCueApp).subscriptionsStore

    private val _all = MutableStateFlow<List<TmdbProviderInfo>>(emptyList())
    val all: StateFlow<List<TmdbProviderInfo>> = _all

    private val _chosen = MutableStateFlow<Set<Int>>(emptySet())
    val chosen: StateFlow<Set<Int>> = _chosen

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        viewModelScope.launch {
            val selected = store.services.first()
            _chosen.value = selected.map { it.id }.toSet()
            load(_chosen.value)
        }
    }

    private suspend fun load(subscribed: Set<Int>) {
        _isLoading.value = true
        _error.value = null
        try {
            // TMDB's own display-priority order reads as random in a long
            // list — your current services first, then everyone else, each
            // alphabetical. Same ordering as iOS's ServicePickerView.
            _all.value = TmdbClient.api.watchProviders()
                .results
                .sortedWith(
                    compareByDescending<TmdbProviderInfo> { subscribed.contains(it.providerId) }
                        .thenBy { it.providerName }
                )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _error.value = "Couldn't reach TMDB. Check your connection."
        } finally {
            _isLoading.value = false
        }
    }

    fun toggle(id: Int) {
        _chosen.value = if (id in _chosen.value) _chosen.value - id else _chosen.value + id
    }

    fun save(onDone: () -> Unit) {
        viewModelScope.launch {
            val services = all.value
                .filter { it.providerId in chosen.value }
                .map { StreamingService(it.providerId, it.providerName, it.logoPath) }
            store.save(services)
            onDone()
        }
    }
}
