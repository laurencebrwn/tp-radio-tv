package com.example.tpradio

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.URL

import androidx.tv.material3.*
import com.example.tpradio.ui.theme.TPRadioTheme
import com.example.tpradio.ui.theme.colorSchemes
import kotlinx.coroutines.delay
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

@OptIn(ExperimentalTvMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private val client = OkHttpClient()
    private var launchedFromMediaControls = false

    // State variables moved to activity level for persistence
    private var currentRoomState: RadioRoom? = null
    private var isPausedState = true
    private var isCurrentlyPlaying by mutableStateOf(false)

    companion object {
        private const val PREFS_NAME = "TPRadioPrefs"
        private const val KEY_CURRENT_ROOM = "currentRoom"
        private const val KEY_IS_PLAYING = "isPlaying"
    }

    private val room1 = RadioRoom(
        name = "Room 1",
        streamUrl = "https://radio.tranceparty.net:2020/stream/room-1",
        metadataUrl = "https://radio.tranceparty.net:2020/json/stream/room-1"
    )

    private val room2 = RadioRoom(
        name = "Room 2",
        streamUrl = "https://radio.tranceparty.net:2020/stream/room-2",
        metadataUrl = "https://radio.tranceparty.net:2020/json/stream/room-2"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        player = ExoPlayer.Builder(this).build().apply {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()
            setAudioAttributes(audioAttributes, true)
        }

        // Add listener to sync internal state with player state changes
        player.addListener(object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                runOnUiThread {
                    when (playbackState) {
                        androidx.media3.common.Player.STATE_READY -> {
                            // Player is ready, update states
                            isCurrentlyPlaying = player.isPlaying
                            isPausedState = !player.isPlaying
                        }
                        androidx.media3.common.Player.STATE_ENDED,
                        androidx.media3.common.Player.STATE_IDLE -> {
                            // Playback stopped
                            isCurrentlyPlaying = false
                            isPausedState = true
                        }
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                runOnUiThread {
                    isCurrentlyPlaying = isPlaying
                    isPausedState = !isPlaying
                }
            }
        })

        // Stop any existing playback to prevent multiple streams
        player.stop()
        player.clearMediaItems()

        val sessionId = "TPRadioSession-${System.currentTimeMillis()}"
        mediaSession = MediaSession.Builder(this, player)
            .setId(sessionId)
            .build()

        setContent {
            val transitionController = remember { TransitionController() }
            var currentSchemeIndex by remember { mutableIntStateOf(0) }
            var displayedSchemeIndex by remember { mutableIntStateOf(0) }

            // Initialize transition controller with starting scheme
            LaunchedEffect(Unit) {
                transitionController.currentSchemeIndex = 0
            }
            var syncToTrack by remember { mutableStateOf(false) }
            var currentRoom by remember { mutableStateOf(currentRoomState) }
            var nowPlaying by remember { mutableStateOf<NowPlayingResponse?>(null) }
            var isPaused by remember { mutableStateOf(isPausedState) }

            // Update activity state when composable state changes
            LaunchedEffect(currentRoom) {
                currentRoomState = currentRoom
            }
            LaunchedEffect(isPaused) {
                isPausedState = isPaused
            }

            val trackThemeIndex = nowPlaying?.getThemeIndex()

            // Calculate target index
            val targetIndex = if (syncToTrack) {
                trackThemeIndex ?: (nowPlaying?.nowplaying?.let { it.hashCode().mod(5) } ?: 0)
            } else {
                currentSchemeIndex
            }.coerceIn(0, 4) // Ensure valid range

            // Trigger transition when target changes
            LaunchedEffect(targetIndex) {
                if (targetIndex != transitionController.currentSchemeIndex && !transitionController.isTransitioning) {
                    transitionController.executeTransition(targetIndex) {
                        displayedSchemeIndex = targetIndex
                    }
                }
            }

            val prevScheme = colorSchemes[transitionController.previousSchemeIndex]
            val currScheme = colorSchemes[transitionController.currentSchemeIndex]
            val backgroundColor = lerp(prevScheme.background, currScheme.background, transitionController.transitionProgress)

            // Interpolate colors for smooth transitions in RadioScreen
            val onBackgroundColor = lerp(prevScheme.onBackground, currScheme.onBackground, transitionController.transitionProgress)
            val primaryColor = lerp(prevScheme.primary, currScheme.primary, transitionController.transitionProgress)

            TPRadioTheme(
                themeIndex = transitionController.currentSchemeIndex,
                previousThemeIndex = transitionController.previousSchemeIndex,
                transitionProgress = transitionController.transitionProgress
            ) {
                // Auto-rotate logic
                LaunchedEffect(Unit) {
                    while (true) {
                        if (!syncToTrack) {
                            delay(120_000) // 5 seconds for testing
                            currentSchemeIndex = (currentSchemeIndex + 1) % 5
                        } else {
                            delay(100) // Small delay when sync is active
                        }
                    }
                }

                LaunchedEffect(currentRoom) {
                    if (currentRoom == null) return@LaunchedEffect
                    while (true) {
                         fetchNowPlaying(currentRoom!!.metadataUrl) {
                             nowPlaying = it
                             lifecycleScope.launch {
                                 updateMediaSessionMetadata(it, currentRoom)
                             }
                         }
                        delay(10_000)
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundColor.copy(alpha = transitionController.backgroundAlpha))
                ) {
                    RadioScreen(
                        currentRoom = currentRoom,
                        nowPlaying = nowPlaying,
                        currentSchemeIndex = displayedSchemeIndex,
                        transitionProgress = transitionController.transitionProgress,
                        onBackgroundColor = onBackgroundColor,
                        primaryColor = primaryColor,
                        interpolatedBackgroundColor = backgroundColor,
                        syncToTrack = syncToTrack,
                        isPaused = isPaused,
                        isCurrentlyPlaying = isCurrentlyPlaying,
                        isTransitioning = transitionController.isTransitioning,
                        videoOverlayAlpha = transitionController.overlayAlpha,
                        onToggleSync = { 
                            if (!transitionController.isTransitioning) {
                                syncToTrack = !syncToTrack 
                            }
                        },
                        onPlayRoom1 = {
                            if (currentRoom == room1) {
                                isPaused = !isPaused
                                if (isPaused) {
                                    player.pause()
                                } else {
                                    player.play()
                                }
                            } else {
                                currentRoom = room1
                                isPaused = false
                                playRoom(room1)
                            }
                        },
                        onPlayRoom2 = {
                            if (currentRoom == room2) {
                                isPaused = !isPaused
                                if (isPaused) {
                                    player.pause()
                                } else {
                                    player.play()
                                }
                            } else {
                                currentRoom = room2
                                isPaused = false
                                playRoom(room2)
                            }
                        }
                    )
                }
            }
        }
    }

    private fun playRoom(room: RadioRoom) {
        // If same room is selected, just toggle play/pause
        if (currentRoomState == room) {
            isPausedState = !isPausedState
            if (isPausedState) {
                player.pause()
            } else {
                player.play()
            }
            return
        }

        // Different room selected - stop current and start new
        player.stop()
        player.clearMediaItems()

        val mediaItem = MediaItem.Builder()
            .setUri(room.streamUrl)
            .setMimeType("audio/mpeg")
            .build()

        player.setMediaItem(mediaItem)
        player.prepare()

        // Always start playing new room (unless paused state is set)
        player.play()
        isPausedState = false

        // Update state
        currentRoomState = room
        launchedFromMediaControls = false
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        // Check if launched from Android TV "Now Playing"
        launchedFromMediaControls = intent?.action == "android.intent.action.VIEW" ||
                                   intent?.getStringExtra("android.intent.extra.START_PLAYBACK") != null

        // Load saved state if launched from media controls
        if (launchedFromMediaControls) {
            val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val savedRoomName = prefs.getString(KEY_CURRENT_ROOM, null)
            savedRoomName?.let { roomName ->
                currentRoomState = when (roomName) {
                    "Room 1" -> room1
                    "Room 2" -> room2
                    else -> null
                }
            }
            isPausedState = !prefs.getBoolean(KEY_IS_PLAYING, false)
        }
    }

    private fun fetchNowPlaying(
        url: String,
        onResult: (NowPlayingResponse) -> Unit
    ) {
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { json ->
                    val obj = JSONObject(json)

                    val data = NowPlayingResponse(
                        status = obj.optBoolean("status"),
                        nowplaying = obj.optString("nowplaying"),
                        coverart = obj.optString("coverart"),
                        bitrate = obj.optString("bitrate"),
                        format = obj.optString("format")
                    )

                    runOnUiThread {
                        onResult(data)
                    }
                }
            }
        })
    }

    private suspend fun updateMediaSessionMetadata(nowPlaying: NowPlayingResponse?, currentRoom: RadioRoom?) {
        try {
            val metadataBuilder = androidx.media3.common.MediaMetadata.Builder()
                .setTitle(nowPlaying?.nowplaying ?: "TPRadio")
                .setArtist("TranceParty Radio")
                .setAlbumTitle(currentRoom?.name ?: "Radio Stream")

            // Load album art if available
            if (!nowPlaying?.coverart.isNullOrEmpty()) {
                try {
                    val bitmap = withContext(Dispatchers.IO) {
                        URL(nowPlaying.coverart).openStream().use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    }
                    if (bitmap != null && !bitmap.isRecycled) {
                        // Convert Bitmap to ByteArray for Media3
                        val stream = java.io.ByteArrayOutputStream()
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, stream)
                        val byteArray = stream.toByteArray()
                        stream.close()
                        metadataBuilder.setArtworkData(byteArray, androidx.media3.common.MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                    }
                } catch (e: Exception) {
                    // Ignore album art loading errors
                    e.printStackTrace()
                }
            }

            val metadata = metadataBuilder.build()

            // Update the current MediaItem with new metadata
            val currentMediaItem = player.currentMediaItem
            if (currentMediaItem != null && player.currentMediaItemIndex >= 0) {
                val uri = currentMediaItem.localConfiguration?.uri
                if (uri != null) {
                    val updatedMediaItem = androidx.media3.common.MediaItem.Builder()
                        .setUri(uri)
                        .setMediaMetadata(metadata)
                        .build()
                    withContext(Dispatchers.Main) {
                        try {
                            player.replaceMediaItem(player.currentMediaItemIndex, updatedMediaItem)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun lerp(start: Color, end: Color, fraction: Float): Color {
        return Color(
            red = start.red + (end.red - start.red) * fraction,
            green = start.green + (end.green - start.green) * fraction,
            blue = start.blue + (end.blue - start.blue) * fraction,
            alpha = start.alpha + (end.alpha - start.alpha) * fraction
        )
    }

    override fun onPause() {
        super.onPause()
        // Save current state
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_CURRENT_ROOM, currentRoomState?.name)
            .putBoolean(KEY_IS_PLAYING, !isPausedState)
            .apply()
    }

    override fun onResume() {
        super.onResume()
        // If launched from media controls and we have a room selected, start playback
        if (launchedFromMediaControls && currentRoomState != null && !isPausedState) {
            // Resume playback
            player.play()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Only release if not finishing (to prevent issues with configuration changes)
        if (isFinishing) {
            try {
                mediaSession.release()
                player.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
