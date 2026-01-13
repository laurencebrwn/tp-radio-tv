package com.snowsnooks.tpradio

import CornerMarker
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import coil.compose.AsyncImage
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.tv.material3.MaterialTheme
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import android.view.ViewGroup
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun RadioScreen(
    currentRoom: RadioRoom?,
    nowPlaying: NowPlayingResponse?,
    currentSchemeIndex: Int,
    transitionProgress: Float,
    onBackgroundColor: androidx.compose.ui.graphics.Color,
    primaryColor: androidx.compose.ui.graphics.Color,
    interpolatedBackgroundColor: androidx.compose.ui.graphics.Color,
    syncToTrack: Boolean,
    isPaused: Boolean,
    isCurrentlyPlaying: Boolean,
    isTransitioning: Boolean,
    onToggleSync: () -> Unit,
    onPlayRoom1: () -> Unit,
    onPlayRoom2: () -> Unit,
    videoOverlayAlpha: Float = 0f
) {
    Row(modifier = Modifier.fillMaxSize()) {

        /* ───────────── LEFT PANEL (1/6) ───────────── */
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(185.dp)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── TOP: Artwork
            Box {
                AsyncImage(
                    model = nowPlaying?.coverart ?: R.drawable.placeholder_album,
                    contentDescription = null,
                    modifier = Modifier
                        .size(140.dp)
                        .padding(16.dp)
                )
                // Top-left corner
                CornerMarker(
                    right = true, down = true,
                    color = onBackgroundColor,
                    modifier = Modifier.align(Alignment.TopStart)
                )

                // Top-right corner
                CornerMarker(
                    left = true, down = true, right = true,
                    color = onBackgroundColor,
                    modifier = Modifier.align(Alignment.TopEnd)
                )

                // Bottom-left corner
                CornerMarker(
                    right = true, up = true, down = true,
                    color = onBackgroundColor,
                    modifier = Modifier.align(Alignment.BottomStart)
                )

                // Bottom-right corner
                CornerMarker(
                    left = true, up = true, down = true,
                    color = onBackgroundColor,
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }


            // ── MIDDLE: Now playing
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .padding(8.dp)
            ) {
                VerticalMarquee(
                    text = nowPlaying?.nowplaying ?: "Select a Room",
                    textColor = onBackgroundColor,
                    backgroundColor = interpolatedBackgroundColor
                )
            }

            // ── BOTTOM: Buttons
            Box(
                modifier = Modifier
                    .fillMaxWidth()  // 👈 frame size
            ) {

                // ── Frame corners
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

                // ── Actual buttons (smaller)
                Column(
                    modifier = Modifier
                        .padding(top = 20.dp, bottom = 20.dp)        // 👈 space from frame
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TVButton(
                        text = when {
                            currentRoom?.name == "Room 1" && !isCurrentlyPlaying -> "ROOM 1   II"
                            currentRoom?.name == "Room 1" && isCurrentlyPlaying -> "ROOM 1   ▶"
                            else -> "ROOM 1"
                        },
                        onClick = onPlayRoom1,
                        modifier = Modifier
                            .width(100.dp)
                            .padding(bottom = 8.dp),
                        textAlign = TextAlign.Left,
                        textPadding = PaddingValues(start = 15.dp),
                        textColor = interpolatedBackgroundColor
                    )

                    TVButton(
                        text = when {
                            currentRoom?.name == "Room 2" && !isCurrentlyPlaying -> "ROOM 2   II"
                            currentRoom?.name == "Room 2" && isCurrentlyPlaying -> "ROOM 2   ▶"
                            else -> "ROOM 2"
                        },
                        onClick = onPlayRoom2,
                        modifier = Modifier.width(100.dp),
                        textAlign = TextAlign.Left,
                        textPadding = PaddingValues(start = 15.dp),
                        textColor = interpolatedBackgroundColor
                    )
                }

            }



        }

        /* ───────────── RIGHT PANEL (5/6) ───────────── */
        /* ───────────── RIGHT PANEL (5/6) ───────────── */
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(5f)
                .padding(16.dp)
        ) {
            // Video in center with its own overlay
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(end = 16.dp)
            ) {
                val videoFiles = listOf(
                    "video_1.mp4",
                    "video_2.mp4",
                    "video_3.mp4",
                    "video_4.mp4",
                    "video_5.mp4"
                )

                VideoPlayer(
                    videoFileName = videoFiles[currentSchemeIndex],
                    modifier = Modifier.size(300.dp)
                )

                // Video-only overlay
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .background(
                            primaryColor.copy(
                                alpha = videoOverlayAlpha // Pass this as a parameter to RadioScreen
                            )
                        )
                )
            }

            // Sync button in bottom right with corner markers
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(0.dp)
            ) {
                // Corner markers around button
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

                // Button centered in corner markers
                Box(
                    modifier = Modifier
                        .padding(20.dp)
                        .align(Alignment.Center)
                ) {
                    TVButton(
                        text = if (syncToTrack) "ᵀᴾ" else "ⴵ",
                        onClick = onToggleSync,
                        modifier = Modifier
                            .width(25.dp)
                            .height(25.dp),
                        textSize = 12,
                        textColor = interpolatedBackgroundColor,
                        enabled = !isTransitioning,
                        forceHighlight = syncToTrack
                    )
                }
            }
            CornerMarker(
                left = true, down = true,
                color = onBackgroundColor,
                modifier = Modifier.align(Alignment.TopEnd)
            )
            CornerMarker(
                left = true, up = true,
                color = onBackgroundColor,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }

    }
}
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TVButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textSize: Int = 12,
    textAlign: TextAlign = TextAlign.Center,
    textPadding: PaddingValues = PaddingValues(0.dp),
    textColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.background,
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
            shape = ButtonDefaults.shape(
                shape = RoundedCornerShape(0.dp)
            ),
            contentPadding = PaddingValues(
                horizontal = 0.dp,
                vertical = 0.dp
            ),
            scale = ButtonDefaults.scale(
                focusedScale = 1f,
                pressedScale = 1f
            )
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


@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun VerticalMarquee(
    text: String,
    modifier: Modifier = Modifier,
    durationMillis: Int = 8000,
    spacing: Float = 40f,
    textColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onBackground,
    backgroundColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.background
) {
    val textWidthPx = remember { mutableStateOf(0f) }
    val containerHeightPx = remember { mutableStateOf(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "marquee")

    // Distance between text copies
    val repeatDistance = textWidthPx.value + spacing

    val offsetX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -repeatDistance,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = durationMillis,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )

    Box(
        modifier = modifier
            .clipToBounds()
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                containerHeightPx.value = coordinates.size.height.toFloat()
            },
        contentAlignment = Alignment.Center
    ) {
        // Scrolling text
        BasicText(
            text = "$text ᵀᴾ ",
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 16.sp,
                color = textColor
            ),
            softWrap = false,
            maxLines = 1,
            modifier = Modifier
                .wrapContentWidth(unbounded = true)
                .rotate(-90f)
                .graphicsLayer {
                    translationX = offsetX - containerHeightPx.value / 2
                }
                .onGloballyPositioned { coordinates ->
                    textWidthPx.value = coordinates.size.width.toFloat()
                }
        )

        BasicText(
            text = "$text ᵀᴾ ",
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 16.sp,
                color = textColor
            ),
            softWrap = false,
            maxLines = 1,
            modifier = Modifier
                .wrapContentWidth(unbounded = true)
                .rotate(-90f)
                .graphicsLayer {
                    translationX = offsetX - containerHeightPx.value / 2 + repeatDistance
                }
        )

        BasicText(
            text = "$text ᵀᴾ ",
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 16.sp,
                color = textColor
            ),
            softWrap = false,
            maxLines = 1,
            modifier = Modifier
                .wrapContentWidth(unbounded = true)
                .rotate(-90f)
                .graphicsLayer {
                    translationX = offsetX - containerHeightPx.value / 2 + (repeatDistance * 2)
                }
        )

        BasicText(
            text = "$text ᵀᴾ ",
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 16.sp,
                color = textColor
            ),
            softWrap = false,
            maxLines = 1,
            modifier = Modifier
                .wrapContentWidth(unbounded = true)
                .rotate(-90f)
                .graphicsLayer {
                    translationX = offsetX - containerHeightPx.value / 2 + (repeatDistance * 3)
                }
        )

        // Top fade
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .align(Alignment.TopCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            backgroundColor,
                            Color.Transparent
                        )
                    )
                )
        )

        // Bottom fade
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            backgroundColor
                        )
                    )
                )
        )
    }
}

@Composable
fun VideoPlayer(
    videoFileName: String, // e.g. "video1.mp4"
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
    LaunchedEffect(videoFileName) {
        val assetUri = "asset:///$videoFileName"
        exoPlayer.setMediaItem(MediaItem.fromUri(assetUri))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

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
}
