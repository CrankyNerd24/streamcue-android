package com.streamcueapp.streamcue.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.streamcueapp.streamcue.model.TrackedMovie
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackedMovieDao {
    @Query("SELECT * FROM tracked_movies ORDER BY title")
    fun observeAll(): Flow<List<TrackedMovie>>

    @Query("SELECT * FROM tracked_movies WHERE tmdbId = :tmdbId LIMIT 1")
    suspend fun find(tmdbId: Int): TrackedMovie?

    // Dedupes on tmdbId, same reasoning as TrackedShowDao.insert.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(movie: TrackedMovie): Long

    @Update
    suspend fun update(movie: TrackedMovie)

    @Delete
    suspend fun delete(movie: TrackedMovie)
}
