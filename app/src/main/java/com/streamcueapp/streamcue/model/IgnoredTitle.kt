package com.streamcueapp.streamcue.model

import androidx.room.Entity

/**
 * Something the user has said they're not interested in — ported from
 * iOS's IgnoredTitle. Kept as a record (not a flag on some other row)
 * since there's nothing else to attach it to: the title was never
 * tracked. `kind` is a DiscoverKind name ("TV"/"MOVIES") — the two id
 * spaces can collide, so it's part of the primary key.
 */
@Entity(tableName = "ignored_titles", primaryKeys = ["tmdbId", "kind"])
data class IgnoredTitle(
    val tmdbId: Int,
    val kind: String,
    val title: String,
    val posterPath: String? = null,
    val addedAtEpochMillis: Long = System.currentTimeMillis(),
)
