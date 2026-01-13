package com.example.tpradio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
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
    private val client = OkHttpClient()

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

        setContent {
            val transitionController = remember { TransitionController() }
            var currentSchemeIndex by remember { mutableIntStateOf(0) }
            var displayedSchemeIndex by remember { mutableIntStateOf(0) }
            var syncToTrack by remember { mutableStateOf(false) }
            var currentRoom by remember { mutableStateOf<RadioRoom?>(null) }
            var nowPlaying by remember { mutableStateOf<NowPlayingResponse?>(null) }
            var isPaused by remember { mutableStateOf(false) }

            val trackThemeIndex = nowPlaying?.getThemeIndex()

            // Calculate target index
            val targetIndex = if (syncToTrack && trackThemeIndex != null) {
                trackThemeIndex
            } else {
                currentSchemeIndex
            }

            // Trigger transition when target changes
            LaunchedEffect(targetIndex) {
                if (targetIndex != displayedSchemeIndex) {
                    transitionController.executeTransition {
                        displayedSchemeIndex = targetIndex
                    }
                }
            }

            val animatedBackgroundColor by animateColorAsState(
                targetValue = colorSchemes[displayedSchemeIndex].background,
                animationSpec = tween(durationMillis = 1500), // Adjust fade duration here
                label = "BackgroundColorFade"
            )

            TPRadioTheme(
                themeIndex = displayedSchemeIndex
            ) {
                // Auto-rotate logic
                LaunchedEffect(syncToTrack) {
                    if (!syncToTrack) {
                        while (true) {
                            delay(5_000) // 2 minutes
                            currentSchemeIndex = (currentSchemeIndex + 1) % 5
                        }
                    }
                }

                LaunchedEffect(currentRoom) {
                    if (currentRoom == null) return@LaunchedEffect
                    while (true) {
                        fetchNowPlaying(currentRoom!!.metadataUrl) {
                            nowPlaying = it
                        }
                        delay(10_000)
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        // 4. Apply the animated color to the background
                        .background(animatedBackgroundColor)
                ) {
                    RadioScreen(
                        currentRoom = currentRoom,
                        nowPlaying = nowPlaying,
                        currentSchemeIndex = displayedSchemeIndex,
                        syncToTrack = syncToTrack,
                        isPaused = isPaused,
                        videoOverlayAlpha = transitionController.overlayAlpha, // ADD THIS
                        onToggleSync = { syncToTrack = !syncToTrack },
                        onPlayRoom1 = {
                            if (currentRoom == room1) {
                                isPaused = !isPaused
                                if (isPaused) player.pause() else player.play()
                            } else {
                                currentRoom = room1
                                isPaused = false
                                playRoom(room1)
                            }
                        },
                        onPlayRoom2 = {
                            if (currentRoom == room2) {
                                isPaused = !isPaused
                                if (isPaused) player.pause() else player.play()
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
        val mediaItem = MediaItem.Builder()
            .setUri(room.streamUrl)
            .setMimeType("audio/mpeg")
            .build()

        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
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

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}
