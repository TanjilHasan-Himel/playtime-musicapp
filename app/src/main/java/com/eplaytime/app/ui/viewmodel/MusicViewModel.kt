package com.eplaytime.app.ui.viewmodel

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.eplaytime.app.data.datastore.PlayTimeDataStore
import com.eplaytime.app.data.database.FavoriteSong
import com.eplaytime.app.data.model.Song
import com.eplaytime.app.data.repository.Album
import com.eplaytime.app.data.repository.Artist
import com.eplaytime.app.data.repository.FavoritesRepository
import com.eplaytime.app.data.repository.MusicFolder
import com.eplaytime.app.data.repository.MusicRepository
import com.eplaytime.app.service.MusicService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MusicViewModel - Connects Music Service to UI
 * Manages playback state and communicates with MediaController
 * Supports: Songs, Albums, Artists, Folders, Favorites, Smart Resume
 */
@HiltViewModel
class MusicViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicRepository: MusicRepository,
    private val favoritesRepository: FavoritesRepository,
    private val dataStore: PlayTimeDataStore
) : ViewModel() {

    companion object {
        private const val PREFS_NAME = "playtime_prefs"
        private const val PREFS_FAVORITES = "favorite_song_ids"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // MediaController for controlling playback
    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var isControllerReady = false
    private var pendingPlayUri: String? = null
    private var playlistPrepared = false
    private var isRestoring = true // Lock: Prevent overwrite during restore

    // State flows for UI
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _progress = MutableStateFlow(0L)
    val progress: StateFlow<Long> = _progress.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _songList = MutableStateFlow<List<Song>>(emptyList())
    val songList: StateFlow<List<Song>> = _songList.asStateFlow()

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    private val _artists = MutableStateFlow<List<Artist>>(emptyList())
    val artists: StateFlow<List<Artist>> = _artists.asStateFlow()

    private val _folders = MutableStateFlow<List<MusicFolder>>(emptyList())
    val folders: StateFlow<List<MusicFolder>> = _folders.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _favoriteSongIds = MutableStateFlow<Set<Long>>(emptySet())
    val favoriteSongIds: StateFlow<Set<Long>> = _favoriteSongIds.asStateFlow()

    // Flag for Sync needed
    private val _hasNewSongs = MutableStateFlow(false)
    val hasNewSongs: StateFlow<Boolean> = _hasNewSongs.asStateFlow()

    // Database-backed favorites
    val allFavorites = favoritesRepository.getAllFavorites()

    // A-B Loop state
    private val _loopStart = MutableStateFlow<Long?>(null)
    val loopStart: StateFlow<Long?> = _loopStart.asStateFlow()

    private val _loopEnd = MutableStateFlow<Long?>(null)
    val loopEnd: StateFlow<Long?> = _loopEnd.asStateFlow()

    private val _loopEnabled = MutableStateFlow(false)
    val loopEnabled: StateFlow<Boolean> = _loopEnabled.asStateFlow()

    // Progress update job
    private var progressUpdateJob: Job? = null

    init {
        initializeMediaController()
        loadLocalSongs()
        loadFavorites()
        
        // Check for new songs in background
        viewModelScope.launch {
            val changes = musicRepository.checkForChanges()
            if (changes) {
                _hasNewSongs.value = true
            }
        }

        // CRITICAL: Sync with service BEFORE restoring state from DataStore
        syncWithRunningService()
    }

    /**
     * SINGLE SOURCE OF TRUTH FIX:
     * If music is already playing in MusicService, sync to it.
     * Do NOT reset playback or load from DataStore.
     * This prevents the MiniPlayer -> PlayerScreen reset bug.
     */
    private fun syncWithRunningService() {
        viewModelScope.launch {
            // Wait for controller to be ready
            while (mediaController == null) {
                delay(100)
            }
            
            mediaController?.let { controller ->
                val isServiceActive = controller.playbackState != Player.STATE_IDLE && 
                                     (controller.isPlaying || controller.mediaItemCount > 0)
                
                if (isServiceActive) {
                    // Service is active - sync UI to service state
                    val currentItem = controller.currentMediaItem
                    if (currentItem != null) {
                        val songUri = currentItem.localConfiguration?.uri.toString()
                        // Find song object from loaded list (wait if list is empty)
                        var retryCount = 0
                        while (_songList.value.isEmpty() && retryCount < 10) {
                            delay(200)
                            retryCount++
                        }

                        val song = _songList.value.find { it.uri == songUri }
                        if (song != null) {
                            _currentSong.value = song
                            _duration.value = controller.duration
                            _isPlaying.value = controller.isPlaying
                            _shuffleEnabled.value = controller.shuffleModeEnabled
                            _repeatMode.value = controller.repeatMode
                            
                            android.util.Log.d("MusicViewModel", "✓ Synced with running service: ${song.title}")
                            
                            // Start progress updates if playing
                            if (controller.isPlaying) {
                                startProgressUpdates()
                            }
                            
                            // Important: Mark playlist as prepared so loadLocalSongs doesn't overwrite it
                            playlistPrepared = true
                            isRestoring = false // Unlock
                            return@launch // Don't load from DataStore
                        } else {
                            // EDGE CASE: Alarm or External file playing (not in our DB)
                            // Create a temporary "Foreign" song so UI doesn't crash or reset
                            val mediaItem = controller.currentMediaItem
                            val tempSong = Song(
                                id = -1L,
                                title = mediaItem?.mediaMetadata?.title?.toString() ?: "External Audio",
                                artist = mediaItem?.mediaMetadata?.artist?.toString() ?: "System",
                                uri = songUri,
                                album = "Unknown",
                                duration = controller.duration,
                                albumArtUri = null,
                                dateAdded = 0L,
                                folderPath = ""
                            )
                            _currentSong.value = tempSong
                            _isPlaying.value = controller.isPlaying
                            _duration.value = controller.duration
                            
                            android.util.Log.d("MusicViewModel", "⚠ Synced with FOREIGN audio (Alarm?): ${tempSong.title}")
                            
                            if (controller.isPlaying) startProgressUpdates()
                            playlistPrepared = true
                            isRestoring = false // Unlock
                            return@launch
                        }
                    }
                }
                
                // Service is idle or completely new session - restore from DataStore
                android.util.Log.d("MusicViewModel", "Service idle, restoring from DataStore")
                restorePlaybackState()
            }
        }
    }

    /**
     * Initialize MediaController to connect to MusicService
     */
    private fun initializeMediaController() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, MusicService::class.java)
        )

        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener(
            {
                mediaController = controllerFuture?.get()
                isControllerReady = true
                setupPlayerListener()
                preparePlaylistIfReady()
                pendingPlayUri?.let { uri ->
                    playSongInternal(uri)
                }
            },
            MoreExecutors.directExecutor()
        )
    }

    /**
     * Setup listener to sync player state with UI
     */
    private fun setupPlayerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    startProgressUpdates()
                } else {
                    stopProgressUpdates()
                    // EAGER SAVE: Save state immediately on Pause (Notification/Earbuds)
                    viewModelScope.launch {
                        _currentSong.value?.let { song ->
                            dataStore.savePlaybackState(song.uri, mediaController?.currentPosition ?: 0L, 1.0f, _shuffleEnabled.value, _repeatMode.value)
                        }
                    }
                }
            }

            override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
                 // EAGER SAVE: Save state on Seek/Skip
                 viewModelScope.launch {
                    _currentSong.value?.let { song ->
                        dataStore.savePlaybackState(song.uri, mediaController?.currentPosition ?: 0L, 1.0f, _shuffleEnabled.value, _repeatMode.value)
                    }
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaItem?.let {
                    // Find song by URI
                    val songUri = it.localConfiguration?.uri.toString()
                    val foundSong = _songList.value.find { song -> song.uri == songUri }
                    
                    if (foundSong != null) {
                        _currentSong.value = foundSong
                    } else if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
                         // Only create temp song if we really have to
                    }

                    // Save to DataStore
                    viewModelScope.launch {
                        dataStore.saveLastSong(songUri)
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _duration.value = mediaController?.duration ?: 0L
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _shuffleEnabled.value = shuffleModeEnabled
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _repeatMode.value = repeatMode
            }
        })
    }

    /**
     * Load songs from device storage
     */
    fun loadLocalSongs() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // OPTIMIZATION: Move heavy loading to IO thread
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    // Get filter settings
                    val filterShortAudio = dataStore.filterShortAudio.first()
                    val filterCallRecordings = dataStore.filterCallRecordings.first()
                    
                    val songs = musicRepository.getAllSongs(filterShortAudio, filterCallRecordings)
                    _songList.value = songs

                    // Load categories
                    _albums.value = musicRepository.getAllAlbums()
                    _artists.value = musicRepository.getAllArtists()
                    _folders.value = musicRepository.getAllFolders()
                }

                // CRITICAL FIX: Do NOT force prepare. Use false.
                // If we force true, we overwrite the restored state (position) with 0!
                preparePlaylistIfReady(force = false)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Rescan device for new music
     */
    fun rescanDevice() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // OPTIMIZATION: Move heavy loading to IO thread
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val filterShortAudio = dataStore.filterShortAudio.first()
                    val filterCallRecordings = dataStore.filterCallRecordings.first()
                    musicRepository.rescanDevice(filterShortAudio, filterCallRecordings)
                    val songs = musicRepository.songs.value
                    _songList.value = songs // Correct property
                    
                    // Reload categories
                    _albums.value = musicRepository.getAllAlbums()
                    _artists.value = musicRepository.getAllArtists()
                    _folders.value = musicRepository.getAllFolders()
                }
                // Reset sync flag
                _hasNewSongs.value = false
                
                // Rescan should validly update the list but NOT reset playback if already running
                preparePlaylistIfReady(force = false)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Restore playback state from DataStore
     */
    /**
     * Restore playback state from DataStore
     * ROBUSTNESS FIX: Waits for song list to load before attempting restore
     */
    private fun restorePlaybackState() {
        viewModelScope.launch {
            try {
                // Check if user already acted or service is playing
                if ((mediaController?.mediaItemCount ?: 0) > 0) {
                     isRestoring = false
                     return@launch
                }

                val state = dataStore.playbackState.first()
                if (state.lastSongUri.isNotEmpty()) {
                    
                    // WAIT for songs to load (up to 5 seconds)
                    var retries = 0
                    while (_songList.value.isEmpty() && retries < 25) {
                         delay(200)
                         retries++
                    }

                    val song = _songList.value.find { it.uri == state.lastSongUri }
                    if (song != null) {
                        _currentSong.value = song
                        android.util.Log.d("MusicViewModel", "✓ Restored state: ${song.title} at ${state.lastPosition}ms")
                        
                        // Prepare but don't play
                        mediaController?.let { controller ->
                            val index = _songList.value.indexOfFirst { it.uri == state.lastSongUri }
                            if (index >= 0) {
                                val mediaItems = _songList.value.map { s ->
                                    MediaItem.Builder()
                                        .setUri(s.uri)
                                        .setMediaId(s.id.toString())
                                        .build()
                                }
                                controller.setMediaItems(mediaItems, index, state.lastPosition)
                                controller.prepare()
                                controller.pause() // Ensure it starts PAUSED
                                
                                playlistPrepared = true
                            }
                        }
                    } else {
                        android.util.Log.w("MusicViewModel", "❌ Could not restore song: ${state.lastSongUri} (Not found in ${_songList.value.size} songs)")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                // UNLOCK: Allow other modifiers now
                isRestoring = false
                // If restore failed or list empty, ensure we at least prepare the list
                if (!playlistPrepared) {
                    preparePlaylistIfReady(false)
                }
            }
        }
    }

    /**
     * Build playlist and prepare if controller is ready
     */
    private fun preparePlaylistIfReady(force: Boolean = false) {
        val controller = mediaController ?: return
        val songs = _songList.value
        if (songs.isEmpty()) return

        // GOLD: If restoration is in progress, DO NOT TOUCH THE PLAYER
        if (isRestoring && !force) return

        // FIX: If controller already has items (synced from service), DO NOT RESET
        if (!force && controller.mediaItemCount > 0) {
            playlistPrepared = true
            return
        }
        
        // If we are restored, we might have set items already. Check if we need to force update.
        if (playlistPrepared && !force) return

        val mediaItems = songs.map { song ->
            MediaItem.Builder()
                .setUri(song.uri)
                .setMediaId(song.id.toString())
                .build()
        }

        // Only set items if we are NOT currently playing or if forced (e.g., refresh)
        if (!controller.isPlaying || force) {
             // If we have a current song (restored), try to find its index to avoid resetting to 0
             val currentIndex = if (_currentSong.value != null) {
                 songs.indexOfFirst { it.uri == _currentSong.value?.uri }
             } else 0
             
             val startPos = if (_currentSong.value != null) _progress.value else 0L

            if (currentIndex >= 0) {
                 controller.setMediaItems(mediaItems, currentIndex, startPos)
            } else {
                 controller.setMediaItems(mediaItems)
            }
        }
        
        controller.prepare()
        playlistPrepared = true
    }

    /**
     * Play a specific song by URI
     */
    fun playSong(uri: String) {
        if (!isControllerReady || _songList.value.isEmpty()) {
            pendingPlayUri = uri
            return
        }
        playSongInternal(uri)
    }

    private fun playSongInternal(uri: String) {
        val controller = mediaController ?: return
        val songIndex = _songList.value.indexOfFirst { it.uri == uri }
        if (songIndex >= 0) {
            val mediaItems = _songList.value.map { song ->
                MediaItem.Builder()
                    .setUri(song.uri)
                    .setMediaId(song.id.toString())
                    .build()
            }
            controller.setMediaItems(mediaItems, songIndex, 0L)
            controller.prepare()
            controller.play()
            playlistPrepared = true
            pendingPlayUri = null
            _currentSong.value = _songList.value[songIndex]

            // Clear A-B loop when new song starts
            clearLoop()
            
            // LAYER 2: ELEPHANT MEMORY - Save immediately
             viewModelScope.launch {
                dataStore.savePlaybackState(
                    songUri = uri,
                    position = 0L,
                    shuffleEnabled = _shuffleEnabled.value,
                    repeatMode = _repeatMode.value
                )
             }
        }
    }

    /**
     * Play song by index
     */
    fun playSongAtIndex(index: Int) {
        if (index >= 0 && index < _songList.value.size) {
            val uri = _songList.value[index].uri
            playSong(uri)
        }
    }

    /**
     * Toggle play/pause
     */
    fun togglePlayPause() {
        mediaController?.let { controller ->
            if (_isPlaying.value) {
                controller.pause()
            } else {
                controller.play()
            }
            // LAYER 2: ELEPHANT MEMORY - Save state immediately
            viewModelScope.launch {
                _currentSong.value?.let { song ->
                    dataStore.savePlaybackState(
                        songUri = song.uri,
                        position = controller.currentPosition,
                        shuffleEnabled = _shuffleEnabled.value,
                        repeatMode = _repeatMode.value
                    )
                }
            }
        }
    }

    /**
     * Seek to position (in milliseconds)
     */
    fun seekTo(position: Long) {
        mediaController?.seekTo(position)
        _progress.value = position
        // LAYER 2: ELEPHANT MEMORY - Save position immediately
        viewModelScope.launch {
             dataStore.saveLastPosition(position)
        }
    }

    /**
     * Play next song
     */
    fun playNext() {
        mediaController?.seekToNext()
    }

    /**
     * Play previous song
     */
    fun playPrevious() {
        mediaController?.seekToPrevious()
    }

    /**
     * Toggle shuffle mode
     */
    fun toggleShuffle() {
        mediaController?.let { controller ->
            val newValue = !controller.shuffleModeEnabled
            controller.shuffleModeEnabled = newValue
            _shuffleEnabled.value = newValue
        }
    }

    /**
     * Cycle repeat mode: OFF -> ONE -> ALL -> OFF
     */
    fun cycleRepeatMode() {
        mediaController?.let { controller ->
            val next = when (controller.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
                Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
                else -> Player.REPEAT_MODE_OFF
            }
            controller.repeatMode = next
            _repeatMode.value = next
        }
    }

    // ====== A-B Loop Functions ======

    /**
     * Toggle A-B Loop
     * Click 1: Set Point A
     * Click 2: Set Point B (activate loop)
     * Click 3: Clear loop
     */
    fun toggleABLoop() {
        when {
            _loopStart.value == null -> {
                // Set Point A
                _loopStart.value = _progress.value
            }
            _loopEnd.value == null -> {
                // Set Point B and activate
                val currentPos = _progress.value
                if (currentPos > (_loopStart.value ?: 0L)) {
                    _loopEnd.value = currentPos
                    _loopEnabled.value = true
                }
            }
            else -> {
                // Clear loop
                clearLoop()
            }
        }
    }

    /**
     * Clear A-B loop
     */
    fun clearLoop() {
        _loopStart.value = null
        _loopEnd.value = null
        _loopEnabled.value = false
    }

    // ====== Category Functions ======

    /**
     * Get songs by folder path
     */
    fun getSongsByFolder(folderPath: String): List<Song> {
        return musicRepository.getSongsByFolder(folderPath)
    }

    /**
     * Get songs by album name
     */
    fun getSongsByAlbum(albumName: String): List<Song> {
        return musicRepository.getSongsByAlbum(albumName)
    }

    /**
     * Get songs by artist name
     */
    fun getSongsByArtist(artistName: String): List<Song> {
        return musicRepository.getSongsByArtist(artistName)
    }

    // ====== Favorites (Database-Backed) ======

    /**
     * Load favorites from database (called in init)
     * Now automatic via Flow
     */
    private fun loadFavorites() {
        // Favorites now come from database via allFavorites Flow
        // No need to manually load
    }

    /**
     * Check if a song is favorited by ID
     * Returns a Flow for reactive updates
     */
    fun isFavoritedById(songId: Long) = favoritesRepository.isFavoritedById(songId)

    /**
     * Check if a song is favorited by URI
     * Returns a Flow for reactive updates
     */
    fun isFavoritedByUri(uri: String) = favoritesRepository.isFavorited(uri)

    /**
     * Toggle favorite status of a song
     * Automatically persists to database
     */
    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            val favorite = FavoriteSong(
                id = song.id,
                title = song.title,
                artist = song.artist,
                uri = song.uri,
                albumArtUri = song.albumArtUri
            )
            // Check if already favorited
            val isFav = favoritesRepository.isFavorited(song.uri).first()
            if (isFav) {
                // Remove from favorites
                favoritesRepository.removeFavoriteByUri(song.uri)
            } else {
                // Add to favorites
                favoritesRepository.addFavorite(favorite)
            }
        }
    }

    /**
     * Add a specific song to favorites
     */
    fun addFavorite(song: Song) {
        viewModelScope.launch {
            val favorite = FavoriteSong(
                id = song.id,
                title = song.title,
                artist = song.artist,
                uri = song.uri,
                albumArtUri = song.albumArtUri
            )
            favoritesRepository.addFavorite(favorite)
        }
    }

    /**
     * Remove a song from favorites by URI
     */
    fun removeFavorite(uri: String) {
        viewModelScope.launch {
            favoritesRepository.removeFavoriteByUri(uri)
        }
    }

    /**
     * Start updating progress every second
     */
    private fun startProgressUpdates() {
        progressUpdateJob?.cancel()
        progressUpdateJob = viewModelScope.launch {
            while (true) {
                mediaController?.let {
                    _progress.value = it.currentPosition
                    _duration.value = it.duration

                    // Check A-B loop
                    if (_loopEnabled.value) {
                        val end = _loopEnd.value
                        val start = _loopStart.value
                        if (end != null && start != null && it.currentPosition >= end) {
                            it.seekTo(start)
                        }
                    }

                    // REMOVED: High-frequency DataStore save to prevent audio stutter ("gag")
                    // We now strictly save on Pause, Stop, and Song Change.
                }
                delay(500L) // Update every 500ms for smoother progress
            }
        }
    }

    /**
     * Stop progress updates
     */
    private fun stopProgressUpdates() {
        progressUpdateJob?.cancel()
    }

    // ====== Settings / Filter ======

    /**
     * Observe filter state
     */
    val isShortAudioHidden: StateFlow<Boolean> = dataStore.filterShortAudio
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    /**
     * Toggle "Smart Filter" (Hide audio < 30s)
     */
    fun toggleShortAudioFilter(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.setFilterShortAudio(enabled)
            // FAST UPDATE: Just reload from DB with new filter
            loadLocalSongs()
        }
    }

    /**
     * Observe call recording filter state
     */
    val isCallRecordingHidden: StateFlow<Boolean> = dataStore.filterCallRecordings
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    /**
     * Toggle "Hide Call Recordings"
     */
    fun toggleCallRecordingFilter(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.setFilterCallRecordings(enabled)
            // FAST UPDATE: Just reload from DB with new filter
            loadLocalSongs()
        }
    }

    /**
     * Move item in queue (Reorder)
     */
    /**
     * Move item in queue (Reorder)
     */
    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        mediaController?.moveMediaItem(fromIndex, toIndex)
    }

    /**
     * Remove item from queue
     */
    fun removeFromQueue(index: Int) {
        mediaController?.removeMediaItem(index)
    }

    /**
     * Release MediaController on ViewModel clear
     */
    override fun onCleared() {
        super.onCleared()
        stopProgressUpdates()
        // Save final state
        viewModelScope.launch {
            _currentSong.value?.let { song ->
                dataStore.savePlaybackState(
                    songUri = song.uri,
                    position = _progress.value,
                    shuffleEnabled = _shuffleEnabled.value,
                    repeatMode = _repeatMode.value
                )
            }
        }
        mediaController?.release() // Changed from MediaController.releaseFuture
    }
}
