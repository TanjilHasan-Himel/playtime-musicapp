package com.eplaytime.app.util

import android.media.audiofx.Visualizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

/**
 * VisualizerHelper - Connects to Android Visualizer API
 * Captures waveform data and exposes it as a normalized Float list for UI.
 */
class VisualizerHelper {

    private var visualizer: Visualizer? = null
    private var smoothedData = FloatArray(0)
    
    // Normalized waveform data (-1.0 to 1.0)
    private val _waveform = MutableStateFlow<List<Float>>(emptyList())
    val waveform: StateFlow<List<Float>> = _waveform.asStateFlow()

    fun start(audioSessionId: Int) {
        stop() // Stop existing if any

        try {
            if (audioSessionId == 0) return // Invalid session

            visualizer = Visualizer(audioSessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1] // Max size (usually 1024)
                
                // FFT Capture (HarmonicArc)
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int
                    ) {
                         // Optional: Keep waveform if needed, but FFT is priority for new look
                    }

                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int
                    ) {
                        fft?.let { bytes ->
                            // FFT Process:
                            // 1. Calculate Magnitudes: sqrt(real^2 + imag^2)
                            // 2. Smooth: weighted average
                            val n = bytes.size / 2
                            if (smoothedData.size != n) {
                                smoothedData = FloatArray(n)
                            }
                            
                            // Only capture first 64 bands (low-mid frequencies where the beat is)
                            // System FFT is size 1024 -> 512 magnitudes. Most are high freq noise.
                            val captureSize = minOf(n, 64)
                            val output = ArrayList<Float>(captureSize)
                            
                            for (i in 0 until captureSize) {
                                // FFT data is Real, Imaginary interleaved
                                // k=0 is DC, k=1 is Nyquist (usually ignored for vis)
                                // Let's simplify:
                                val rfk = bytes[2 * i]
                                val ifk = bytes[2 * i + 1]
                                val magnitude = kotlin.math.sqrt((rfk.toFloat() * rfk + ifk * ifk)) / 180f
                                
                                // SMOOTHING (Poweramp Style)
                                // 0.85f * current + 0.15f * target
                                smoothedData[i] = smoothedData[i] * 0.85f + magnitude * 0.15f
                                
                                output.add(smoothedData[i])
                            }
                            _waveform.value = output
                        }
                    }
                }, Visualizer.getMaxCaptureRate() / 2, false, true) // Waveform=false, FFT=true
                
                enabled = true
            }
            Log.d("VisualizerHelper", "Visualizer started on Session ID: $audioSessionId")
        } catch (e: Exception) {
            Log.e("VisualizerHelper", "Error starting visualizer", e)
        }
    }

    fun stop() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
            visualizer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
