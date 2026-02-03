package com.eplaytime.app.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * FavoriteSong - Room Entity for storing favorited songs
 * Allows users to mark songs as favorites and persist them across app restarts
 */
@Entity(tableName = "favorites")
data class FavoriteSong(
    @PrimaryKey
    val id: Long,           // Song ID from MediaStore
    val title: String,      // Song title
    val artist: String,     // Artist name
    val uri: String,        // Content URI for playback
    val albumArtUri: String? = null,  // Album art URI (optional)
    val addedAt: Long = System.currentTimeMillis()  // Timestamp when favorited
)
