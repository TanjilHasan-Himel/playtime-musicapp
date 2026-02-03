package com.eplaytime.app.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * FavoritesDAO - Data Access Object for Favorites table
 * Provides methods to insert, delete, and query favorite songs
 */
@Dao
interface FavoritesDAO {

    /**
     * Insert a song into favorites
     */
    @Insert
    suspend fun addFavorite(song: FavoriteSong)

    /**
     * Remove a song from favorites
     */
    @Delete
    suspend fun removeFavorite(song: FavoriteSong)

    /**
     * Remove a favorite by song URI
     */
    @Query("DELETE FROM favorites WHERE uri = :uri")
    suspend fun removeFavoriteByUri(uri: String)

    /**
     * Get all favorite songs (returns Flow for reactive updates)
     */
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteSong>>

    /**
     * Check if a song is favorited by URI
     */
    @Query("SELECT COUNT(*) > 0 FROM favorites WHERE uri = :uri")
    fun isFavorited(uri: String): Flow<Boolean>

    /**
     * Check if a song is favorited by ID
     */
    @Query("SELECT COUNT(*) > 0 FROM favorites WHERE id = :songId")
    fun isFavoritedById(songId: Long): Flow<Boolean>

    /**
     * Get favorite by URI
     */
    @Query("SELECT * FROM favorites WHERE uri = :uri LIMIT 1")
    suspend fun getFavoriteByUri(uri: String): FavoriteSong?

    /**
     * Get favorite by ID
     */
    @Query("SELECT * FROM favorites WHERE id = :songId LIMIT 1")
    suspend fun getFavoriteById(songId: Long): FavoriteSong?

    /**
     * Clear all favorites
     */
    @Query("DELETE FROM favorites")
    suspend fun clearAllFavorites()

    /**
     * Get count of favorites
     */
    @Query("SELECT COUNT(*) FROM favorites")
    fun getFavoritesCount(): Flow<Int>
}
