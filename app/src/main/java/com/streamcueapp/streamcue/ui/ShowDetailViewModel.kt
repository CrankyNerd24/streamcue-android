package com.streamcueapp.streamcue.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.streamcueapp.streamcue.StreamCueApp
import com.streamcueapp.streamcue.data.Refresh
import com.streamcueapp.streamcue.model.TrackedShow
import com.streamcueapp.streamcue.network.TmdbClient
import com.streamcueapp.streamcue.network.TmdbEpisode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ShowDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as StreamCueApp).database.trackedShowDao()

    private val _show = MutableStateFlow<TrackedShow?>(null)
    val show: StateFlow<TrackedShow?> = _show

    private val _episodes = MutableStateFlow<List<TmdbEpisode>>(emptyList())
    val episodes: StateFlow<List<TmdbEpisode>> = _episodes

    private var loadedId: Int? = null
    private var loadedSeasonKey: String? = null

    fun load(tmdbId: Int) {
        if (loadedId == tmdbId) return
        loadedId = tmdbId
        viewModelScope.launch {
            dao.observe(tmdbId).collect { current ->
                _show.value = current
                if (current == null) return@collect
                if (current.isStale) {
                    runCatching { Refresh.show(dao, current) }
                }
                val season = current.lastAiredSeason
                val seasonKey = "${current.tmdbId}-$season"
                if (season != null && loadedSeasonKey != seasonKey) {
                    loadedSeasonKey = seasonKey
                    loadEpisodes(current.tmdbId, season)
                }
            }
        }
    }

    // Read-only, fetched straight from TMDB rather than stored — the
    // Android analog of iOS's "Recent episodes" list is trimmed: no
    // per-episode watched/dismissed tracking (that's iOS's EpisodeSync, a
    // separate sync subsystem not ported — see CLAUDE.md), just the
    // season's episode list for reference.
    private fun loadEpisodes(tmdbId: Int, season: Int) {
        viewModelScope.launch {
            _episodes.value = runCatching {
                TmdbClient.api.seasonDetail(tmdbId, season).episodes
            }.getOrDefault(emptyList())
        }
    }

    fun setWatched(watched: Boolean) {
        val current = _show.value ?: return
        viewModelScope.launch { dao.update(current.copy(watched = watched)) }
    }

    fun setDayOffset(newOffset: Int) {
        val current = _show.value ?: return
        val clamped = newOffset.coerceIn(TrackedShow.dayOffsetMin, TrackedShow.dayOffsetMax)
        viewModelScope.launch { dao.update(current.copy(dayOffset = clamped)) }
    }

    fun delete() {
        val current = _show.value ?: return
        viewModelScope.launch { dao.delete(current) }
    }
}
