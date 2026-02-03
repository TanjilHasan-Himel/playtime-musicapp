package com.eplaytime.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.eplaytime.app.data.model.Song
import com.eplaytime.app.data.repository.MusicFolder
import com.eplaytime.app.ui.components.SongList
import com.eplaytime.app.ui.theme.BlackBackground
import com.eplaytime.app.ui.theme.OutfitFontFamily
import com.eplaytime.app.ui.theme.SoftGold
import com.eplaytime.app.ui.theme.TextPrimary
import com.eplaytime.app.ui.theme.TextSecondary
import com.eplaytime.app.ui.viewmodel.MusicViewModel
import androidx.compose.animation.core.tween

/**
 * FolderScreen - Browse music organized by folders
 * Single Source of Truth: Folders are scanned from MediaStore
 * User clicks folder → sees songs in that folder → clicks song to play
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun FolderScreen(
    onNavigateBack: () -> Unit,
    viewModel: MusicViewModel,
    initialFolderPath: String? = null
) {
    val folders by viewModel.folders.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()

    var selectedFolder by remember { mutableStateOf<MusicFolder?>(null) }
    var folderSongs by remember { mutableStateOf<List<Song>>(emptyList()) }

    // Handle deep link
    LaunchedEffect(initialFolderPath, folders) {
        if (initialFolderPath != null && folders.isNotEmpty() && selectedFolder == null) {
            val match = folders.find { it.path == initialFolderPath }
            if (match != null) {
                selectedFolder = match
                folderSongs = viewModel.getSongsByFolder(match.path)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = selectedFolder?.name ?: "Folders",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedFolder != null) {
                            selectedFolder = null
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = BlackBackground,
        contentWindowInsets = WindowInsets.systemBars
    ) { paddingValues ->
        AnimatedContent(
            targetState = selectedFolder,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) with fadeOut(animationSpec = tween(300))
            },
            label = "folder_content"
        ) { folder ->
            if (folder == null) {
                // Folder list view
                if (folders.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No folders found",
                            fontFamily = OutfitFontFamily,
                            color = TextSecondary,
                            fontSize = 16.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(folders) { folderItem ->
                            FolderItem(
                                folder = folderItem,
                                onClick = {
                                    selectedFolder = folderItem
                                    folderSongs = viewModel.getSongsByFolder(folderItem.path)
                                }
                            )
                        }
                    }
                }
            } else {
                // Songs in folder view
                SongList(
                    songs = folderSongs,
                    currentSongId = currentSong?.id,
                    onSongClick = { song ->
                        viewModel.playSong(song.uri)
                    },
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp, 
                        end = 16.dp, 
                        top = 16.dp, 
                        bottom = 16.dp
                    )
                )
            }
        }
    }
}

@Composable
private fun FolderItem(
    folder: MusicFolder,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                tint = SoftGold,
                modifier = Modifier.size(40.dp)
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = folder.name,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${folder.songCount} songs",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Light,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}
