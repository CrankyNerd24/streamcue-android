package com.streamcueapp.streamcue.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.streamcueapp.streamcue.model.IgnoredTitle
import kotlinx.coroutines.flow.Flow

@Dao
interface IgnoredTitleDao {
    @Query("SELECT * FROM ignored_titles")
    fun observeAll(): Flow<List<IgnoredTitle>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(ignored: IgnoredTitle)
}
