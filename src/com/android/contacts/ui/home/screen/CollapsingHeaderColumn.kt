package com.android.contacts.ui.home.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt

@Composable
internal fun CollapsingHeaderColumn(
    header: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    state: CollapsingHeaderState = remember { CollapsingHeaderState() },
    content: @Composable () -> Unit,
) {
    Layout(
        contents = listOf(header, content),
        modifier = modifier
            .clipToBounds()
            .nestedScroll(state.nestedScrollConnection),
    ) { (headerMeasurables, contentMeasurables), constraints ->
        val looseConstraints = constraints.copy(minHeight = 0)
        val headerPlaceables = headerMeasurables.map {
            it.measure(looseConstraints.copy(maxHeight = Constraints.Infinity))
        }
        val headerHeight = headerPlaceables.maxOfOrNull { it.height } ?: 0
        state.headerHeight = headerHeight.toFloat()

        val headerOffset = state.headerOffset.coerceIn(-headerHeight.toFloat(), 0f).roundToInt()
        val contentTop = headerHeight + headerOffset
        val contentPlaceables = contentMeasurables.map {
            it.measure(
                looseConstraints.copy(
                    maxHeight = (constraints.maxHeight - contentTop).coerceAtLeast(0),
                ),
            )
        }

        layout(constraints.maxWidth, constraints.maxHeight) {
            headerPlaceables.forEach { it.placeRelative(0, headerOffset) }
            contentPlaceables.forEach { it.placeRelative(0, contentTop) }
        }
    }
}

@Stable
internal class CollapsingHeaderState {
    var headerOffset by mutableFloatStateOf(0f)
        private set

    internal var headerHeight = 0f

    fun show() {
        headerOffset = 0f
    }

    internal val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val currentOffset = headerOffset.coerceIn(-headerHeight, 0f)
            val newOffset = (currentOffset + available.y).coerceIn(-headerHeight, 0f)
            headerOffset = newOffset
            return Offset(0f, newOffset - currentOffset)
        }
    }
}
