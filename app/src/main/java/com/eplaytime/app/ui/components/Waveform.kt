package com.eplaytime.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.eplaytime.app.ui.viewmodel.MusicViewModel.VisualizerStyle
import com.eplaytime.app.util.VisualizerHelper
import kotlin.math.abs

/**
 * SwipeableVisualizer - A multi-mode visualizer carousel.
 * Supports: LIQUID_FILL, NEON_LINE, CLASSIC_BARS.
 * Swipe horizontally to switch styles.
 */
@Composable
fun SwipeableVisualizer(
    visualizerHelper: VisualizerHelper,
    currentStyle: VisualizerStyle,
    onSwipeNext: () -> Unit,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    val waveform by visualizerHelper.waveform.collectAsState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp) // Adjusted height as requested
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    // Simple swipe detection threshold
                    if (abs(dragAmount) > 20) {
                        change.consume()
                        onSwipeNext()
                    }
                }
            }
    ) {
        AnimatedContent(
            targetState = currentStyle,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            },
            label = "VisualizerStyleData"
        ) { style ->
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (waveform.isEmpty()) return@Canvas

                val width = size.width
                val height = size.height
                val centerY = height / 2f
                val points = waveform

                when (style) {
                    VisualizerStyle.LIQUID_FILL -> {
                        // Style 1: LIQUID FILL (Symmetric/Mirrored)
                        if (points.size < 2) return@Canvas
                        val path = Path()
                        val stepX = width / (points.size - 1)
                        
                        // Top Curve (Mirrored)
                        path.moveTo(0f, centerY)
                        for (i in 0 until points.size) {
                            val x = i * stepX
                            val y = centerY - (points[i] * centerY) // Upwards
                            if (i == 0) path.lineTo(x, y)
                            else {
                                val prevX = (i - 1) * stepX
                                val prevY = centerY - (points[i - 1] * centerY)
                                val cx = prevX + (x - prevX) / 2
                                path.cubicTo(cx, prevY, cx, y, x, y)
                            }
                        }
                        
                        // Bottom Curve
                        for (i in points.size - 1 downTo 0) {
                            val x = i * stepX
                            val y = centerY + (points[i] * centerY) // Downwards
                            val prevX = (i + 1) * stepX
                            val prevY = centerY + (if (i < points.size - 1) points[i + 1] else points[i]) * centerY
                            
                            val cx = prevX + (x - prevX) / 2
                            path.cubicTo(cx, prevY, cx, y, x, y)
                        }
                        path.close()

                        drawPath(
                            path = path,
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, primaryColor.copy(alpha = 0.8f), Color.Transparent),
                            )
                        )
                    }

                    VisualizerStyle.NEON_LINE -> {
                        // Style 2: NEON LINE (Symmetric Stroke)
                        if (points.size < 2) return@Canvas
                        val path = Path()
                        val stepX = width / (points.size - 1)
                        
                        // Draw just one line? Or mirrored?
                        // Let's do a single nice curve that represents the "Beat"
                        // Or a mirrored line looks like a soundwave.
                        
                        // Top Line
                        path.moveTo(0f, centerY - points[0] * centerY)
                        for (i in 1 until points.size) {
                            val prevX = (i - 1) * stepX
                            val prevY = centerY - points[i - 1] * centerY
                            val currX = i * stepX
                            val currY = centerY - points[i] * centerY
                            val cx = prevX + (currX - prevX) / 2
                            path.cubicTo(cx, prevY, cx, currY, currX, currY)
                        }
                        
                        // Bottom Line (Reverse)
                        for (i in points.size - 1 downTo 0) {
                            val currX = i * stepX
                            val currY = centerY + points[i] * centerY
                            if (i == points.size - 1) {
                                path.moveTo(currX, currY) 
                            } else {
                                val prevX = (i + 1) * stepX
                                val prevY = centerY + points[i + 1] * centerY
                                val cx = prevX + (currX - prevX) / 2
                                path.cubicTo(cx, prevY, cx, currY, currX, currY)
                            }
                        }

                        drawPath(
                            path = path,
                            color = primaryColor,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )
                    }

                    VisualizerStyle.CLASSIC_BARS -> {
                        // Style 3: CLASSIC BARS (Symmetric)
                        val barCount = 40
                        val barWidth = (width / barCount) * 0.6f
                        val gap = (width / barCount) * 0.4f
                        
                        val sampleStep = points.size / barCount.toFloat()
                        
                        for (i in 0 until barCount) {
                            val index = (i * sampleStep).toInt().coerceIn(0, points.lastIndex)
                            val amplitude = points[index]
                            
                            val barHeight = (amplitude * height).coerceAtLeast(4.dp.toPx())
                            
                            val x = i * (barWidth + gap) + gap/2
                            val top = centerY - (barHeight / 2)
                            
                            drawRoundRect(
                                color = primaryColor,
                                topLeft = Offset(x, top),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(barWidth/2, barWidth/2)
                            )
                        }
                    }
                }
            }
        }
    }
}
