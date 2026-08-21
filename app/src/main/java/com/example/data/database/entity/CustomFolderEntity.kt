package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_folders")
data class CustomFolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uriString: String,
    val displayName: String,
    val songCount: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)
