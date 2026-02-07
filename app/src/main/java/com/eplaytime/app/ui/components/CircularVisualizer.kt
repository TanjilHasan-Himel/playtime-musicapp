package com.eplaytime.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.eplaytime.app.ui.theme.SoftGold
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.min

/**
 * HarmonicArc Lite - Circular FFT Visualizer
 * "Poweramp-style" premium look with smoothed physics.
 */
@Composable
fun CircularVisualizer(
    magnitudes: List<Float>,
    modifier: Modifier = Modifier,
    primaryColor: Color = SoftGold
) {
    // Idle Animation: Breathing subtle sine wave when no audio
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val idlePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "idle_phase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width / 2
        val cy = size.height / 2
        val radius = min(cx, cy) * 0.6f // Base radius (room for spikes)
        val maxSpike = min(cx, cy) * 0.35f
        
        // Premium Look: Dark/Glowy center (optional, nice touch)
        // drawCircle(Color.Black.copy(alpha = 0.5f), radius)

        if (magnitudes.isEmpty() || magnitudes.all { it < 0.1f }) {
            // Idle State: Draw gentle breathing circle
            for (i in 0 until 64) {
                val angle = (i.toFloat() / 64) * 2 * Math.PI + idlePhase
                val offset = sin(angle * 3 + idlePhase).toFloat() * 10f // Subtle wave
                
                val r = radius + offset
                val x = cx + cos(angle).toFloat() * r
                val y = cy + sin(angle).toFloat() * r
                
                // Draw dots or short lines for minimal idle look
                drawCircle(
                    color = primaryColor.copy(alpha = 0.3f),
                    radius = 2f,
                    center = Offset(x, y)
                )
            }
            return@Canvas
        }

        // Active State: Draw FFT Spikes
        val bandCount = magnitudes.size
        
        for (i in 0 until bandCount) {
            // Map index to angle (Circle)
            // We want bass (low index) at bottom or top? Usually circular goes 0..360
            // Let's start from top (-PI/2) and go clockwise
            val angle = (i.toFloat() / bandCount) * 2 * Math.PI - (Math.PI / 2)
            
            // Value Processing (Normalize)
            // FFT magnitudes can be large (0..128+). We expect 'magnitudes' to be smoothed already.
            // Let's normalize visually. 
            // In AudioVisualizer we smoothed raw values (range ~0..1+ after normalization/div).
            // Wait, in AudioVisualizer I used: (byte - 128) / 128f -> range -1..1?
            // No, for FFT bytes are 0..255. 
            // Let's check AudioVisualizer code again. I implemented:
            // magnitude = sqrt(real^2 + imag^2). 
            // Real/Imag are normalized or raw?
            // "val rfk = bytes[index]" -> byte is -128..127. 
            // I should have treated them as raw.
            // Assuming magnitude is roughly 0..100+ range.
            // We need to scale it to pixels.
            
            var value = magnitudes[i]
            
            // Bass Weighting (Visual Trick)
            if (i < 8) {
                value *= 1.4f
            }
            
            // Log Scale / Scaling to MaxSpike
            // Cap value to avoid drawing off screen
            // Heuristic scaling
            val spikeHeight = (value * 2f).coerceAtMost(maxSpike) 

            val startR = radius
            val endR = radius + spikeHeight
            
            val cosA = cos(angle).toFloat()
            val sinA = sin(angle).toFloat()
            
            val start = Offset(cx + cosA * startR, cy + sinA * startR)
            val end = Offset(cx + cosA * endR, cy + sinA * endR)
            
            // Draw Line
            // Outer fade: opacity based on height? Or consistent?
            // "Glow via alpha" -> Lower alpha for tip?
            // Let's keep it simple: Solid line with alpha
            drawLine(
                color = primaryColor.copy(alpha = 0.8f + (value/50f).coerceAtMost(0.2f)), // Brighter if loud
                start = start,
                end = end,
                strokeWidth = 4f, // 1.5dp equivalent
                cap = StrokeCap.Round
            )
        }
    }
}
