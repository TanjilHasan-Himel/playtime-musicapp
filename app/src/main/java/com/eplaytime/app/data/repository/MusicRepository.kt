package com.eplaytime.app.data.repository

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.eplaytime.app.data.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Album data model
 */
data class Album(
    val id: Long,
    val name: String,
    val artist: String,
    val albumArtUri: String?,
    val songCount: Int
)

/**
 * Artist data model
 */
data class Artist(
    val id: Long,
    val name: String,
    val songCount: Int
)

/**
 * Genre data model
 */
data class Genre(
    val id: Long,
    val name: String
)

/**
 * Folder data model
 */
data class MusicFolder(
    val path: String,
    val name: String,
    val songCount: Int
)

/**
 * MusicRepository - Scans and retrieves music from device storage
 * Supports: Songs, Albums, Artists, Genres, Folders
 */
@Singleton
class MusicRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songDao: com.eplaytime.app.data.dao.SongDao
) {
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs

    // ... (other flows stay same)
    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums

    private val _artists = MutableStateFlow<List<Artist>>(emptyList())
    val artists: StateFlow<List<Artist>> = _artists

    private val _genres = MutableStateFlow<List<Genre>>(emptyList())
    val genres: StateFlow<List<Genre>> = _genres

    private val _folders = MutableStateFlow<List<MusicFolder>>(emptyList())
    val folders: StateFlow<List<MusicFolder>> = _folders

    companion object {
        private const val TAG = "MusicRepository"
        private const val MIN_DURATION_MS = 30000L
    }

    /**
     * Get all songs from Local Database (with in-memory filtering)
     * This is FAST and does NOT access MediaStore.
     */
    suspend fun getAllSongs(filterShortAudio: Boolean = false, filterCallRecordings: Boolean = false): List<Song> = withContext(Dispatchers.IO) {
        // 1. Fetch from DB
        val allSongs = songDao.getAllSongs()
        
        // 2. Apply Filters in Memory
        val filteredSongs = allSongs.filter { song ->
            var include = true
            
            // Filter Short Audio
            if (filterShortAudio && song.duration < MIN_DURATION_MS) {
                include = false
            }
            
            // Filter Call Recordings
            if (include && filterCallRecordings) {
                val path = song.folderPath.lowercase() // normalized path or data
                // Note: Song.folderPath is populated in scanFromMediaStore.
                // We also check uri or title if needed, but path is best.
                // The filter logic from SQL: NOT LIKE %/Call/% etc.
                if (path.contains("/call/") || 
                    path.contains("/record") || 
                    path.contains("/voice") || 
                    path.contains("whatsapp audio")) {
                    include = false
                }
            }
            include
        }

        _songs.value = filteredSongs
        filteredSongs
    }

    /**
     * Rescan device for music files
     * Scans MediaStore (SLOW) -> Updates Database -> Returns fresh list
     */
    suspend fun rescanDevice(filterShortAudio: Boolean = false, filterCallRecordings: Boolean = false) {
        withContext(Dispatchers.IO) {
            val freshSongs = scanFromMediaStore()
            Log.d(TAG, "Scanned ${freshSongs.size} songs from MediaStore. Updating Database...")
            
            songDao.deleteAll()
            songDao.insertAll(freshSongs)
            
            // Refresh exposed flow with filters applied
            getAllSongs(filterShortAudio, filterCallRecordings)
        }
    }

    /**
     * Check if there are changes in MediaStore vs Database
     */
    suspend fun checkForChanges(): Boolean = withContext(Dispatchers.IO) {
        val dbCount = songDao.getAllSongs().size
        var mediaStoreCount = 0
        try {
            val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(MediaStore.Audio.Media._ID)
            val selection = "${MediaStore.Audio.Media.DURATION} > 0"
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                null
            )?.use { c ->
                mediaStoreCount = c.count
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking media store count", e)
        }
        
        // If counts differ by more than 0, assume changes.
        // Simple heuristic.
        dbCount != mediaStoreCount
    }

    /**
     * Private: Query MediaStore for ALL audio files (Broad search)
     */
    private fun scanFromMediaStore(): List<Song> {
        Log.d(TAG, "=== Starting Raw MediaStore Scan ===")
        val songs = mutableListOf<Song>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA,
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.BUCKET_DISPLAY_NAME
            } else {
                MediaStore.Audio.Media.DATA
            }
        )

        // Minimal selection: DURATION > 0 (to avoid corrupted files)
        val selection = "${MediaStore.Audio.Media.DURATION} > 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )?.use { c ->
                val idColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val dateAddedColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val dataColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                
                val bucketColumn = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    c.getColumnIndex(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME)
                } else {
                    -1
                }

                while (c.moveToNext()) {
                    try {
                        val id = c.getLong(idColumn)
                        val title = c.getString(titleColumn) ?: "Unknown"
                        val artist = c.getString(artistColumn) ?: "Unknown Artist"
                        val album = c.getString(albumColumn) ?: "Unknown Album"
                        val duration = c.getLong(durationColumn)
                        val albumId = c.getLong(albumIdColumn)
                        val dateAdded = c.getLong(dateAddedColumn)
                        val filePath = c.getString(dataColumn) ?: ""
                        
                        var bucketName = "Unknown"
                        if (bucketColumn != -1) {
                            bucketName = c.getString(bucketColumn) ?: "Unknown"
                        }
                        
                        // Fallback logic if bucket name is missing or unknown
                        if (bucketName == "Unknown" || bucketName.isEmpty()) {
                             try {
                                val parent = java.io.File(filePath).parentFile
                                bucketName = parent?.name ?: "Root"
                            } catch (e: Exception) {
                                bucketName = "Root"
                            }
                        }

                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id
                        ).toString()

                        val albumArtUri = ContentUris.withAppendedId(
                            Uri.parse("content://media/external/audio/albumart"),
                            albumId
                        ).toString()

                        songs.add(
                            Song(
                                id = id,
                                title = title,
                                artist = artist,
                                album = album,
                                uri = contentUri,
                                duration = duration,
                                albumArtUri = albumArtUri,
                                dateAdded = dateAdded,
                                folderPath = filePath,
                                bucketDisplayName = bucketName
                            )
                        )
                    } catch (e: Exception) {
                        continue
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning MediaStore", e)
        }
        return songs
    }

    /**
     * Get all albums
     */
    suspend fun getAllAlbums(): List<Album> = withContext(Dispatchers.IO) {
        val albumList = mutableListOf<Album>()
        val collection = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.NUMBER_OF_SONGS
        )

        context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            "${MediaStore.Audio.Albums.ALBUM} ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)
            val countColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(albumColumn) ?: "Unknown Album"
                val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val count = cursor.getInt(countColumn)

                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    id
                ).toString()

                albumList.add(Album(id, name, artist, albumArtUri, count))
            }
        }

        _albums.value = albumList
        albumList
    }

    /**
     * Get all artists
     */
    suspend fun getAllArtists(): List<Artist> = withContext(Dispatchers.IO) {
        val artistList = mutableListOf<Artist>()
        val collection = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Artists._ID,
            MediaStore.Audio.Artists.ARTIST,
            MediaStore.Audio.Artists.NUMBER_OF_TRACKS
        )

        context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            "${MediaStore.Audio.Artists.ARTIST} ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST)
            val countColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_TRACKS)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(artistColumn) ?: "Unknown Artist"
                val count = cursor.getInt(countColumn)

                artistList.add(Artist(id, name, count))
            }
        }

        _artists.value = artistList
        artistList
    }

    /**
     * Get all genres
     */
    suspend fun getAllGenres(): List<Genre> = withContext(Dispatchers.IO) {
        val genreList = mutableListOf<Genre>()
        val collection = MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Genres._ID,
            MediaStore.Audio.Genres.NAME
        )

        context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            "${MediaStore.Audio.Genres.NAME} ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Genres._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn) ?: "Unknown Genre"

                if (name.isNotBlank() && name != "<unknown>") {
                    genreList.add(Genre(id, name))
                }
            }
        }

        _genres.value = genreList
        genreList
    }

    /**
     * Get folders from loaded songs
     */
    suspend fun getAllFolders(): List<MusicFolder> = withContext(Dispatchers.IO) {
        val songs = _songs.value.ifEmpty { getAllSongs() }

        // Group by Bucket Display Name (e.g. "Download", "Music")
        // We use the first song's path as the folder path for navigation
        val folderMap = songs.groupBy { it.bucketDisplayName } // Group by Name
        val folderList = folderMap.map { (bucketName, songsInFolder) ->
            MusicFolder(
                path = bucketName.ifEmpty { "Root" }, // Use Bucket Name as ID/Path
                name = bucketName.ifEmpty { "Root" },
                songCount = songsInFolder.size
            )
        }.sortedBy { it.name }

        _folders.value = folderList
        folderList
    }

    /**
     * Get songs by folder path - NOT used anymore if we group by bucket,
     * BUT we need to filter by bucket name for navigation.
     * Let's change this to filter by bucketDisplayName
     */
    fun getSongsByFolder(folderName: String): List<Song> {
        return _songs.value.filter { it.bucketDisplayName == folderName }
    }

    /**
     * Get songs by album
     */
    fun getSongsByAlbum(albumName: String): List<Song> {
        return _songs.value.filter { it.album == albumName }
    }

    /**
     * Get songs by artist
     */
    fun getSongsByArtist(artistName: String): List<Song> {
        return _songs.value.filter { it.artist == artistName }
    }

}
