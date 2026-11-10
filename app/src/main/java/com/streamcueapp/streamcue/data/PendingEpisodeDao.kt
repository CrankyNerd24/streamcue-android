package com.streamcueapp.streamcue.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.streamcueapp.streamcue.model.PendingEpisode
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingEpisodeDao {
    // Outstanding episodes across every tracked show — the Shows tab's
    // Ready to watch section. Grouped by show first, same as iOS, so a show
    // with several ready episodes reads as one consecutive run.
    @Query(
        "SELECT * FROM pending_episodes WHERE watched = 0 AND dismissed = 0 " +
            "ORDER BY showName, seasonNumber, episodeNumber"
    )
    fun observePending(): Flow<List<PendingEpisode>>

    // Every recorded episode, watched/dismissed included — used to check
    // whether a show's specific next episode has already been actioned,
    // same as iOS's unfiltered episodeRecords query.
    @Query("SELECT * FROM pending_episodes")
    fun observeAll(): Flow<List<PendingEpisode>>

    @Query("SELECT * FROM pending_episodes WHERE showId = :showId ORDER BY airDateEpochDay DESC")
    fun observeForShow(showId: Int): Flow<List<PendingEpisode>>

    @Query("SELECT * FROM pending_episodes WHERE showId = :showId ORDER BY airDateEpochDay DESC")
    suspend fun forShow(showId: Int): List<PendingEpisode>

    @Query("SELECT * FROM pending_episodes WHERE key = :key LIMIT 1")
    suspend fun find(key: String): PendingEpisode?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(episode: PendingEpisode)

    @Update
    suspend fun update(episode: PendingEpisode)

    @Query("DELETE FROM pending_episodes WHERE showId = :showId")
    suspend fun deleteAllForShow(showId: Int)

    // Safe to run outright, actioned or not — longer than EpisodeSync's own
    // 30-day window, so nothing this old could ever be re-added.
    @Query("DELETE FROM pending_episodes WHERE airDateEpochDay < :cutoffEpochDay")
    suspend fun deleteOlderThan(cutoffEpochDay: Long)
}
