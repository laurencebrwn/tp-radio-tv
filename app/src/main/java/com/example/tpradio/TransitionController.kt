package com.example.tpradio

import androidx.compose.runtime.*
import kotlinx.coroutines.delay

class TransitionController {
    var overlayAlpha by mutableFloatStateOf(0f)
        private set

    var backgroundAlpha by mutableFloatStateOf(0f)
        private set

    var isTransitioning by mutableStateOf(false)
        private set

    var transitionProgress by mutableFloatStateOf(1f)
        private set

    var previousSchemeIndex by mutableIntStateOf(0)
    var currentSchemeIndex by mutableIntStateOf(0)

    suspend fun executeTransition(newSchemeIndex: Int, onSwitchContent: () -> Unit) {
        if (isTransitioning) return
        isTransitioning = true

        // Store previous scheme and set new target
        previousSchemeIndex = currentSchemeIndex
        currentSchemeIndex = newSchemeIndex

        // Reset transition progress to start from previous colors
        transitionProgress = 0f

        // 1. Fade video out (500ms)
        animateAlpha(from = 0f, to = 1f, durationMs = 500)

        // 2. Change colors while video is faded (500ms)
        animateTransitionProgress(from = 0f, to = 1f, durationMs = 500)

        // Switch theme/video while faded
        onSwitchContent()

        // Stay faded briefly (500ms)
        delay(500)

        // 3. Fade new video in (500ms)
        animateAlpha(from = 1f, to = 0f, durationMs = 500)

        // Ensure everything is in final state
        transitionProgress = 1f

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

    private suspend fun animateBothAlphas(overlayFrom: Float, overlayTo: Float, backgroundFrom: Float, backgroundTo: Float, durationMs: Long) {
        val steps = 25
        val delayPerStep = durationMs / steps
        repeat(steps) { step ->
            val progress = (step + 1).toFloat() / steps
            overlayAlpha = overlayFrom + (overlayTo - overlayFrom) * progress
            backgroundAlpha = backgroundFrom + (backgroundTo - backgroundFrom) * progress
            delay(delayPerStep)
        }
    }

    private suspend fun animateTransitionProgress(from: Float, to: Float, durationMs: Long) {
        val steps = 25
        val delayPerStep = durationMs / steps
        repeat(steps) { step ->
            val progress = (step + 1).toFloat() / steps
            transitionProgress = from + (to - from) * progress
            delay(delayPerStep)
        }
    }
}