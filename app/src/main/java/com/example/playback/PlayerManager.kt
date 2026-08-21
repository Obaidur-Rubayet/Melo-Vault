package com.example.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.example.data.model.Song
import com.example.data.repository.MusicRepository
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class RepeatMode(val value: Int) {
    OFF(Player.REPEAT_MODE_OFF),
    ONE(Player.REPEAT_MODE_ONE),
    ALL(Player.REPEAT_MODE_ALL)
}

data class EqualizerPreset(
    val name: String,
    val bands: List<Int> // dB gain values in range -10 to +10 dB
)

class PlayerManager(
    private val context: Context,
    private val repository: MusicRepository
) {
    companion object {
        private const val TAG = "PlayerManager"
        @Volatile
        private var INSTANCE: PlayerManager? = null

        fun getInstance(context: Context, repository: MusicRepository): PlayerManager {
            return INSTANCE ?: synchronized(this) {
                val instance = PlayerManager(context.applicationContext, repository)
                INSTANCE = instance
                instance
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    // State Flows
    private val prefs = context.getSharedPreferences("melovault_playback_prefs", Context.MODE_PRIVATE)

    private val _isGaplessEnabled = MutableStateFlow(prefs.getBoolean("gapless_playback", true))
    val isGaplessEnabled: StateFlow<Boolean> = _isGaplessEnabled.asStateFlow()

    private val _crossfadeDurationSeconds = MutableStateFlow(prefs.getInt("crossfade_duration_seconds", 0))
    val crossfadeDurationSeconds: StateFlow<Int> = _crossfadeDurationSeconds.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _currentQueueIndex = MutableStateFlow(0)
    val currentQueueIndex: StateFlow<Int> = _currentQueueIndex.asStateFlow()

    private val _shuffleMode = MutableStateFlow(false)
    val shuffleMode: StateFlow<Boolean> = _shuffleMode.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    // Sleep Timer
    private val _sleepTimerRemainingSeconds = MutableStateFlow<Int?>(null)
    val sleepTimerRemainingSeconds: StateFlow<Int?> = _sleepTimerRemainingSeconds.asStateFlow()
    private var sleepTimerJob: Job? = null
    private var endAfterCurrentSong: Boolean = false

    // Equalizer & FX
    private val _isEqualizerEnabled = MutableStateFlow(true)
    val isEqualizerEnabled: StateFlow<Boolean> = _isEqualizerEnabled.asStateFlow()

    private val _equalizerBands = MutableStateFlow(listOf(0, 0, 0, 0, 0)) // 5 bands in dB
    val equalizerBands: StateFlow<List<Int>> = _equalizerBands.asStateFlow()

    private val _bassBoostLevel = MutableStateFlow(0) // 0 to 1000
    val bassBoostLevel: StateFlow<Int> = _bassBoostLevel.asStateFlow()

    private val _virtualizerLevel = MutableStateFlow(0) // 0 to 1000
    val virtualizerLevel: StateFlow<Int> = _virtualizerLevel.asStateFlow()

    private val _selectedPreset = MutableStateFlow("Normal")
    val selectedPreset: StateFlow<String> = _selectedPreset.asStateFlow()

    val presets = listOf(
        EqualizerPreset("Normal", listOf(0, 0, 0, 0, 0)),
        EqualizerPreset("Rock", listOf(4, 2, -1, 3, 5)),
        EqualizerPreset("Pop", listOf(-1, 2, 4, 2, -1)),
        EqualizerPreset("Jazz", listOf(3, 1, -1, 2, 4)),
        EqualizerPreset("Classical", listOf(4, 2, -2, 3, 4)),
        EqualizerPreset("Vocal", listOf(-2, 1, 4, 3, 0)),
        EqualizerPreset("Bass Boost", listOf(6, 4, 1, 0, -1)),
        EqualizerPreset("Electronic", listOf(4, 2, 0, 3, 5))
    )

    private var progressJob: Job? = null

    init {
        initializeMediaController()
    }

    private fun initializeMediaController() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, MusicPlaybackService::class.java)
        )
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                setupPlayerListener()
                syncEqualizerState()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to MediaSession", e)
            }
        }, MoreExecutors.directExecutor())
    }

    private fun setupPlayerListener() {
        val player = mediaController ?: return

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    startProgressTracker()
                } else {
                    stopProgressTracker()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val currentMediaIndex = player.currentMediaItemIndex
                _currentQueueIndex.value = currentMediaIndex
                val queueList = _queue.value
                if (currentMediaIndex in queueList.indices) {
                    val song = queueList[currentMediaIndex]
                    _currentSong.value = song
                    _durationMs.value = song.duration
                    // Record play in database
                    scope.launch {
                        repository.recordSongPlay(song.id)
                    }
                }

                // Handle "End after current song" sleep timer
                if (endAfterCurrentSong && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                    pause()
                    cancelSleepTimer()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        _durationMs.value = player.duration.coerceAtLeast(0L)
                    }
                    Player.STATE_ENDED -> {
                        _isPlaying.value = false
                        if (endAfterCurrentSong) {
                            cancelSleepTimer()
                        }
                    }
                    else -> {}
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _shuffleMode.value = shuffleModeEnabled
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _repeatMode.value = when (repeatMode) {
                    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                    Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                    else -> RepeatMode.OFF
                }
            }
        })

        _isPlaying.value = player.isPlaying
        _shuffleMode.value = player.shuffleModeEnabled
        _repeatMode.value = when (player.repeatMode) {
            Player.REPEAT_MODE_ONE -> RepeatMode.ONE
            Player.REPEAT_MODE_ALL -> RepeatMode.ALL
            else -> RepeatMode.OFF
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                mediaController?.let { player ->
                    val pos = player.currentPosition.coerceAtLeast(0L)
                    val dur = player.duration.coerceAtLeast(0L)
                    _currentPositionMs.value = pos
                    if (dur > 0) {
                        _durationMs.value = dur
                    }

                    // Crossfade volume envelope
                    val crossfadeSecs = _crossfadeDurationSeconds.value
                    if (crossfadeSecs > 0 && dur > crossfadeSecs * 2000L && player.isPlaying) {
                        val crossfadeMs = crossfadeSecs * 1000L
                        val remainingMs = dur - pos
                        if (remainingMs in 0L..crossfadeMs) {
                            // Fade out towards track end
                            val factor = (remainingMs.toFloat() / crossfadeMs).coerceIn(0.0f, 1f)
                            player.volume = factor
                            if (remainingMs <= 150L && player.hasNextMediaItem()) {
                                player.seekToNextMediaItem()
                                player.volume = 0.0f
                            }
                        } else if (pos in 0L..crossfadeMs) {
                            // Fade in at track start
                            val factor = (pos.toFloat() / crossfadeMs).coerceIn(0.0f, 1f)
                            player.volume = factor
                        } else {
                            player.volume = 1.0f
                        }
                    } else {
                        player.volume = 1.0f
                    }
                }
                delay(250)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        mediaController?.let { player ->
            _currentPositionMs.value = player.currentPosition.coerceAtLeast(0L)
        }
    }

    fun playSongList(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return

        _queue.value = songs
        _currentQueueIndex.value = startIndex
        val songToPlay = songs[startIndex]
        _currentSong.value = songToPlay

        val player = mediaController ?: return

        val mediaItems = songs.map { song ->
            val metadata = MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artist)
                .setAlbumTitle(song.album)
                .setArtworkUri(song.albumArtUri?.let { Uri.parse(it) })
                .build()

            MediaItem.Builder()
                .setUri(Uri.parse(song.contentUri))
                .setMediaId(song.id.toString())
                .setMediaMetadata(metadata)
                .build()
        }

        player.setMediaItems(mediaItems, startIndex, 0L)
        player.prepare()
        player.play()
    }

    fun playNext(song: Song) {
        val currentList = _queue.value.toMutableList()
        val currentIndex = _currentQueueIndex.value
        val insertIndex = (currentIndex + 1).coerceAtMost(currentList.size)
        currentList.add(insertIndex, song)
        _queue.value = currentList

        mediaController?.let { player ->
            val metadata = MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artist)
                .setAlbumTitle(song.album)
                .setArtworkUri(song.albumArtUri?.let { Uri.parse(it) })
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(Uri.parse(song.contentUri))
                .setMediaId(song.id.toString())
                .setMediaMetadata(metadata)
                .build()

            player.addMediaItem(insertIndex, mediaItem)
        }
    }

    fun addToQueue(song: Song) {
        val currentList = _queue.value.toMutableList()
        currentList.add(song)
        _queue.value = currentList

        mediaController?.let { player ->
            val metadata = MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artist)
                .setAlbumTitle(song.album)
                .setArtworkUri(song.albumArtUri?.let { Uri.parse(it) })
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(Uri.parse(song.contentUri))
                .setMediaId(song.id.toString())
                .setMediaMetadata(metadata)
                .build()

            player.addMediaItem(mediaItem)
        }
    }

    fun removeFromQueue(index: Int) {
        val currentList = _queue.value.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            _queue.value = currentList
            mediaController?.removeMediaItem(index)
        }
    }

    fun clearQueue() {
        _queue.value = emptyList()
        _currentSong.value = null
        _isPlaying.value = false
        mediaController?.clearMediaItems()
    }

    fun play() {
        mediaController?.play()
    }

    fun pause() {
        mediaController?.pause()
    }

    fun togglePlayPause() {
        val player = mediaController ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun seekTo(positionMs: Long) {
        _currentPositionMs.value = positionMs
        mediaController?.seekTo(positionMs)
    }

    fun skipToNext() {
        val player = mediaController ?: return
        player.volume = 1.0f
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        }
    }

    fun skipToPrevious() {
        val player = mediaController ?: return
        player.volume = 1.0f
        if (player.currentPosition > 3000 || !player.hasPreviousMediaItem()) {
            player.seekTo(0L)
        } else {
            player.seekToPreviousMediaItem()
        }
    }

    fun toggleShuffle() {
        val player = mediaController ?: return
        val newShuffle = !player.shuffleModeEnabled
        player.shuffleModeEnabled = newShuffle
        _shuffleMode.value = newShuffle
    }

    fun toggleRepeat() {
        val player = mediaController ?: return
        val newMode = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        player.repeatMode = newMode.value
        _repeatMode.value = newMode
    }

    // Sleep Timer
    fun startSleepTimer(minutes: Int, afterCurrentSong: Boolean = false) {
        cancelSleepTimer()
        endAfterCurrentSong = afterCurrentSong

        if (afterCurrentSong) {
            _sleepTimerRemainingSeconds.value = -1 // Indicates end of song
            return
        }

        var remaining = minutes * 60
        _sleepTimerRemainingSeconds.value = remaining

        sleepTimerJob = scope.launch {
            while (remaining > 0 && isActive) {
                delay(1000)
                remaining--
                _sleepTimerRemainingSeconds.value = remaining
            }
            if (remaining <= 0) {
                pause()
                _sleepTimerRemainingSeconds.value = null
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        endAfterCurrentSong = false
        _sleepTimerRemainingSeconds.value = null
    }

    // Equalizer & FX adjustments
    private fun syncEqualizerState() {
        val controller = mediaController ?: return
        val enabled = _isEqualizerEnabled.value
        controller.sendCustomCommand(SessionCommand(MusicPlaybackService.CMD_SET_EQUALIZER_ENABLED, Bundle.EMPTY), Bundle().apply { putBoolean("enabled", enabled) })
        
        _equalizerBands.value.forEachIndexed { index, gain ->
            controller.sendCustomCommand(SessionCommand(MusicPlaybackService.CMD_SET_EQUALIZER, Bundle.EMPTY), Bundle().apply {
                putInt("band", index)
                putShort("level", (gain * 100).toShort())
            })
        }
        
        controller.sendCustomCommand(SessionCommand(MusicPlaybackService.CMD_SET_BASS_BOOST, Bundle.EMPTY), Bundle().apply {
            putShort("strength", _bassBoostLevel.value.toShort())
        })
        
        controller.sendCustomCommand(SessionCommand(MusicPlaybackService.CMD_SET_VIRTUALIZER, Bundle.EMPTY), Bundle().apply {
            putShort("strength", _virtualizerLevel.value.toShort())
        })
    }

    fun setEqualizerPreset(presetName: String) {
        val preset = presets.find { it.name.equals(presetName, ignoreCase = true) } ?: return
        _selectedPreset.value = preset.name
        _equalizerBands.value = preset.bands
        preset.bands.forEachIndexed { index, gain ->
            sendBandGainCommand(index, gain)
        }
    }

    fun setBandGain(bandIndex: Int, gainDb: Int) {
        val current = _equalizerBands.value.toMutableList()
        if (bandIndex in current.indices) {
            current[bandIndex] = gainDb.coerceIn(-10, 10)
            _equalizerBands.value = current
            _selectedPreset.value = "Custom"
            sendBandGainCommand(bandIndex, gainDb)
        }
    }

    private fun sendBandGainCommand(bandIndex: Int, gainDb: Int) {
        mediaController?.let { controller ->
            val command = SessionCommand(MusicPlaybackService.CMD_SET_EQUALIZER, Bundle.EMPTY)
            val args = Bundle().apply {
                putInt("band", bandIndex)
                putShort("level", (gainDb * 100).toShort())
            }
            controller.sendCustomCommand(command, args)
        }
    }

    fun setBassBoost(level: Int) {
        val clamped = level.coerceIn(0, 1000)
        _bassBoostLevel.value = clamped
        mediaController?.let { controller ->
            val command = SessionCommand(MusicPlaybackService.CMD_SET_BASS_BOOST, Bundle.EMPTY)
            val args = Bundle().apply {
                putShort("strength", clamped.toShort())
            }
            controller.sendCustomCommand(command, args)
        }
    }

    fun setVirtualizer(level: Int) {
        val clamped = level.coerceIn(0, 1000)
        _virtualizerLevel.value = clamped
        mediaController?.let { controller ->
            val command = SessionCommand(MusicPlaybackService.CMD_SET_VIRTUALIZER, Bundle.EMPTY)
            val args = Bundle().apply {
                putShort("strength", clamped.toShort())
            }
            controller.sendCustomCommand(command, args)
        }
    }

    fun toggleEqualizer(enabled: Boolean) {
        _isEqualizerEnabled.value = enabled
        mediaController?.let { controller ->
            val command = SessionCommand(MusicPlaybackService.CMD_SET_EQUALIZER_ENABLED, Bundle.EMPTY)
            val args = Bundle().apply {
                putBoolean("enabled", enabled)
            }
            controller.sendCustomCommand(command, args)
        }
    }

    fun setGaplessEnabled(enabled: Boolean) {
        _isGaplessEnabled.value = enabled
        prefs.edit().putBoolean("gapless_playback", enabled).apply()
    }

    fun setCrossfadeDuration(seconds: Int) {
        val clamped = seconds.coerceIn(0, 12)
        _crossfadeDurationSeconds.value = clamped
        prefs.edit().putInt("crossfade_duration_seconds", clamped).apply()
    }

    fun updateSongMetadata(
        songId: Long,
        title: String,
        artist: String,
        album: String,
        albumArtist: String? = null,
        genre: String? = null,
        year: Int? = null,
        trackNumber: Int = 0
    ) {
        _currentSong.value?.let { current ->
            if (current.id == songId) {
                _currentSong.value = current.copy(
                    title = title,
                    artist = artist,
                    album = album,
                    albumArtist = albumArtist,
                    genre = genre,
                    year = year,
                    trackNumber = trackNumber
                )
            }
        }
        val updatedQueue = _queue.value.map { song ->
            if (song.id == songId) {
                song.copy(
                    title = title,
                    artist = artist,
                    album = album,
                    albumArtist = albumArtist,
                    genre = genre,
                    year = year,
                    trackNumber = trackNumber
                )
            } else {
                song
            }
        }
        _queue.value = updatedQueue
    }

    fun toggleFavorite(song: Song) {
        val newFav = !song.isFavorite
        scope.launch {
            repository.toggleFavorite(song.id, newFav)
            if (_currentSong.value?.id == song.id) {
                _currentSong.value = _currentSong.value?.copy(isFavorite = newFav)
            }
        }
    }
}
