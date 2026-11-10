package com.streamcueapp.streamcue.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * An episode that has already aired and hasn't been watched or dismissed —
 * mirrors the iOS app's PendingEpisode (SwiftData) model field for field.
 * `key` ("showId-season-episode") is the primary key directly, rather than
 * a separate id plus a uniqueness check like iOS's CloudKit-safe model —
 * Room enforces it for real, and there's no multi-device store to merge.
 */
@Entity(tableName = "pending_episodes")
data class PendingEpisode(
    @PrimaryKey val key: String,
    val showId: Int,
    val showName: String,
    val posterPath: String? = null,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String? = null,
    val airDateEpochDay: Long? = null,
    val watched: Boolean = false,
    val dismissed: Boolean = false,
    val addedAtEpochMillis: Long = System.currentTimeMillis(),
) {
    /** "S03E07 · Title" — same format as TmdbEpisode.label / iOS's label. */
    val label: String
        get() {
            val number = "S%02dE%02d".format(seasonNumber, episodeNumber)
            return if (title.isNullOrEmpty()) number else "$number · $title"
        }

    /** "Aired today" / "Aired Thu, Mar 7" — same as iOS's airedSummary. */
    val airedSummary: String
        get() {
            val day = airDateEpochDay ?: return "Aired"
            if (day == LocalDate.now().toEpochDay()) return "Aired today"
            val formatted = LocalDate.ofEpochDay(day)
                .format(DateTimeFormatter.ofPattern("EEE, MMM d"))
            return "Aired $formatted"
        }

    companion object {
        fun key(showId: Int, season: Int, episode: Int) = "$showId-$season-$episode"
    }
}
