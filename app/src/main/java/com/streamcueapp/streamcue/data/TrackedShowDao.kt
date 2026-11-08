package com.streamcueapp.streamcue.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.streamcueapp.streamcue.model.TrackedShow
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackedShowDao {
    @Query("SELECT * FROM tracked_shows ORDER BY name")
    fun observeAll(): Flow<List<TrackedShow>>

    @Query("SELECT COUNT(*) FROM tracked_shows")
    suspend fun count(): Int

    @Query("SELECT * FROM tracked_shows WHERE tmdbId = :tmdbId LIMIT 1")
    suspend fun find(tmdbId: Int): TrackedShow?

    @Query("SELECT * FROM tracked_shows WHERE tmdbId = :tmdbId LIMIT 1")
    fun observe(tmdbId: Int): Flow<TrackedShow?>

    // Dedupes on tmdbId the same way Library.addShow does on iOS — never
    // insert a tracked show any other way.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(show: TrackedShow): Long

    @Update
    suspend fun update(show: TrackedShow)

    @Delete
    suspend fun delete(show: TrackedShow)
}
