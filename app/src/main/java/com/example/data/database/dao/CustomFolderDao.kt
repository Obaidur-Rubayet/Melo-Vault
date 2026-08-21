package com.example.data.database.dao

import androidx.room.*
import com.example.data.database.entity.CustomFolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomFolderDao {
    @Query("SELECT * FROM custom_folders ORDER BY addedAt DESC")
    fun getAllFolders(): Flow<List<CustomFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: CustomFolderEntity): Long

    @Delete
    suspend fun deleteFolder(folder: CustomFolderEntity)

    @Query("DELETE FROM custom_folders WHERE id = :id")
    suspend fun deleteFolderById(id: Long)
}
