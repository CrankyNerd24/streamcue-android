package com.streamcueapp.streamcue.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.streamcueapp.streamcue.model.IgnoredTitle
import com.streamcueapp.streamcue.model.TrackedMovie
import com.streamcueapp.streamcue.model.TrackedShow

@Database(
    entities = [TrackedShow::class, TrackedMovie::class, IgnoredTitle::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class StreamCueDatabase : RoomDatabase() {
    abstract fun trackedShowDao(): TrackedShowDao
    abstract fun trackedMovieDao(): TrackedMovieDao
    abstract fun ignoredTitleDao(): IgnoredTitleDao

    companion object {
        @Volatile private var instance: StreamCueDatabase? = null

        fun get(context: Context): StreamCueDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    StreamCueDatabase::class.java,
                    "streamcue.db"
                )
                    // Pre-release: no installed base to migrate yet, so a
                    // schema bump just resets the local store instead of
                    // crashing on launch.
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { instance = it }
            }
    }
}
