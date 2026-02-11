package com.eplaytime.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Binder
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaStyleNotificationHelper
import com.eplaytime.app.MainActivity
import com.eplaytime.app.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import android.util.Log
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel

/**
 * MusicService - Background music playback service using Media3 (ExoPlayer).
 * **Single Source of Truth:** The ONLY place where playback state is managed.
 * ViewModel and UI connect to this service via MediaController (not local player).
 */
@AndroidEntryPoint
class MusicService : MediaSessionService() {

    companion object {
        const val ACTION_PLAY_ALARM = "com.eplaytime.app.ACTION_PLAY_ALARM"
        const val EXTRA_SONG_URI = "extra_song_uri"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        private const val NOTIFICATION_CHANNEL_ID = "music_playback_channel"
        private const val NOTIFICATION_ID = 1
        private const val TAG = "MusicService"
    }

    @Inject
    lateinit var musicRepository: com.eplaytime.app.data.repository.MusicRepository

    private val binder = LocalBinder()
    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private lateinit var audioManager: AudioManager
    private var previousVolume: Int = -1
    private var wakeLock: android.os.PowerManager.WakeLock? = null
    
    private val serviceScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main)

    inner class LocalBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🎵 MusicService created - Single Source of Truth initialized")
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        initializePlayer()
        initializeMediaSession()
        
        // STANDARD NOTIFICATION PROVIDER (Configured for Lock Screen)
        val notificationProvider = androidx.media3.session.DefaultMediaNotificationProvider(this).apply {
            setSmallIcon(R.drawable.updateappicon)
        }
        setMediaNotificationProvider(notificationProvider)
    }

    // Helper for Alarm Notification (Legacy style for immediate start)
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent?.action == ACTION_PLAY_ALARM) {
            Log.d(TAG, "Alarm playback triggered")
            handleAlarmPlayback(intent)
        }

        return START_STICKY
    }

    private fun handleAlarmPlayback(intent: Intent) {
        val songUri = intent.getStringExtra(EXTRA_SONG_URI)
        
        if (songUri != null) {
            // 1. Maximize Volume
            previousVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val alarmVolume = (maxVolume * 0.8f).toInt() 
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, alarmVolume, 0)

            // 1.5 Acquire WakeLock (Immortal Mode)
            val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            wakeLock = powerManager.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "MusicService::AlarmWakeLock").apply {
                acquire(30 * 60 * 1000L) // 30 minutes timeout
            }

            // 1.6 IMMEDIATELY start Foreground Service to prevent kill
            Log.d(TAG, "🚀 STARTING FOREGROUND IMMEDIATELY")
            val notification = buildNotification(isPlaying = false, title = "Starting Alarm...", artist = "Audia Player")
            startForeground(NOTIFICATION_ID, notification)

            // 2. Play IMMEDIATELY
            serviceScope.launch {
                try {
                    val mediaItem = MediaItem.Builder()
                        .setUri(Uri.parse(songUri))
                        .setMediaMetadata(
                            androidx.media3.common.MediaMetadata.Builder()
                            .setTitle("Alarm")
                            .setArtist("Audia Player")
                            .build()
                        )
                        .build()

                    player.setMediaItem(mediaItem)
                    player.prepare()
                    player.playWhenReady = true
                    player.play()
                } catch (e: Exception) {
                    Log.e(TAG, "Error playing alarm", e)
                }
            }
        }
    }

    private fun buildNotification(isPlaying: Boolean, title: String?, artist: String?): Notification {
        val channelId = "playtime_music_channel"
        val channelName = "Music Playback"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val pauseIntent = Intent(this, MusicService::class.java).apply { action = "PAUSE_ACTION" } // We'll need to handle this
        val playIntent = Intent(this, MusicService::class.java).apply { action = "PLAY_ACTION" }
        
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.updateappicon) // Using appicon as fallback or ic_music_note if available
            .setContentTitle(title ?: "Audia Player")
            .setContentText(artist ?: "Playing")
            .setOngoing(isPlaying)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // NOTE: We don't attach the session token here because Media3 types mismatch with Legacy MediaStyle. 
            // This 'Starting...' notification is just an anchor to keep the service alive until playback starts.
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle())
            
        
        // Add minimal intent to open app
        val openIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE)
        builder.setContentIntent(openPendingIntent)

        return builder.build()
    }

    private fun initializePlayer() {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
    }

    private fun initializeMediaSession() {
        val sessionActivityIntent = Intent(this, MainActivity::class.java)
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            sessionActivityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .setCallback(CustomSessionCallback())
            .build()
    }

    private fun restoreVolume() {
        if (previousVolume >= 0) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, previousVolume, 0)
            previousVolume = -1
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    // Handle Custom Commands from ViewModel (MediaController)
    private val customCommandSessionId = androidx.media3.session.SessionCommand("ACTION_GET_AUDIO_SESSION_ID", android.os.Bundle.EMPTY)

    inner class CustomSessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val validCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
                .add(customCommandSessionId)
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(validCommands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: androidx.media3.session.SessionCommand,
            args: android.os.Bundle
        ): com.google.common.util.concurrent.ListenableFuture<androidx.media3.session.SessionResult> {
            if (customCommand.customAction == "ACTION_GET_AUDIO_SESSION_ID") {
                val resultBundle = android.os.Bundle().apply {
                    putInt("AUDIO_SESSION_ID", player.audioSessionId)
                }
                return com.google.common.util.concurrent.Futures.immediateFuture(
                    androidx.media3.session.SessionResult(androidx.media3.session.SessionResult.RESULT_SUCCESS, resultBundle)
                )
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (player.playWhenReady && player.mediaItemCount > 0) {
            // Keep playing
        } else {
            stopSelf()
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        restoreVolume()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
