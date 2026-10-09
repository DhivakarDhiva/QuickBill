/*
 * QuickBill + QuickKitchen
 *
 * Author: Dhivakar
 * Role: Android Developer
 *
 * Copyright (c) 2026 Dhivakar
 *
 * This file is part of the QuickBill + QuickKitchen project.
 * The original implementation and modifications in this file were
 * created by Dhivakar for the project/assignment.
 *
 * QuickBill-QuickKitchen-Author: Dhivakar
 *
 * Do not remove or alter this attribution notice.
 */

package com.quickbill.pos.ui.theme

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * FocusDesk-style Motion System mapping SwiftUI physics-based springs and smooth easing
 * to Jetpack Compose animations.
 */
object SwiftUiMotion {

    /**
     * Matches SwiftUI `.animation(.snappy, value:)`
     */
    fun <T> snappy(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.82f,
        stiffness = 380f
    )

    /**
     * Matches SwiftUI `.animation(.smooth, value:)`
     */
    fun <T> smooth(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.90f,
        stiffness = 220f
    )

    /**
     * Matches SwiftUI `.animation(.bouncy, value:)`
     */
    fun <T> bouncy(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.65f,
        stiffness = 300f
    )

    /**
     * Matches SwiftUI `.animation(.easeInOut(duration: 0.3))`
     */
    fun <T> easeInOut(durationMillis: Int = 300): FiniteAnimationSpec<T> = tween(
        durationMillis = durationMillis,
        easing = FastOutSlowInEasing
    )

    /**
     * Screen Transition (Fade + Subtle Scale)
     */
    val ScreenTransition: ContentTransform =
        (fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing)) +
                scaleIn(initialScale = 0.96f, animationSpec = snappy<Float>()))
            .togetherWith(
                fadeOut(animationSpec = tween(180)) +
                        scaleOut(targetScale = 1.02f, animationSpec = snappy<Float>())
            )

    /**
     * Navigation Slide Transitions (Bottom-to-Top slide + Fade with Spring physics)
     */
    val NavEnterTransition: EnterTransition =
        slideInVertically(initialOffsetY = { it / 6 }, animationSpec = snappy<IntOffset>()) +
                fadeIn(animationSpec = tween(240))

    val NavExitTransition: ExitTransition =
        slideOutVertically(targetOffsetY = { -it / 8 }, animationSpec = snappy<IntOffset>()) +
                fadeOut(animationSpec = tween(180))

    val NavPopEnterTransition: EnterTransition =
        slideInVertically(initialOffsetY = { -it / 8 }, animationSpec = snappy<IntOffset>()) +
                fadeIn(animationSpec = tween(240))

    val NavPopExitTransition: ExitTransition =
        slideOutVertically(targetOffsetY = { it / 6 }, animationSpec = snappy<IntOffset>()) +
                fadeOut(animationSpec = tween(180))

    /**
     * Bottom Sheet & Modal Transitions
     */
    val ModalSlideIn: EnterTransition =
        slideInVertically(initialOffsetY = { it }, animationSpec = snappy<IntOffset>()) +
                fadeIn(animationSpec = tween(250))

    val ModalSlideOut: ExitTransition =
        slideOutVertically(targetOffsetY = { it }, animationSpec = snappy<IntOffset>()) +
                fadeOut(animationSpec = tween(200))

    val ModalEnterTransition: EnterTransition get() = ModalSlideIn
    val ModalExitTransition: ExitTransition get() = ModalSlideOut
}

/**
 * Modifier for staggered entrance layout animations, arranging elements sequentially
 * with spring physics when each screen launches.
 */
fun Modifier.staggeredEntrance(
    index: Int = 0,
    baseDelayMs: Long = 45L
): Modifier = composed {
    val anim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(index * baseDelayMs)
        anim.animateTo(
            targetValue = 1f,
            animationSpec = SwiftUiMotion.snappy()
        )
    }

    this.graphicsLayer {
        val progress = anim.value
        translationY = (1f - progress) * 36.dp.toPx()
        scaleX = 0.92f + (0.08f * progress)
        scaleY = 0.92f + (0.08f * progress)
        alpha = progress.coerceIn(0f, 1f)
    }
}
