package com.eplaytime.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.eplaytime.app.R
import com.eplaytime.app.data.model.Song
import com.eplaytime.app.data.repository.Album
import com.eplaytime.app.data.repository.Artist
import com.eplaytime.app.data.repository.MusicFolder
import com.eplaytime.app.ui.components.MiniPlayer
import com.eplaytime.app.ui.components.SongList
import com.eplaytime.app.ui.theme.*
import com.eplaytime.app.ui.viewmodel.MusicViewModel
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * HomeScreen - Flagship UI with Scrollable Tabs and Glass Header
 * Features:
 * - True OLED Black background
 * - Outfit font throughout
 * - ScrollableTabRow for categories
 * - Glass header with greeting
 * - Scan/Refresh button
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun HomeScreen(
    viewModel: MusicViewModel,
    onNavigateToScheduler: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    onNavigateToAbout: () -> Unit = {},
    onNavigateToQueue: () -> Unit = {},
    onNavigateToFolder: (String) -> Unit = {}
) {
    val context = LocalContext.current
    
    // Permission handling
    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            viewModel.rescanDevice()
        }
    }
    
    // Request permission on first launch
    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(audioPermission)
        } else {
            // Already have permission, load songs
            viewModel.loadLocalSongs()
        }
    }
    
    // If no permission, show permission request UI
    if (!hasPermission) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BlackBackground),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = SoftGold,
                    modifier = Modifier.size(72.dp)
                )
                Text(
                    text = "Permission Required",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = TextPrimary
                )
                Text(
                    text = "PlayTime needs access to your music files to play them.",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Light,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { permissionLauncher.launch(audioPermission) },
                    colors = ButtonDefaults.buttonColors(containerColor = SoftGold)
                ) {
                    Text(
                        text = "Grant Permission",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                }
            }
        }
        return
    }
    
    val songList by viewModel.songList.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val allFavorites by viewModel.allFavorites.collectAsState(initial = emptyList())
    val isShortAudioHidden by viewModel.isShortAudioHidden.collectAsState()
    val isCallRecordingHidden by viewModel.isCallRecordingHidden.collectAsState()
    val hasNewSongs by viewModel.hasNewSongs.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }

    var searchQuery by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    var selectedTabIndex by rememberSaveable { mutableStateOf(0) }

    val tabs = listOf("Tracks", "Folders", "Albums", "Artists", "Favorites")

    val filteredSongs = remember(songList, searchQuery.text, selectedTabIndex, allFavorites) {
        val query = searchQuery.text.trim().lowercase(Locale.US)
        val baseList = when (selectedTabIndex) {
            4 -> songList.filter { song -> allFavorites.any { it.uri == song.uri } } // Favorites from DB
            else -> songList
        }
        if (query.isEmpty()) baseList
        else baseList.filter { song ->
            song.title.lowercase(Locale.US).contains(query) ||
                song.artist.lowercase(Locale.US).contains(query) ||
                song.album.lowercase(Locale.US).contains(query)
        }
    }

    Scaffold(
        containerColor = BlackBackground,
        contentWindowInsets = WindowInsets.systemBars,
        bottomBar = {
            // MiniPlayer with safe area - OUTSIDE content to prevent recreation on tab change
            Column {
                if (isPlaying || currentSong != null) {
                    currentSong?.let { song ->
                        MiniPlayer(
                            song = song,
                            isPlaying = isPlaying,
                            onPlayPauseClick = { viewModel.togglePlayPause() },
                            onSongClick = onNavigateToPlayer,
                            onNextClick = { viewModel.playNext() },
                            onPreviousClick = { viewModel.playPrevious() },
                            viewModel = viewModel
                        )
                    }
                }
                // Navigation bar spacer
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsBottomHeight(WindowInsets.navigationBars)
                        .background(BlackBackground)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            // Glass Header with Greeting
            GlassHeader(
                onSchedulerClick = onNavigateToScheduler,
                onAboutClick = onNavigateToAbout,
                onSettingsClick = { showSettingsDialog = true },
                onRefreshClick = { viewModel.rescanDevice() },
                isLoading = isLoading,
                hasNewSongs = hasNewSongs
            )

            // ... (Search Bar code) ...

            // Settings Dialog
            if (showSettingsDialog) {
                com.eplaytime.app.ui.components.SettingsDialog(
                    showDialog = showSettingsDialog,
                    onDismiss = { showSettingsDialog = false },
                    currentFilterState = isShortAudioHidden,
                    onToggleFilter = { viewModel.toggleShortAudioFilter(it) },
                    currentCallFilterState = isCallRecordingHidden,
                    onToggleCallFilter = { viewModel.toggleCallRecordingFilter(it) },
                    onTermsClick = onNavigateToAbout, // Navigate to About (where Terms are located)
                    onAboutClick = onNavigateToAbout
                )
            }


            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = {
                    Text(
                        "Search songs, artists, albums",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Light
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextSecondary
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SoftGold,
                    unfocusedBorderColor = GlassBorder,
                    cursorColor = SoftGold,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = BlackSurface,
                    unfocusedContainerColor = BlackSurface
                )
            )

            // Scrollable Tab Row (CRITICAL: Must scroll horizontally)
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = SoftGold,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        Box(
                            modifier = Modifier
                                .tabIndicatorOffset(tabPositions[selectedTabIndex])
                                .height(3.dp)
                                .background(SoftGold, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        )
                    }
                },
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontFamily = OutfitFontFamily,
                                fontWeight = if (selectedTabIndex == index) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 14.sp,
                                color = if (selectedTabIndex == index) SoftGold else TextSecondary,
                                maxLines = 1
                            )
                        }
                    )
                }
            }
            
            // Hoist list states to preserve scroll position
            val tracksListState = rememberSaveable(saver = androidx.compose.foundation.lazy.LazyListState.Saver) {
                androidx.compose.foundation.lazy.LazyListState()
            }
            val foldersListState = rememberSaveable(saver = androidx.compose.foundation.lazy.LazyListState.Saver) {
                androidx.compose.foundation.lazy.LazyListState()
            }
            val albumsListState = rememberSaveable(saver = androidx.compose.foundation.lazy.LazyListState.Saver) {
                androidx.compose.foundation.lazy.LazyListState()
            }
            val artistsListState = rememberSaveable(saver = androidx.compose.foundation.lazy.LazyListState.Saver) {
                androidx.compose.foundation.lazy.LazyListState()
            }
            val favoritesListState = rememberSaveable(saver = androidx.compose.foundation.lazy.LazyListState.Saver) {
                androidx.compose.foundation.lazy.LazyListState()
            }

            // Content based on selected tab - using AnimatedContent instead of Crossfade
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = selectedTabIndex,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(200)) with
                        fadeOut(animationSpec = tween(200))
                    },
                    label = "tab_content"
                ) { tabIndex ->
                    when (tabIndex) {
                        0 -> {
                            // Tracks
                            if (isLoading) LoadingState()
                            else if (filteredSongs.isEmpty()) EmptyState()
                            else SongList(
                                songs = filteredSongs,
                                currentSongId = currentSong?.id,
                                onSongClick = { song -> viewModel.playSong(song.uri) },
                                state = tracksListState
                            )
                        }
                        1 -> {
                            // Folders
                            if (folders.isEmpty()) EmptyState("No folders found")
                            else FolderList(
                                folders = folders,
                                onFolderClick = { folder ->
                                    onNavigateToFolder(folder.path)
                                },
                                state = foldersListState
                            )
                        }
                        2 -> {
                            // Albums
                            if (albums.isEmpty()) EmptyState("No albums found")
                            else AlbumList(
                                albums = albums,
                                onAlbumClick = { album ->
                                    val songs = viewModel.getSongsByAlbum(album.name)
                                    songs.firstOrNull()?.let { viewModel.playSong(it.uri) }
                                },
                                state = albumsListState
                            )
                        }
                        3 -> {
                            // Artists
                            if (artists.isEmpty()) EmptyState("No artists found")
                            else ArtistList(
                                artists = artists,
                                onArtistClick = { artist ->
                                    val songs = viewModel.getSongsByArtist(artist.name)
                                    songs.firstOrNull()?.let { viewModel.playSong(it.uri) }
                                },
                                state = artistsListState
                            )
                        }
                        4 -> {
                            // Favorites
                            if (filteredSongs.isEmpty()) EmptyState("No favorites yet")
                            else SongList(
                                songs = filteredSongs,
                                currentSongId = currentSong?.id,
                                onSongClick = { song -> viewModel.playSong(song.uri) },
                                state = favoritesListState
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassHeader(
    onSchedulerClick: () -> Unit,
    onAboutClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onRefreshClick: () -> Unit,
    isLoading: Boolean,
    hasNewSongs: Boolean // New param
) {
    // ... (greeting logic) ...
    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good Morning"
            in 12..17 -> "Good Afternoon"
            in 18..21 -> "Good Evening"
            else -> "Good Night"
        }
    }

    val haptic = LocalHapticFeedback.current
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Pulse animation for New Songs
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (hasNewSongs) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Rotation animation for refresh icon
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        BlackSurface.copy(alpha = 0.95f),
                        Color.Transparent
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = greeting,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Light,
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Text(
                    text = "PlayTime",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = TextPrimary
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Refresh/Scan Button with Haptic Feedback + Pulse
                IconButton(
                    onClick = {
                        if (!isRefreshing && !isLoading) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isRefreshing = true
                            onRefreshClick()
                            // Reset after animation
                            scope.launch {
                                kotlinx.coroutines.delay(1500)
                                isRefreshing = false
                            }
                        }
                    },
                    modifier = Modifier.scale(if (hasNewSongs) pulseScale else 1.1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Scan",
                        tint = if (hasNewSongs) Color.Red else if (isRefreshing || isLoading) SoftGold else TextSecondary,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer { rotationZ = if (isRefreshing || isLoading) rotation else 0f }
                    )
                }

                // Settings Button (Gear)
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Scheduler Button
                IconButton(onClick = onSchedulerClick) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = "Scheduler",
                        tint = SoftGold,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}



@Composable
private fun FolderList(
    folders: List<MusicFolder>,
    onFolderClick: (MusicFolder) -> Unit,
    state: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = state,
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items = folders, key = { it.path }) { folder ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BlackSurface)
                    .clickable { onFolderClick(folder) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = SoftGold,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = folder.name,
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "${folder.songCount} songs",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Light,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumList(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit,
    state: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = state,
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items = albums, key = { it.id }) { album ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BlackSurface)
                    .clickable { onAlbumClick(album) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = album.albumArtUri ?: R.drawable.logo,
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BlackCard),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = album.name,
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${album.artist} • ${album.songCount} songs",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Light,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistList(
    artists: List<Artist>,
    onArtistClick: (Artist) -> Unit,
    state: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = state,
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items = artists, key = { it.id }) { artist ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BlackSurface)
                    .clickable { onArtistClick(artist) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SoftGold.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = artist.name.first().uppercase(),
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = SoftGold
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = artist.name,
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "${artist.songCount} tracks",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Light,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = SoftGold,
                strokeWidth = 3.dp
            )
            Text(
                text = "Scanning music library...",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Light,
                fontSize = 16.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun EmptyState(message: String = "No music found") {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MusicOff,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = message,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                color = TextPrimary
            )
            Text(
                text = "Add music to your device\nand tap refresh to scan",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Light,
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

private fun formatDuration(millis: Long): String {
    val seconds = (millis / 1000).toInt()
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, remainingSeconds)
}
