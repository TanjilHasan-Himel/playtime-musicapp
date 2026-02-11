package com.eplaytime.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import com.eplaytime.app.ui.theme.SoftGold

@Composable
fun VerticalFastScroller(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    thumbColor: Color = SoftGold,
    trackColor: Color = Color.Gray.copy(alpha = 0.3f),
    thumbHeightMin: Float = 100f
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    var isDragging by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    val layoutInfo = listState.layoutInfo
    val totalItemsCount = layoutInfo.totalItemsCount
    val visibleItemsInfo = layoutInfo.visibleItemsInfo
    val viewportHeight = layoutInfo.viewportSize.height.toFloat()

    if (totalItemsCount == 0 || visibleItemsInfo.isEmpty()) return

    val firstVisibleItemIndex = listState.firstVisibleItemIndex
    val firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset

    // Estimate total content height
    val estimatedItemHeight = if (visibleItemsInfo.isNotEmpty()) {
        visibleItemsInfo.sumOf { it.size } / visibleItemsInfo.size.toFloat()
    } else {
        0f
    }
    val estimatedContentHeight = estimatedItemHeight * totalItemsCount
    
    // Calculate scroll progress (0..1)
    val maxScrollOffset = max(0f, estimatedContentHeight - viewportHeight)
    val currentScrollOffset = (firstVisibleItemIndex * estimatedItemHeight) + firstVisibleItemScrollOffset
    
    val scrollProgress = if (maxScrollOffset > 0) {
        (currentScrollOffset / maxScrollOffset).coerceIn(0f, 1f)
    } else {
        0f
    }

    // Dynamic thumb height
    val thumbHeight = if (estimatedContentHeight > viewportHeight) {
         (viewportHeight * (viewportHeight / estimatedContentHeight)).coerceIn(thumbHeightMin, viewportHeight / 2)
    } else {
        viewportHeight 
    }

    val trackHeight = viewportHeight
    val scrollRange = trackHeight - thumbHeight
    
    // Animate thumb movement if not dragging
    val animatedScrollProgress by animateFloatAsState(
        targetValue = scrollProgress,
        animationSpec = tween(durationMillis = 100),
        label = "ScrollProgress"
    )
    
    // Calculate final thumb Y position
    val thumbY = if (isDragging) {
        dragOffset.coerceIn(0f, scrollRange)
    } else {
        scrollRange * animatedScrollProgress
    }

    // Scrolling logic
    fun scrollToPosition(offset: Float) {
        val newProgress = (offset / scrollRange).coerceIn(0f, 1f)
        val exactIndex = newProgress * totalItemsCount
        val index = floor(exactIndex).toInt().coerceIn(0, totalItemsCount - 1)
        val scrollOffset = ((exactIndex - index) * estimatedItemHeight).toInt()
        
        coroutineScope.launch {
            listState.scrollToItem(index = index, scrollOffset = scrollOffset)
        }
    }

    Box(
        modifier = modifier
            .width(24.dp)
            .fillMaxHeight()
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    isDragging = true
                    dragOffset = (thumbY + delta).coerceIn(0f, scrollRange)
                    scrollToPosition(dragOffset)
                },
                onDragStarted = { offset ->
                    isDragging = true
                    dragOffset = offset.y.coerceIn(0f, scrollRange)
                    scrollToPosition(dragOffset)
                },
                onDragStopped = {
                    isDragging = false
                }
            )
            .padding(horizontal = 2.dp)
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val canvasWidth = size.width
            
            // Draw Track (Thin line on the right)
            drawLine(
                color = trackColor,
                start = Offset(x = canvasWidth - 4.dp.toPx(), y = 0f),
                end = Offset(x = canvasWidth - 4.dp.toPx(), y = size.height),
                strokeWidth = 2.dp.toPx()
            )

            // Draw Thumb
            drawRoundRect(
                color = thumbColor,
                topLeft = Offset(x = canvasWidth - 8.dp.toPx(), y = thumbY),
                size = Size(width = 6.dp.toPx(), height = thumbHeight),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
        }
    }
}
