package com.eplaytime.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.eplaytime.app.data.dao.ScheduledTaskDao
import com.eplaytime.app.data.dao.SongDao
import com.eplaytime.app.data.model.ScheduledTask
import com.eplaytime.app.data.model.Song

/**
 * PlayTime Room Database
 */
@Database(entities = [ScheduledTask::class, FavoriteSong::class, Song::class], version = 6, exportSchema = false)
abstract class PlayTimeDatabase : RoomDatabase() {

    abstract fun scheduledTaskDao(): ScheduledTaskDao
    abstract fun favoritesDAO(): FavoritesDAO
    abstract fun songDao(): SongDao

    companion object {
        @Volatile
        private var INSTANCE: PlayTimeDatabase? = null

        fun getDatabase(context: Context): PlayTimeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PlayTimeDatabase::class.java,
                    "playtime_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
