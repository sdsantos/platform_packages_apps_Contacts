package com.android.contacts.ui.search.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalInspectionMode

private const val EXPAND_DURATION_MILLIS = 250
private const val COLLAPSE_DURATION_MILLIS = 175

@Stable
internal class SearchTransitionState(
    initialProgress: Float,
) {

    private val animatable = Animatable(initialProgress)

    val progress: Float
        get() = animatable.value

    var isClosing by mutableStateOf(false)
        private set

    suspend fun expand() {
        animatable.animateTo(
            targetValue = 1f,
            animationSpec = tween(EXPAND_DURATION_MILLIS, easing = FastOutSlowInEasing),
        )
    }

    suspend fun collapse() {
        isClosing = true
        animatable.animateTo(
            targetValue = 0f,
            animationSpec = tween(COLLAPSE_DURATION_MILLIS, easing = FastOutSlowInEasing),
        )
    }
}

@Composable
internal fun rememberSearchTransitionState(): SearchTransitionState {
    val isPreview = LocalInspectionMode.current
    var hasExpanded by rememberSaveable { mutableStateOf(isPreview) }
    val state = remember { SearchTransitionState(if (hasExpanded) 1f else 0f) }

    LaunchedEffect(state) {
        if (!hasExpanded) {
            state.expand()
            hasExpanded = true
        }
    }

    return state
}
