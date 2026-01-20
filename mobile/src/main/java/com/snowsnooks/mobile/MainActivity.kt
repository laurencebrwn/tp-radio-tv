package com.snowsnooks.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import android.content.ComponentName
import android.graphics.Paint
import android.graphics.Typeface
import android.view.ViewGroup
import coil.compose.AsyncImage
import com.snowsnooks.core.domain.RadioRoom
import com.snowsnooks.core.domain.NowPlayingResponse
import com.snowsnooks.core.data.RadioApiRepository
import com.snowsnooks.core.playback.MediaPlaybackService
import androidx.compose.animation.core.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas

class MainActivity : ComponentActivity() {

    private lateinit var mediaController: MediaController
    private val radioRepository = RadioApiRepository()
    private var pendingPlayRoom: RadioRoom? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Connect to MediaPlaybackService
        val sessionToken = SessionToken(this, ComponentName(this, MediaPlaybackService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()

        controllerFuture.addListener({
            mediaController = controllerFuture.get()

            // Add listener to sync internal state with controller state changes
            mediaController.addListener(object : androidx.media3.common.Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    // Handle playback state changes
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    // Handle playing state changes
                }
            })
        }, { runnable -> runnable.run() })

        setContent {
            TPRadioMobileTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets(0)),
                    shape = RectangleShape
                ) {
                    MobileRadioApp()
                }
            }
        }
    }
}

@Composable
fun MobileRadioApp() {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    // Calculate responsive sizes
    val albumArtSize = (screenWidth * 0.6f).coerceAtMost(280.dp).coerceAtLeast(200.dp)
    val videoSize = (screenHeight * 0.3f).coerceAtMost(300.dp).coerceAtLeast(200.dp)
    val buttonSize = (screenWidth * 0.15f).coerceAtMost(60.dp).coerceAtLeast(40.dp)
    val padding = (screenWidth * 0.04f).coerceAtMost(16.dp).coerceAtLeast(8.dp)

    // State management
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val rooms = listOf(
        RadioRoom("Room 1", "https://radio.tranceparty.net:2020/stream/room-1", "https://radio.tranceparty.net:2020/json/stream/room-1"),
        RadioRoom("Room 2", "https://radio.tranceparty.net:2020/stream/room-2", "https://radio.tranceparty.net:2020/json/stream/room-2")
    )

    var currentRoom by remember { mutableStateOf(rooms[pagerState.currentPage]) }
    var nowPlaying by remember { mutableStateOf<NowPlayingResponse?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var showThemeToggle by remember { mutableStateOf(false) }

    // Auto-fetch now playing data
    LaunchedEffect(currentRoom) {
        while (true) {
            // TODO: Integrate with repository
            kotlinx.coroutines.delay(10000)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Ambient video background (subtle)
        AmbientVideoBackground(
            videoIndex = pagerState.currentPage,
            modifier = Modifier.fillMaxSize().alpha(0.2f)
        )

        // Main content
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top room selector
            RoomSelector(
                rooms = rooms,
                currentPage = pagerState.currentPage,
                onRoomSelected = { page ->
                    // TODO: Animate to page
                },
                onSettingsClick = { showThemeToggle = !showThemeToggle },
                modifier = Modifier.padding(top = padding * 2)
            )

            Spacer(modifier = Modifier.weight(0.5f))

            // Album art
            AlbumArtDisplay(
                nowPlaying = nowPlaying,
                size = albumArtSize,
                modifier = Modifier.padding(padding)
            )

            // Now playing marquee
            NowPlayingMarquee(
                text = nowPlaying?.nowplaying ?: "Select a Room",
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(40.dp)
                    .padding(vertical = padding)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Bottom playback controls
            PlaybackControls(
                isPlaying = isPlaying,
                onPlayPause = {
                    isPlaying = !isPlaying
                    // TODO: Connect to media controller
                },
                modifier = Modifier.padding(bottom = padding * 2)
            )
        }

        // Theme toggle overlay (if enabled)
        if (showThemeToggle) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text("Theme Toggle Coming Soon", color = Color.White)
            }
        }
    }
}

@Composable
fun RoomSelector(
    rooms: List<RadioRoom>,
    currentPage: Int,
    onRoomSelected: (Int) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        rooms.forEachIndexed { index, room ->
            Button(
                onClick = { onRoomSelected(index) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (index == currentPage)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.width(100.dp)
            ) {
                Text(
                    text = room.name,
                    color = if (index == currentPage)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        IconButton(onClick = onSettingsClick) {
            Icon(Icons.Default.Settings, contentDescription = "Settings")
        }
    }
}

@Composable
fun AlbumArtDisplay(
    nowPlaying: NowPlayingResponse?,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
    ) {
        AsyncImage(
            model = nowPlaying?.coverart ?: "android.resource://com.snowsnooks.mobile/drawable/placeholder_album",
            contentDescription = "Album Art",
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun NowPlayingMarquee(
    text: String,
    modifier: Modifier = Modifier
) {
    val textWidthPx = remember { mutableStateOf(0f) }
    val containerWidthPx = remember { mutableStateOf(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "marquee")
    val offsetX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -textWidthPx.value,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )

    Box(
        modifier = modifier
            .clipToBounds()
            .onGloballyPositioned { coordinates ->
                containerWidthPx.value = coordinates.size.width.toFloat()
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val paint = Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 16.sp.toPx()
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.LEFT
            }

            val textToDraw = "$text • "
            val textWidth = paint.measureText(textToDraw)

            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(
                    textToDraw,
                    offsetX,
                    size.height / 2 + paint.textSize / 3,
                    paint
                )

                // Draw second copy for seamless loop
                if (offsetX < -textWidth + containerWidthPx.value) {
                    canvas.nativeCanvas.drawText(
                        textToDraw,
                        offsetX + textWidth,
                        size.height / 2 + paint.textSize / 3,
                        paint
                    )
                }
            }
        }
    }
}

@Composable
fun PlaybackControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onPlayPause,
        modifier = modifier.size(72.dp),
        containerColor = MaterialTheme.colorScheme.primary
    ) {
        Text(
            text = if (isPlaying) "⏸️" else "▶️",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

@Composable
fun AmbientVideoBackground(
    videoIndex: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f // Muted
        }
    }

    // Update media when video changes
    LaunchedEffect(videoIndex) {
        try {
            val videoResId = context.resources.getIdentifier(
                "video_${(videoIndex % 5) + 1}",
                "raw",
                context.packageName
            )

            if (videoResId != 0) {
                val videoUri = "android.resource://${context.packageName}/$videoResId"
                val mediaItem = MediaItem.fromUri(videoUri)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            } else {
                // Video not found, just show colored background
                exoPlayer.stop()
            }
        } catch (e: Exception) {
            // Handle video loading errors gracefully
            exoPlayer.stop()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Only show video if available, otherwise show gradient background
    val videoResId = context.resources.getIdentifier(
        "video_${(videoIndex % 5) + 1}",
        "raw",
        context.packageName
    )

    if (videoResId != 0) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = modifier
        )
    } else {
        // Fallback: gradient background when video not available
        Box(
            modifier = modifier.background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1a1a2e),
                        Color(0xFF16213e),
                        Color(0xFF0f3460)
                    )
                )
            )
        )
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
        MobileRadioApp()
    }
}