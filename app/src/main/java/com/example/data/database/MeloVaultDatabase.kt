package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.database.dao.CustomFolderDao
import com.example.data.database.dao.PlaybackHistoryDao
import com.example.data.database.dao.PlaylistDao
import com.example.data.database.dao.SongDao
import com.example.data.database.entity.CustomFolderEntity
import com.example.data.database.entity.PlaybackHistoryEntity
import com.example.data.database.entity.PlaylistEntity
import com.example.data.database.entity.PlaylistSongCrossRef
import com.example.data.database.entity.SongEntity

@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        CustomFolderEntity::class,
        PlaybackHistoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class MeloVaultDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun customFolderDao(): CustomFolderDao
    abstract fun playbackHistoryDao(): PlaybackHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: MeloVaultDatabase? = null

        fun getDatabase(context: Context): MeloVaultDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MeloVaultDatabase::class.java,
                    "melovault_music_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
