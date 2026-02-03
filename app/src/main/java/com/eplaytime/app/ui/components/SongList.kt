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
import coil.compose.AsyncImage
import com.eplaytime.app.R
import com.eplaytime.app.data.model.Song
import com.eplaytime.app.ui.theme.*
import java.util.Locale

@Composable
fun SongList(
    songs: List<Song>,
    currentSongId: Long?,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    state: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
) {
    Box(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = state,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items = songs, key = { it.id }) { song ->
                SongListItem(
                    song = song,
                    isCurrentlyPlaying = song.id == currentSongId,
                    onClick = { onSongClick(song) }
                )
            }
        }
        
        // Custom Scrollbar
        VerticalScrollbar(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(),
            listState = state
        )
    }
}

@Composable
fun VerticalScrollbar(
    modifier: Modifier = Modifier,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    val layoutInfo = listState.layoutInfo
    val totalItems = layoutInfo.totalItemsCount
    val visibleItemsInfo = layoutInfo.visibleItemsInfo
    
    if (totalItems == 0 || visibleItemsInfo.isEmpty()) return
    
    val firstVisibleItemIndex = listState.firstVisibleItemIndex
    val viewportHeight = layoutInfo.viewportSize.height
    
    // Estimate total content height: (viewportHeight / visibleItemsCount) * totalItems
    // This assumes roughly equal item height, which is fine for visual estimation
    val visibleItemsCount = visibleItemsInfo.size
    val averageItemHeight = if (visibleItemsCount > 0) viewportHeight / visibleItemsCount else 0
    val estimatedContentHeight = averageItemHeight * totalItems
    
    // Calculate scrollbar thumb size and offset
    val thumbHeight = (viewportHeight.toFloat() / estimatedContentHeight.toFloat() * viewportHeight.toFloat())
        .coerceIn(50f, viewportHeight.toFloat()) // Min height 50px
        
    val scrollOffset = firstVisibleItemIndex * averageItemHeight
    val thumbOffset = (scrollOffset.toFloat() / estimatedContentHeight.toFloat() * viewportHeight.toFloat())
        .coerceIn(0f, viewportHeight.toFloat() - thumbHeight)

    // Draw the scrollbar
    androidx.compose.foundation.Canvas(
        modifier = modifier
            .width(6.dp)
            .padding(vertical = 4.dp, horizontal = 1.dp)
    ) {
        drawRoundRect(
            color = com.eplaytime.app.ui.theme.TextTertiary.copy(alpha = 0.5f),
            topLeft = androidx.compose.ui.geometry.Offset(0f, thumbOffset),
            size = androidx.compose.ui.geometry.Size(size.width, thumbHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
        )
    }
}

@Composable
fun SongListItem(
    song: Song,
    isCurrentlyPlaying: Boolean,
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
        // Round Album Art
        AsyncImage(
            model = song.albumArtUri ?: R.drawable.logo,
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
