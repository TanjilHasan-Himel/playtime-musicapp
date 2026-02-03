package com.eplaytime.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.eplaytime.app.R
import com.eplaytime.app.ui.theme.*
import com.eplaytime.app.ui.viewmodel.MusicViewModel
import java.util.*
import kotlin.math.abs
import kotlin.math.sin

/**
 * PlayerScreen - Full-screen Glass Player with A-B Loop
 * Rich & Soft aesthetic with Outfit font and blurred background
 */
@Composable
fun PlayerScreen(
    onNavigateBack: () -> Unit,
    onNavigateToQueue: () -> Unit = {},
    viewModel: MusicViewModel
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val shuffleEnabled by viewModel.shuffleEnabled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val loopStart by viewModel.loopStart.collectAsState()
    val loopEnd by viewModel.loopEnd.collectAsState()
    val loopEnabled by viewModel.loopEnabled.collectAsState()
    val allFavorites by viewModel.allFavorites.collectAsState(initial = emptyList())

    // Check if current song is favorited
    val isFavorite = currentSong?.let { song ->
        allFavorites.any { it.uri == song.uri }
    } ?: false

    // Redirect back if no song
    if (currentSong == null) {
        SideEffect { onNavigateBack() }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BlackBackground)
    ) {
        // Blurred Background Album Art
        if (currentSong?.albumArtUri != null) {
            AsyncImage(
                model = currentSong!!.albumArtUri,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(50.dp)
                    .alpha(0.6f),
                contentScale = ContentScale.Crop
            )
        }
        
        // Dark Overlay (50% opacity)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text(
                    text = "NOW PLAYING",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Light,
                    fontSize = 12.sp,
                    letterSpacing = 3.sp,
                    color = TextSecondary
                )
                IconButton(onClick = onNavigateToQueue) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = "Queue",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Album Art (Vinyl Style)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "vinyl")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(20000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "vinyl_rotation"
                )

                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                        .border(2.dp, SoftGold.copy(alpha = 0.3f), CircleShape)
                        .graphicsLayer {
                            rotationZ = if (isPlaying) rotation else 0f
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = currentSong?.albumArtUri ?: R.drawable.logo,
                        contentDescription = "Album Art",
                        modifier = Modifier
                            .size(180.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Center hole
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(BlackBackground)
                            .border(1.dp, SoftGold.copy(alpha = 0.5f), CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Song Info
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = currentSong?.title ?: "Unknown Title",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentSong?.artist ?: "Unknown Artist",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Light,
                    fontSize = 16.sp,
                    color = SoftGold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Waveform Visualizer
            WaveformVisualizer(
                isPlaying = isPlaying,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Slider with A-B Loop markers
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = if (duration > 0) progress.toFloat() else 0f,
                        onValueChange = { viewModel.seekTo(it.toLong()) },
                        valueRange = 0f..(duration.toFloat().coerceAtLeast(1f)),
                        colors = SliderDefaults.colors(
                            thumbColor = SoftGold,
                            activeTrackColor = SoftGold,
                            inactiveTrackColor = ProgressBackground
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // A-B Loop markers
                    if (loopStart != null && duration > 0) {
                        val startPercent = (loopStart!! / duration.toFloat())
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(startPercent)
                                .align(Alignment.CenterStart)
                                .padding(start = 16.dp)
                        ) {
                            Text(
                                text = "A",
                                color = SoftGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.CenterEnd)
                            )
                        }
                    }
                    if (loopEnd != null && duration > 0) {
                        val endPercent = (loopEnd!! / duration.toFloat())
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(endPercent)
                                .align(Alignment.CenterStart)
                                .padding(start = 16.dp)
                        ) {
                            Text(
                                text = "B",
                                color = SoftGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.CenterEnd)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDurationFull(progress),
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Light,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "-${formatDurationFull((duration - progress).coerceAtLeast(0L))}",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Light,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Secondary Controls Row (Shuffle, Repeat, A-B Loop, Favorite)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                IconButton(onClick = { viewModel.toggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (shuffleEnabled) SoftGold else TextTertiary
                    )
                }

                // A-B Loop Toggle
                IconButton(onClick = { viewModel.toggleABLoop() }) {
                    val loopText = when {
                        loopEnabled -> "A-B"
                        loopStart != null -> "A"
                        else -> "A-B"
                    }
                    val loopColor = when {
                        loopEnabled -> SoftGold
                        loopStart != null -> SoftGold.copy(alpha = 0.6f)
                        else -> TextTertiary
                    }
                    Text(
                        text = loopText,
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = loopColor
                    )
                }

                // Repeat
                IconButton(onClick = { viewModel.cycleRepeatMode() }) {
                    val repeatIcon = if (repeatMode == Player.REPEAT_MODE_ONE) {
                        Icons.Default.RepeatOne
                    } else {
                        Icons.Default.Repeat
                    }
                    Icon(
                        imageVector = repeatIcon,
                        contentDescription = "Repeat",
                        tint = if (repeatMode != Player.REPEAT_MODE_OFF) SoftGold else TextTertiary
                    )
                }

                // Favorite
                val isFavorite = currentSong?.let { song ->
                    allFavorites.any { it.uri == song.uri }
                } ?: false
                IconButton(onClick = { currentSong?.let { viewModel.toggleFavorite(it) } }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) SoftGold else TextTertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous
                IconButton(
                    onClick = { viewModel.playPrevious() },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Play/Pause (Large, Round, Soft edges)
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(SoftGold, SoftGoldDark)
                            )
                        )
                        .clickable { viewModel.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.Black,
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Next
                IconButton(
                    onClick = { viewModel.playNext() },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun WaveformVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val barCount = 32
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier) {
        val barWidth = size.width / (barCount * 1.8f)
        val gap = barWidth * 0.8f
        val maxHeight = size.height

        for (i in 0 until barCount) {
            val x = i * (barWidth + gap) + gap / 2
            val wave = if (isPlaying) {
                abs(sin(phase + i * 0.3f))
            } else {
                0.15f
            }
            val barHeight = maxHeight * (0.15f + 0.85f * wave)
            drawRoundRect(
                color = SoftGold.copy(alpha = 0.8f),
                topLeft = Offset(x, maxHeight - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}

private fun formatDurationFull(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}
