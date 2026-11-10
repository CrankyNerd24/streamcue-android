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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as StreamCueApp
    private val showDao = app.database.trackedShowDao()
    private val episodeDao = app.database.pendingEpisodeDao()
    private val collapseStore = app.sectionCollapseStore

    val shows: StateFlow<List<TrackedShow>> = showDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Outstanding episodes across every show — the Ready to watch section.
    val pendingEpisodes: StateFlow<List<PendingEpisode>> = episodeDao.observePending()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Keys of every watched-or-dismissed episode ever recorded, so a show's
    // specific next episode can be checked even after it's left `pending` —
    // the same exclusion iOS's `dated` applies against `episodeRecords`.
    val actionedEpisodeKeys: StateFlow<Set<String>> = episodeDao.observeAll()
        .map { all -> all.filter { it.watched || it.dismissed }.map { it.key }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val collapsedSections: StateFlow<Set<String>> = collapseStore.collapsed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val readyExpanded: StateFlow<Boolean> = collapseStore.readyExpanded
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    fun toggleSection(title: String) {
        viewModelScope.launch {
            collapseStore.setCollapsed(title, title !in collapsedSections.value)
        }
    }

    fun toggleReadyExpanded() {
        viewModelScope.launch { collapseStore.setReadyExpanded(!readyExpanded.value) }
    }

    /** Ticks an outstanding episode off as watched. Returns whether this
     * caught its show up, so the caller's Undo can flip it back — mirrors
     * iOS's onWatched + markCaughtUpIfNeeded. */
    suspend fun markWatched(episode: PendingEpisode): Boolean {
        episodeDao.update(episode.copy(watched = true, dismissed = false))
        return markCaughtUpIfNeeded(episode)
    }

    /** Dismisses an outstanding episode without marking it watched. Same
     * caught-up bookkeeping as markWatched — mirrors iOS's onDismiss. */
    suspend fun dismiss(episode: PendingEpisode): Boolean {
        episodeDao.update(episode.copy(watched = false, dismissed = true))
        return markCaughtUpIfNeeded(episode)
    }

    /** Puts the episode back in Ready to watch, and un-catches-up its show
     * if ticking it off is what caught it up — mirrors iOS's undo(_:). */
    suspend fun undoEpisodeAction(episode: PendingEpisode, markedShowCaughtUp: Boolean) {
        episodeDao.update(episode.copy(watched = false, dismissed = false))
        if (!markedShowCaughtUp) return
        val show = showDao.find(episode.showId) ?: return
        if (show.watched) showDao.update(show.copy(watched = false))
    }

    /** "Watched" for a show is derived, not manual: caught up once nothing
     * it aired is still outstanding. */
    private suspend fun markCaughtUpIfNeeded(actioned: PendingEpisode): Boolean {
        val show = showDao.find(actioned.showId) ?: return false
        val stillPending = episodeDao.forShow(actioned.showId)
            .any { it.key != actioned.key && !it.watched && !it.dismissed }
        if (stillPending || show.watched) return false
        showDao.update(show.copy(watched = true))
        return true
    }

    /** Pull-to-refresh: re-fetches every tracked show from TMDB and syncs
     * its episodes, same as iOS's refreshAll(). */
    fun refreshAll() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                shows.value.forEach { show ->
                    runCatching {
                        Refresh.show(showDao, show)
                        val refreshed = showDao.find(show.tmdbId) ?: return@runCatching
                        EpisodeSync.sync(episodeDao, showDao, refreshed)
                    }
                }
                EpisodeSync.prune(episodeDao)
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
