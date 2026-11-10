package com.streamcueapp.streamcue.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.streamcueapp.streamcue.StreamCueApp
import com.streamcueapp.streamcue.data.EpisodeSync
import com.streamcueapp.streamcue.data.Refresh
import com.streamcueapp.streamcue.model.PendingEpisode
import com.streamcueapp.streamcue.model.TrackedShow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ShowDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as StreamCueApp).database.trackedShowDao()
    private val episodeDao = (application as StreamCueApp).database.pendingEpisodeDao()

    private val _show = MutableStateFlow<TrackedShow?>(null)
    val show: StateFlow<TrackedShow?> = _show

    // The persisted, newest-first "Recent episodes" list — the Android
    // analog of iOS's @Query over PendingEpisode, filtered to this show.
    private val _episodes = MutableStateFlow<List<PendingEpisode>>(emptyList())
    val episodes: StateFlow<List<PendingEpisode>> = _episodes

    private var loadedId: Int? = null

    fun load(tmdbId: Int) {
        if (loadedId == tmdbId) return
        loadedId = tmdbId
        viewModelScope.launch {
            dao.observe(tmdbId).collect { current ->
                _show.value = current
                if (current == null) return@collect
                if (current.isStale) {
                    runCatching {
                        Refresh.show(dao, current)
                        val refreshed = dao.find(current.tmdbId) ?: return@runCatching
                        EpisodeSync.sync(episodeDao, dao, refreshed)
                    }
                }
            }
        }
        viewModelScope.launch {
            episodeDao.observeForShow(tmdbId).collect { _episodes.value = it }
        }
    }

    fun setWatched(watched: Boolean) {
        val current = _show.value ?: return
        viewModelScope.launch { dao.update(current.copy(watched = watched)) }
    }

    /** Marks an outstanding episode watched, or undoes a watched or
     * dismissed one, then re-derives the show's caught-up flag the same
     * way the Shows tab does: caught up once nothing it aired is still
     * outstanding. Mirrors iOS's ShowDetailView.setWatched. */
    fun setEpisodeWatched(episode: PendingEpisode, watched: Boolean) {
        viewModelScope.launch {
            episodeDao.update(episode.copy(watched = watched, dismissed = false))
            val current = dao.find(episode.showId) ?: return@launch
            val siblings = episodeDao.forShow(episode.showId)
            val outstanding = !watched || siblings.any {
                it.key != episode.key && !it.watched && !it.dismissed
            }
            if (current.watched != !outstanding) {
                dao.update(current.copy(watched = !outstanding))
            }
        }
    }

    fun setDayOffset(newOffset: Int) {
        val current = _show.value ?: return
        val clamped = newOffset.coerceIn(TrackedShow.dayOffsetMin, TrackedShow.dayOffsetMax)
        viewModelScope.launch { dao.update(current.copy(dayOffset = clamped)) }
    }

    fun delete() {
        val current = _show.value ?: return
        viewModelScope.launch {
            episodeDao.deleteAllForShow(current.tmdbId)
            dao.delete(current)
        }
    }
}
