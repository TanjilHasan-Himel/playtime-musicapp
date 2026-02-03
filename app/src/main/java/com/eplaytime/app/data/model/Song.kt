package com.eplaytime.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Song data model - Preserved in Room Database
 */
@Entity(tableName = "songs")
data class Song(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val uri: String,
    val duration: Long,
    val albumArtUri: String? = null,
    val dateAdded: Long = 0L,
    val folderPath: String = "",
    val bucketDisplayName: String = ""
)
