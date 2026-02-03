package com.eplaytime.app.data.repository

import com.eplaytime.app.data.database.FavoriteSong
import com.eplaytime.app.data.database.FavoritesDAO
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FavoritesRepository - Repository for managing favorite songs
 * Provides a single access point to favorites database operations
 */
@Singleton
class FavoritesRepository @Inject constructor(
    private val favoritesDAO: FavoritesDAO
) {

    /**
     * Get all favorite songs as a Flow (reactive updates)
     */
    fun getAllFavorites(): Flow<List<FavoriteSong>> = favoritesDAO.getAllFavorites()

    /**
     * Check if a song is favorited by URI
     */
    fun isFavorited(uri: String): Flow<Boolean> = favoritesDAO.isFavorited(uri)

    /**
     * Check if a song is favorited by ID
     */
    fun isFavoritedById(songId: Long): Flow<Boolean> = favoritesDAO.isFavoritedById(songId)

    /**
     * Add a song to favorites
     */
    suspend fun addFavorite(song: FavoriteSong) {
        favoritesDAO.addFavorite(song)
    }

    /**
     * Remove a song from favorites by URI
     */
    suspend fun removeFavoriteByUri(uri: String) {
        favoritesDAO.removeFavoriteByUri(uri)
    }

    /**
     * Remove a song from favorites
     */
    suspend fun removeFavorite(song: FavoriteSong) {
        favoritesDAO.removeFavorite(song)
    }

    /**
     * Get count of favorites
     */
    fun getFavoritesCount(): Flow<Int> = favoritesDAO.getFavoritesCount()

    /**
     * Clear all favorites
     */
    suspend fun clearAllFavorites() {
        favoritesDAO.clearAllFavorites()
    }
}
