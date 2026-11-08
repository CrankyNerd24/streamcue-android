package com.streamcueapp.streamcue.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.streamcueapp.streamcue.StreamCueApp
import com.streamcueapp.streamcue.model.IgnoredTitle
import com.streamcueapp.streamcue.model.TrackedMovie
import com.streamcueapp.streamcue.model.TrackedShow
import com.streamcueapp.streamcue.network.TmdbClient
import com.streamcueapp.streamcue.network.availability
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class TitlePreview(
    val title: String,
    val overview: String,
    val posterPath: String?,
    val scheduleOrSubtitle: String,
    val headlineRating: String?,
    val freeOn: List<String>,
    val subscriptionOn: List<String>,
    val rentOrBuyOn: List<String>,
    val watchLink: String?,
)

/**
 * Unsaved-title preview, the Android analog of iOS's TitlePreviewSheet —
 * fetches straight from TMDB rather than Room, since the title shown here
 * usually isn't tracked yet. Three things change state: Add, Watched (add
 * then mark watched — one insert, not two, same as iOS's markWatched()),
 * and Not interested (records an IgnoredTitle, nothing tracked).
 */
class TitlePreviewViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as StreamCueApp
    private val showDao = app.database.trackedShowDao()
    private val movieDao = app.database.trackedMovieDao()
    private val ignoredDao = app.database.ignoredTitleDao()

    private val _preview = MutableStateFlow<TitlePreview?>(null)
    val preview: StateFlow<TitlePreview?> = _preview

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _isTracked = MutableStateFlow(false)
    val isTracked: StateFlow<Boolean> = _isTracked

    private var loadedKey: String? = null
    private var kind: DiscoverKind = DiscoverKind.TV
    private var tmdbId: Int = 0
    private var rawTitle: String = ""
    private var rawPosterPath: String? = null
    private var rawOverview: String = ""
    private var rawGenreIds: List<Int> = emptyList()

    fun load(kind: DiscoverKind, tmdbId: Int) {
        val key = "$kind-$tmdbId"
        if (loadedKey == key) return
        loadedKey = key
        this.kind = kind
        this.tmdbId = tmdbId

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _isTracked.value = when (kind) {
                    DiscoverKind.TV -> showDao.find(tmdbId) != null
                    DiscoverKind.MOVIES -> movieDao.find(tmdbId) != null
                }
                _preview.value = when (kind) {
                    DiscoverKind.TV -> loadShow(tmdbId)
                    DiscoverKind.MOVIES -> loadMovie(tmdbId)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = "Couldn't reach TMDB. Check your connection."
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadShow(tmdbId: Int): TitlePreview {
        val details = TmdbClient.api.showDetail(tmdbId)
        val avail = TmdbClient.api.showWatchProviders(tmdbId).availability()
        rawTitle = details.name
        rawPosterPath = details.posterPath
        rawOverview = details.overview
        rawGenreIds = details.genres.map { it.id }
        return TitlePreview(
            title = details.name,
            overview = details.overview,
            posterPath = details.posterPath,
            scheduleOrSubtitle = details.status,
            headlineRating = headline(details.voteAverage),
            freeOn = avail.free,
            subscriptionOn = avail.subscription,
            rentOrBuyOn = avail.rentOrBuy,
            watchLink = avail.link,
        )
    }

    private suspend fun loadMovie(tmdbId: Int): TitlePreview {
        val details = TmdbClient.api.movieDetail(tmdbId)
        val avail = TmdbClient.api.movieWatchProviders(tmdbId).availability()
        rawTitle = details.title
        rawPosterPath = details.posterPath
        rawOverview = details.overview
        rawGenreIds = details.genres.map { it.id }
        return TitlePreview(
            title = details.title,
            overview = details.overview,
            posterPath = details.posterPath,
            scheduleOrSubtitle = details.releaseDate?.take(4).orEmpty(),
            headlineRating = headline(details.voteAverage),
            freeOn = avail.free,
            subscriptionOn = avail.subscription,
            rentOrBuyOn = avail.rentOrBuy,
            watchLink = avail.link,
        )
    }

    private fun headline(voteAverage: Double?): String? =
        if ((voteAverage ?: 0.0) > 0.0) "TMDB " + "%.1f".format(voteAverage) else null

    /** @param watched For a show this is just an initial value — the next
     * refresh can't correct it the way iOS's episode sync would, since
     * that subsystem isn't ported (see CLAUDE.md). */
    fun add(watched: Boolean = false) {
        viewModelScope.launch {
            when (kind) {
                DiscoverKind.TV -> showDao.insert(
                    TrackedShow(
                        tmdbId = tmdbId,
                        name = rawTitle,
                        posterPath = rawPosterPath,
                        overview = rawOverview,
                        genreIds = rawGenreIds,
                        watched = watched,
                    )
                )
                DiscoverKind.MOVIES -> movieDao.insert(
                    TrackedMovie(
                        tmdbId = tmdbId,
                        title = rawTitle,
                        posterPath = rawPosterPath,
                        overview = rawOverview,
                        genreIds = rawGenreIds,
                        watched = watched,
                    )
                )
            }
            _isTracked.value = true
        }
    }

    fun ignore() {
        viewModelScope.launch {
            ignoredDao.insert(
                IgnoredTitle(
                    tmdbId = tmdbId,
                    kind = kind.name,
                    title = rawTitle,
                    posterPath = rawPosterPath,
                )
            )
        }
    }
}
