package org.andrew.jpad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.andrew.jpad.ui.theme.JPadTheme
import java.awt.Cursor

@Composable
fun JPadHorizontalSplitter(
    onDelta: (Float) -> Unit,
    modifier: Modifier = Modifier,
    thickness: Dp = 4.dp
) {
    var isDragging by remember { mutableStateOf(false) }
    val colors = JPadTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .background(if (isDragging) colors.accentPrimary else colors.borderStrong)
            .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR)))
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    onDelta(delta)
                },
                onDragStarted = { isDragging = true },
                onDragStopped = { isDragging = false }
            )
    )
}

@Composable
fun JPadVerticalSplitter(
    onDelta: (Float) -> Unit,
    modifier: Modifier = Modifier,
    thickness: Dp = 4.dp
) {
    var isDragging by remember { mutableStateOf(false) }
    val colors = JPadTheme.colors

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(thickness)
            .background(if (isDragging) colors.accentPrimary else colors.borderStrong)
            .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)))
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    onDelta(delta)
                },
                onDragStarted = { isDragging = true },
                onDragStopped = { isDragging = false }
            )
    )
}
