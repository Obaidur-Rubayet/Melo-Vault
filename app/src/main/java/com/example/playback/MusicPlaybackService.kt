package com.example.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.MainActivity
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class MusicPlaybackService : MediaSessionService() {

    companion object {
        private const val TAG = "MusicPlaybackService"
        const val CHANNEL_ID = "melovault_playback_channel"
        const val NOTIFICATION_ID = 1001

        const val CMD_SET_EQUALIZER = "CMD_SET_EQUALIZER"
        const val CMD_SET_BASS_BOOST = "CMD_SET_BASS_BOOST"
        const val CMD_SET_VIRTUALIZER = "CMD_SET_VIRTUALIZER"
        const val CMD_SET_EQUALIZER_ENABLED = "CMD_SET_EQUALIZER_ENABLED"
    }

    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null

    // Audio effects
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true) // handle audio focus automatically
            .setHandleAudioBecomingNoisy(true)
            .build()

        exoPlayer = player

        // Intent to open MainActivity when notification is tapped
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .setCallback(CustomMediaSessionCallback())
            .build()

        initAudioEffects(player.audioSessionId)
        if (equalizer == null) {
            initAudioEffects(0)
        }

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    initAudioEffects(player.audioSessionId)
                }
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                initAudioEffects(audioSessionId)
            }
        })
    }

    private fun initAudioEffects(audioSessionId: Int) {
        val sessionId = if (audioSessionId <= 0) 0 else audioSessionId
        try {
            if (equalizer == null) {
                equalizer = Equalizer(0, sessionId).apply {
                    enabled = true
                }
            }
            if (bassBoost == null) {
                bassBoost = BassBoost(0, sessionId).apply {
                    enabled = true
                }
            }
            if (virtualizer == null) {
                virtualizer = Virtualizer(0, sessionId).apply {
                    enabled = true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not initialize audio effects", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "MeloVault Music Playback"
            val descriptionText = "Media playback controls and notification"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        equalizer?.release()
        bassBoost?.release()
        virtualizer?.release()
        super.onDestroy()
    }

    private inner class CustomMediaSessionCallback : MediaSession.Callback {
        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                CMD_SET_EQUALIZER -> {
                    if (equalizer == null) initAudioEffects(exoPlayer?.audioSessionId ?: 0)
                    val band = args.getInt("band", -1)
                    val level = args.getShort("level", 0)
                    if (band >= 0) {
                        try {
                            equalizer?.setBandLevel(band.toShort(), level)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error setting equalizer band", e)
                        }
                    }
                }
                CMD_SET_BASS_BOOST -> {
                    if (bassBoost == null) initAudioEffects(exoPlayer?.audioSessionId ?: 0)
                    val strength = args.getShort("strength", 0)
                    try {
                        bassBoost?.setStrength(strength)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error setting bass boost", e)
                    }
                }
                CMD_SET_VIRTUALIZER -> {
                    if (virtualizer == null) initAudioEffects(exoPlayer?.audioSessionId ?: 0)
                    val strength = args.getShort("strength", 0)
                    try {
                        virtualizer?.setStrength(strength)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error setting virtualizer", e)
                    }
                }
                CMD_SET_EQUALIZER_ENABLED -> {
                    if (equalizer == null || bassBoost == null || virtualizer == null) initAudioEffects(exoPlayer?.audioSessionId ?: 0)
                    val enabled = args.getBoolean("enabled", true)
                    try {
                        equalizer?.enabled = enabled
                        bassBoost?.enabled = enabled
                        virtualizer?.enabled = enabled
                    } catch (e: Exception) {
                        Log.e(TAG, "Error setting equalizer enabled state", e)
                    }
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }
}
