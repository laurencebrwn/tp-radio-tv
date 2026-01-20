package com.snowsnooks.mobile

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil.compose.AsyncImage
import com.snowsnooks.core.domain.RadioRoom
import com.snowsnooks.core.domain.NowPlayingResponse
import com.snowsnooks.core.data.RadioApiRepository
import com.snowsnooks.core.playback.MediaPlaybackService
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalConfiguration
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private lateinit var mediaController: MediaController
    private val radioRepository = RadioApiRepository()
    private var pendingPlayRoom: RadioRoom? = null
    private var isControllerReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Allow content to extend behind system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Connect to MediaPlaybackService
        val sessionToken = SessionToken(this, ComponentName(this, MediaPlaybackService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()

        controllerFuture.addListener({
            mediaController = controllerFuture.get()
            isControllerReady = true

            // Add listener to sync internal state with controller state changes
            mediaController.addListener(object : androidx.media3.common.Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    runOnUiThread {
                        // Update UI state based on playback state
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    runOnUiThread {
                        // Update UI state based on playing state
                    }
                }
            })

            // If there was a pending play request, execute it now
            pendingPlayRoom?.let { playRoom(it) }
            pendingPlayRoom = null
        }, { runnable -> runnable.run() })

        setContent {
            TPRadioMobileTheme {
                MobileRadioApp(this@MainActivity)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mediaController.isInitialized) {
            mediaController.release()
        }
    }

    internal fun playRoom(room: RadioRoom) {
        if (!isControllerReady) {
            pendingPlayRoom = room
            return
        }

        // Stop any existing playback
        mediaController.stop()
        mediaController.clearMediaItems()

        // Set new media item
        val mediaItem = androidx.media3.common.MediaItem.Builder()
            .setUri(room.streamUrl)
            .setMimeType("audio/mpeg")
            .build()

        mediaController.setMediaItem(mediaItem)
        mediaController.prepare()
        mediaController.play()
    }

    internal fun togglePlayback() {
        if (!isControllerReady) return

        if (mediaController.isPlaying) {
            mediaController.pause()
        } else {
            mediaController.play()
        }
    }
}

@Composable
fun MobileRadioApp(activity: MainActivity) {
    // TV-style mobile layout with dark gradient background
    val backgroundColor = Color(0xFF1a1a2e)
    val onBackgroundColor = Color.White

    // State management
    val rooms = listOf(
        RadioRoom("Room 1", "https://radio.tranceparty.net:2020/stream/room-1", "https://radio.tranceparty.net:2020/json/stream/room-1"),
        RadioRoom("Room 2", "https://radio.tranceparty.net:2020/stream/room-2", "https://radio.tranceparty.net:2020/json/stream/room-2")
    )

    var currentRoom by remember { mutableStateOf(rooms[0]) }
    var nowPlaying by remember { mutableStateOf<NowPlayingResponse?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var syncToTrack by remember { mutableStateOf(false) }

    // Auto-fetch now playing data
    val radioRepository = RadioApiRepository()
    LaunchedEffect(currentRoom) {
        while (true) {
            radioRepository.fetchNowPlaying(currentRoom.metadataUrl, onResult = { response ->
                nowPlaying = response
            })
            delay(10000)
        }
    }

    // TV-style layout with gradient background extending behind system bars
    Box(modifier = Modifier.fillMaxSize()) {
        // Background gradient (TV-style dark theme) - fills entire screen including behind system bars
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            backgroundColor,
                            Color(0xFF16213e),
                            Color(0xFF0f3460)
                        )
                    )
                )
        )

        // Main content - with proper padding for system bars
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Room selector buttons (TV-style like the TV app)
            Row(
                modifier = Modifier.fillMaxWidth(0.95f),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Room 1 button (TV-style with corner markers)
                Box(modifier = Modifier.weight(1f)) {
                    CornerMarker(
                        up = true, right = true, down = true,
                        color = onBackgroundColor,
                        modifier = Modifier.align(Alignment.TopStart)
                    )
                    CornerMarker(
                        up = true, left = true, down = true,
                        color = onBackgroundColor,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                    CornerMarker(
                        right = true, up = true,
                        color = onBackgroundColor,
                        modifier = Modifier.align(Alignment.BottomStart)
                    )
                    CornerMarker(
                        left = true, up = true, right = true,
                        color = onBackgroundColor,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )

                    TVButton(
                        text = "ROOM 1",
                        onClick = {
                            currentRoom = rooms[0]
                            activity.playRoom(rooms[0])
                            isPlaying = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        textColor = onBackgroundColor,
                        forceHighlight = currentRoom == rooms[0]
                    )
                }

                // Room 2 button (TV-style with corner markers)
                Box(modifier = Modifier.weight(1f)) {
                    CornerMarker(
                        up = true, right = true, down = true,
                        color = onBackgroundColor,
                        modifier = Modifier.align(Alignment.TopStart)
                    )
                    CornerMarker(
                        up = true, left = true, down = true,
                        color = onBackgroundColor,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                    CornerMarker(
                        right = true, up = true,
                        color = onBackgroundColor,
                        modifier = Modifier.align(Alignment.BottomStart)
                    )
                    CornerMarker(
                        left = true, up = true, right = true,
                        color = onBackgroundColor,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )

                    TVButton(
                        text = "ROOM 2",
                        onClick = {
                            currentRoom = rooms[1]
                            activity.playRoom(rooms[1])
                            isPlaying = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        textColor = onBackgroundColor,
                        forceHighlight = currentRoom == rooms[1]
                    )
                }
            }

            // Album art (TV-style with corner markers)
            Box(modifier = Modifier.size(200.dp)) {
                CornerMarker(
                    up = true, right = true, down = true,
                    color = onBackgroundColor,
                    modifier = Modifier.align(Alignment.TopStart)
                )
                CornerMarker(
                    up = true, left = true, down = true,
                    color = onBackgroundColor,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
                CornerMarker(
                    right = true, up = true,
                    color = onBackgroundColor,
                    modifier = Modifier.align(Alignment.BottomStart)
                )
                CornerMarker(
                    left = true, up = true, right = true,
                    color = onBackgroundColor,
                    modifier = Modifier.align(Alignment.BottomEnd)
                )

                AsyncImage(
                    model = nowPlaying?.coverart ?: "android.resource://com.snowsnooks.mobile/drawable/placeholder_album",
                    contentDescription = "Album Art",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )
            }

            // Now playing text
            Text(
                text = nowPlaying?.nowplaying ?: "Select a Room",
                style = MaterialTheme.typography.headlineSmall,
                color = onBackgroundColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Current room indicator
            Text(
                text = "Playing: ${currentRoom.name}",
                style = MaterialTheme.typography.bodyLarge,
                color = onBackgroundColor.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Bottom controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play/Pause button
                TVButton(
                    text = if (isPlaying) "⏸ PAUSE" else "▶ PLAY",
                    onClick = {
                        activity.togglePlayback()
                        isPlaying = !isPlaying
                    },
                    modifier = Modifier
                        .width(120.dp)
                        .height(50.dp),
                    textColor = onBackgroundColor
                )

                // Sync button (TV-style)
                Box(modifier = Modifier.size(60.dp)) {
                    CornerMarker(
                        right = true, down = true,
                        color = onBackgroundColor,
                        modifier = Modifier.align(Alignment.TopStart)
                    )
                    CornerMarker(
                        left = true, down = true, up = true,
                        color = onBackgroundColor,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                    CornerMarker(
                        right = true, up = true, left = true,
                        color = onBackgroundColor,
                        modifier = Modifier.align(Alignment.BottomStart)
                    )
                    CornerMarker(
                        left = true, up = true,
                        color = onBackgroundColor,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )

                    TVButton(
                        text = if (syncToTrack) "ᵀᴾ" else "ⴵ",
                        onClick = { syncToTrack = !syncToTrack },
                        modifier = Modifier.fillMaxSize(),
                        textSize = 16,
                        textColor = onBackgroundColor,
                        forceHighlight = syncToTrack
                    )
                }
            }
        }
    }
}

@Composable
fun CornerMarker(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Dp = 14.dp,
    stroke: Dp = 2.dp,
    up: Boolean = false,
    down: Boolean = false,
    left: Boolean = false,
    right: Boolean = false
) {
    Canvas(modifier = modifier.size(size)) {
        val s = size.toPx()
        val t = stroke.toPx()
        val c = s / 2f   // center of the square
        val l = s / 2f   // length of each line from the center

        drawRect(color, topLeft = Offset(c - t/2, c - t/2), size = Size(t, t))

        // vertical lines
        if (up) drawRect(color, topLeft = Offset(c - t / 2, 0f), size = Size(t, c))
        if (down) drawRect(color, topLeft = Offset(c - t / 2, c), size = Size(t, c))

        // horizontal lines
        if (left) drawRect(color, topLeft = Offset(0f, c - t / 2), size = Size(c, t))
        if (right) drawRect(color, topLeft = Offset(c, c - t / 2), size = Size(c, t))
    }
}

@Composable
fun TVButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textSize: Int = 12,
    textAlign: TextAlign = TextAlign.Center,
    textPadding: PaddingValues = PaddingValues(0.dp),
    textColor: Color = Color.White,
    enabled: Boolean = true,
    forceHighlight: Boolean = false
) {
    Box(
        modifier = modifier
            .background(
                if (forceHighlight) MaterialTheme.colorScheme.primary else Color.Transparent
            )
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(0.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = text,
                modifier = Modifier.fillMaxWidth().padding(textPadding),
                fontSize = textSize.sp,
                color = textColor,
                textAlign = textAlign,
                maxLines = 1
            )
        }
    }
}

@Composable
fun TPRadioMobileTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF6200EE),
            onPrimary = Color.White,
            surfaceVariant = Color(0xFFE7E0EC),
            onSurfaceVariant = Color(0xFF49454F)
        ),
        content = content
    )
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    TPRadioMobileTheme {
        // Simple preview without activity
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "TPRadio Mobile Preview",
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}