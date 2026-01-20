package com.snowsnooks.tpradio

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import android.content.ComponentName
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

import androidx.tv.material3.*
import com.snowsnooks.tpradio.ui.theme.TPRadioTheme
import com.snowsnooks.tpradio.ui.theme.colorSchemes
import com.snowsnooks.core.domain.RadioRoom
import com.snowsnooks.core.domain.NowPlayingResponse
import com.snowsnooks.core.data.RadioApiRepository
import com.snowsnooks.core.playback.MediaPlaybackService
import kotlinx.coroutines.delay

@OptIn(ExperimentalTvMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private lateinit var mediaController: MediaController
    private val radioRepository = RadioApiRepository()
    private var launchedFromMediaControls = false
    private var pendingPlayRoom: RadioRoom? = null

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

        // Connect to the MediaPlaybackService
        val sessionToken = SessionToken(this, ComponentName(this, MediaPlaybackService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()

        controllerFuture.addListener({
            mediaController = controllerFuture.get()

            // Add listener to sync internal state with controller state changes
            mediaController.addListener(object : androidx.media3.common.Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    runOnUiThread {
                        when (playbackState) {
                            androidx.media3.common.Player.STATE_READY -> {
                                // Player is ready, update states
                                isCurrentlyPlaying = mediaController.isPlaying
                                isPausedState = !mediaController.isPlaying
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

            // If there was a pending play request, execute it now
            pendingPlayRoom?.let { playRoom(it) }
            pendingPlayRoom = null
        }, { runnable -> runnable.run() })

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
                          radioRepository.fetchNowPlaying(currentRoom!!.metadataUrl, onResult = {
                              nowPlaying = it
                              lifecycleScope.launch {
                                  updateMediaSessionMetadata(it, currentRoom)
                              }
                          })
                         delay(10_000)
                     }
                 }

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets(0))
                        .background(backgroundColor.copy(alpha = transitionController.backgroundAlpha)),
                    shape = RectangleShape
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
                                 if (::mediaController.isInitialized) {
                                     if (isPaused) {
                                         mediaController.pause()
                                     } else {
                                         mediaController.play()
                                     }
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
                                 if (::mediaController.isInitialized) {
                                     if (isPaused) {
                                         mediaController.pause()
                                     } else {
                                         mediaController.play()
                                     }
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
        // Start the service
        val intent = Intent(this, MediaPlaybackService::class.java)
        startService(intent)

        // Check if controller is ready
        if (!::mediaController.isInitialized) {
            // Defer playback until controller is ready
            pendingPlayRoom = room
            return
        }

        // If same room is selected, just toggle play/pause
        if (currentRoomState == room) {
            isPausedState = !isPausedState
            if (isPausedState) {
                mediaController.pause()
            } else {
                mediaController.play()
            }
            return
        }

        // Different room selected - stop current and start new
        mediaController.stop()
        mediaController.clearMediaItems()

        val mediaItem = MediaItem.Builder()
            .setUri(room.streamUrl)
            .setMimeType("audio/mpeg")
            .build()

        mediaController.setMediaItem(mediaItem)
        mediaController.prepare()

        // Always start playing new room (unless paused state is set)
        mediaController.play()
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



    private suspend fun updateMediaSessionMetadata(nowPlaying: NowPlayingResponse?, currentRoom: RadioRoom?) {
        if (!::mediaController.isInitialized) return
        try {
            val metadataBuilder = androidx.media3.common.MediaMetadata.Builder()
                .setTitle(nowPlaying?.nowplaying ?: "TPRadio")
                .setArtist(currentRoom?.name ?: "Radio Stream")
                .setAlbumTitle("TranceParty Radio")

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
            val currentMediaItem = mediaController.currentMediaItem
            if (currentMediaItem != null && mediaController.currentMediaItemIndex >= 0) {
                val uri = currentMediaItem.localConfiguration?.uri
                if (uri != null) {
                    val updatedMediaItem = androidx.media3.common.MediaItem.Builder()
                        .setUri(uri)
                        .setMediaMetadata(metadata)
                        .build()
                    withContext(Dispatchers.Main) {
                        try {
                            mediaController.replaceMediaItem(mediaController.currentMediaItemIndex, updatedMediaItem)
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
            if (::mediaController.isInitialized) {
                mediaController.play()
            } else {
                // Defer
                currentRoomState?.let { pendingPlayRoom = it }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Only release if not finishing (to prevent issues with configuration changes)
        if (isFinishing) {
            try {
                if (::mediaController.isInitialized) {
                    mediaController.release()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
