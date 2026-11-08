package com.streamcueapp.streamcue.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.streamcueapp.streamcue.model.TrackedShow

@Database(entities = [TrackedShow::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class StreamCueDatabase : RoomDatabase() {
    abstract fun trackedShowDao(): TrackedShowDao

    companion object {
        @Volatile private var instance: StreamCueDatabase? = null

        fun get(context: Context): StreamCueDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    StreamCueDatabase::class.java,
                    "streamcue.db"
                ).build().also { instance = it }
            }
    }
}
