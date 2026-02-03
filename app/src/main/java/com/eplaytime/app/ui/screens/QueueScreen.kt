package com.eplaytime.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.eplaytime.app.R
import com.eplaytime.app.data.model.Song
import com.eplaytime.app.ui.theme.*
import com.eplaytime.app.ui.viewmodel.MusicViewModel
import java.util.Locale

/**
 * QueueScreen - Shows current playback queue
 * Rich & Soft aesthetic with Outfit font
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    onNavigateBack: () -> Unit,
    viewModel: MusicViewModel
) {
    val songList by viewModel.songList.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Queue",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
        if (songList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Queue is empty",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Light,
                    color = TextSecondary,
                    fontSize = 16.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Now Playing Header
                currentSong?.let { song ->
                    item {
                        Text(
                            text = "NOW PLAYING",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Light,
                            fontSize = 12.sp,
                            color = SoftGold,
                            letterSpacing = 2.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        QueueItem(
                            song = song,
                            isCurrentlyPlaying = true,
                            onClick = { }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "UP NEXT",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Light,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            letterSpacing = 2.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }

                // Queue items
                val currentIndex = songList.indexOfFirst { it.id == currentSong?.id }
                val upNext = if (currentIndex >= 0 && currentIndex < songList.size - 1) {
                    songList.subList(currentIndex + 1, songList.size)
                } else {
                    songList
                }

                itemsIndexed(upNext, key = { _, song -> song.id }) { index, song ->
                    // Calculate actual index in the full list
                    // upNext is subList(currentIndex + 1, size)
                    // So actual index = currentIndex + 1 + index
                    val actualIndex = currentIndex + 1 + index
                    
                    QueueItem(
                        song = song,
                        isCurrentlyPlaying = false,
                        onMoveUp = {
                            if (actualIndex > currentIndex + 1) { // Can't move above "Now Playing" + 1
                                viewModel.moveQueueItem(actualIndex, actualIndex - 1)
                            }
                        },
                        onMoveDown = {
                            if (actualIndex < songList.size - 1) {
                                viewModel.moveQueueItem(actualIndex, actualIndex + 1)
                            }
                        },
                        onRemove = {
                            viewModel.removeFromQueue(actualIndex)
                        },
                        onClick = { viewModel.playSong(song.uri) }
                    )
                }

                // Bottom spacer for nav bar
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun QueueItem(
    song: Song,
    isCurrentlyPlaying: Boolean,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isCurrentlyPlaying) {
                    Brush.horizontalGradient(
                        colors = listOf(SoftGold.copy(alpha = 0.15f), BlackSurface)
                    )
                } else {
                    Brush.horizontalGradient(listOf(BlackSurface, BlackSurface))
                }
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ... (Image and Text - same as before) ...
        // Album Art
        AsyncImage(
            model = song.albumArtUri ?: R.drawable.logo,
            contentDescription = "Album Art",
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(BlackCard),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Song Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                fontFamily = OutfitFontFamily,
                fontWeight = if (isCurrentlyPlaying) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = 14.sp,
                color = if (isCurrentlyPlaying) SoftGold else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Light,
                fontSize = 12.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Duration (Hide if controls are present to save space, or keep it?)
        // Let's keep it but maybe smaller padding
        Text(
            text = formatDuration(song.duration),
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Light,
            fontSize = 12.sp,
            color = TextTertiary
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Reorder Controls (Only for Up Next items)
        if (onMoveUp != null && onMoveDown != null && onRemove != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Move Up
                IconButton(
                    onClick = onMoveUp,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Move Up",
                        tint = TextSecondary
                    )
                }
                
                // Move Down
                IconButton(
                    onClick = onMoveDown,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Move Down",
                        tint = TextSecondary
                    )
                }
                
                // Delete
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = TextSecondary
                    )
                }
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val seconds = (millis / 1000).toInt()
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, remainingSeconds)
}
