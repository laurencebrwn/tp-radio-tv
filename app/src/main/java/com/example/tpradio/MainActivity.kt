package com.example.tpradio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
            var currentSchemeIndex by remember { mutableStateOf(0) }
            var syncToTrack by remember { mutableStateOf(false) }
            var currentRoom by remember { mutableStateOf<RadioRoom?>(null) }
            var nowPlaying by remember { mutableStateOf<NowPlayingResponse?>(null) }
            var isPaused by remember { mutableStateOf(false) }

            val trackThemeIndex = nowPlaying?.getThemeIndex()

            TPRadioTheme(
                syncToTrack = syncToTrack,
                trackThemeIndex = trackThemeIndex,
                onSchemeChanged = { currentSchemeIndex = it }
            ) {
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
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    RadioScreen(
                        currentRoom = currentRoom,
                        nowPlaying = nowPlaying,
                        currentSchemeIndex = currentSchemeIndex,
                        syncToTrack = syncToTrack,
                        isPaused = isPaused,
                        onToggleSync = { syncToTrack = !syncToTrack },
                        onPlayRoom1 = {
                            if (currentRoom == room1) {
                                // Toggle pause for same room
                                isPaused = !isPaused
                                if (isPaused) {
                                    player.pause()
                                } else {
                                    player.play()
                                }
                            } else {
                                // Switch to Room 1
                                currentRoom = room1
                                isPaused = false
                                playRoom(room1)
                            }
                        },
                        onPlayRoom2 = {
                            if (currentRoom == room2) {
                                // Toggle pause for same room
                                isPaused = !isPaused
                                if (isPaused) {
                                    player.pause()
                                } else {
                                    player.play()
                                }
                            } else {
                                // Switch to Room 2
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
