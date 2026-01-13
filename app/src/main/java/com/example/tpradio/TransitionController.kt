package com.example.tpradio

import androidx.compose.runtime.*
import kotlinx.coroutines.delay

class TransitionController {
    var overlayAlpha by mutableFloatStateOf(0f)
        private set

    var isTransitioning by mutableStateOf(false)
        private set

    suspend fun executeTransition(onSwitchContent: () -> Unit) {
        if (isTransitioning) return
        isTransitioning = true

        // Fade to background (500ms)
        animateAlpha(from = 0f, to = 1f, durationMs = 500)

        // Switch theme/video while covered
        onSwitchContent()

        // Stay covered a bit longer (1000ms)
        delay(1000)

        // Fade from background (500ms)
        animateAlpha(from = 1f, to = 0f, durationMs = 500)

        isTransitioning = false
    }

    private suspend fun animateAlpha(from: Float, to: Float, durationMs: Long) {
        val steps = 25
        val delayPerStep = durationMs / steps
        repeat(steps) { step ->
            overlayAlpha = from + (to - from) * (step + 1) / steps
            delay(delayPerStep)
        }
    }
}