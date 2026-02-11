package com.eplaytime.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import coil.compose.AsyncImage
import com.eplaytime.app.R
import com.eplaytime.app.data.model.Song
import com.eplaytime.app.ui.theme.*
import java.util.Locale
import coil.request.ImageRequest
import coil.request.CachePolicy

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SongList(
    songs: List<Song>,
    currentSongId: Long?,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    state: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = state,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // stickyHeader for "All Songs (Count)" and Shuffle button
            stickyHeader {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    BlackBackground.copy(alpha = 0.95f),
                                    BlackBackground.copy(alpha = 0.8f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "All Songs (${songs.size})",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )

                        // Shuffle Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable {
                                    if (songs.isNotEmpty()) {
                                        // Trigger shuffle play (logic handled by parent usually, 
                                        // check if we need to expose a callback or just play random)
                                        // For now, simpler: user clicks logic.
                                        // Ideally, pass onShuffleClick up. 
                                        // Assuming first song + shuffle logic is enough for now or 
                                        // just simple play.
                                        // Let's just pick a random song to ensure shuffle-like start if no shuffle param
                                        if (songs.isNotEmpty()) {
                                            onSongClick(songs.random())
                                        }
                                    }
                                }
                                .background(SoftGold.copy(alpha = 0.2f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Shuffle",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = SoftGold
                            )
                        }
                    }
                }
            }

            items(items = songs, key = { it.id }, contentType = { "song" }) { song ->
                val itemModifier = Modifier.graphicsLayer {
                    val itemInfo = state.layoutInfo.visibleItemsInfo.find { it.key == song.id }
                    if (itemInfo != null) {
                        val offset = itemInfo.offset
                        // Start transforming when item is near the top (e.g., top 300px)
                        if (offset < 300) {
                            val normalized = (offset / 300f).coerceIn(0f, 1f)
                            scaleX = 0.9f + (0.1f * normalized)
                            scaleY = 0.9f + (0.1f * normalized)
                            alpha = normalized
                        } else {
                            scaleX = 1f
                            scaleY = 1f
                            alpha = 1f
                        }
                    }
                }

                SongListItem(
                    song = song,
                    isCurrentlyPlaying = song.id == currentSongId,
                    onClick = { onSongClick(song) },
                    modifier = itemModifier
                )
            }
        }
        
        // Custom Fast Scrollbar
        VerticalFastScroller(
            listState = state,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

// ... (existing content)

@Composable
fun SongListItem(
    song: Song,
    isCurrentlyPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
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
        // Round Album Art
        AsyncImage(
            model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                .data(song.albumArtUri ?: R.drawable.logo)
                .size(128, 128) // CRITICAL: Downsample
                .crossfade(true)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build(),
            contentDescription = "Album Art",
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(BlackCard),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                fontFamily = OutfitFontFamily,
                fontWeight = if (isCurrentlyPlaying) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = 16.sp,
                color = if (isCurrentlyPlaying) SoftGold else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${song.artist} • ${song.album}",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Light,
                fontSize = 13.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = formatDuration(song.duration),
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Light,
            fontSize = 12.sp,
            color = TextTertiary
        )
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}
