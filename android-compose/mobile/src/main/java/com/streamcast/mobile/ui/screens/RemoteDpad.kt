package com.streamcast.mobile.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamcast.mobile.ui.theme.StreamCastColors
import com.streamcast.shared.atv.AtvKey
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * A four-way directional pad.
 *
 * ## Why this exists alongside the trackpad
 * A swipe is faster for travelling, but it is a guess: you learn how far you
 * moved only by watching the television. Some things want exactly one step -
 * nudging to the next item in a settings list, moving one square in a grid -
 * and for those a discrete key with an edge under the thumb beats a gesture
 * every time. Both surfaces drive the same key codes; which one is better
 * depends on the task, so the user picks.
 *
 * ## Holding a direction
 * Holding repeats the key rather than sending the protocol's long-press. A
 * long press on Android TV usually means something specific to the focused app
 * - often a context menu - whereas a repeated short press is what the platform
 * itself generates for a held direction, and what list auto-scroll responds to.
 * The first repeat waits [REPEAT_DELAY_MS] so an ordinary tap is never mistaken
 * for a hold.
 */
@Composable
fun DpadSurface(
    enabled: Boolean,
    onKey: (AtvKey) -> Unit,
    onLongSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(StreamCastColors.SurfaceSecondary, StreamCastColors.SurfaceLow)
                )
            )
            .border(
                BorderStroke(1.dp, StreamCastColors.Outline.copy(alpha = 0.6f)),
                CircleShape
            )
            .alpha(if (enabled) 1f else 0.45f),
        contentAlignment = Alignment.Center
    ) {
        DirectionKey(
            icon = Icons.Rounded.KeyboardArrowUp,
            label = "Up",
            key = AtvKey.DPAD_UP,
            enabled = enabled,
            onKey = onKey,
            alignment = Alignment.TopCenter
        )
        DirectionKey(
            icon = Icons.Rounded.KeyboardArrowDown,
            label = "Down",
            key = AtvKey.DPAD_DOWN,
            enabled = enabled,
            onKey = onKey,
            alignment = Alignment.BottomCenter
        )
        DirectionKey(
            icon = Icons.Rounded.KeyboardArrowLeft,
            label = "Left",
            key = AtvKey.DPAD_LEFT,
            enabled = enabled,
            onKey = onKey,
            alignment = Alignment.CenterStart
        )
        DirectionKey(
            icon = Icons.Rounded.KeyboardArrowRight,
            label = "Right",
            key = AtvKey.DPAD_RIGHT,
            enabled = enabled,
            onKey = onKey,
            alignment = Alignment.CenterEnd
        )

        SelectKey(enabled = enabled, onSelect = { onKey(AtvKey.DPAD_CENTER) }, onLongSelect = onLongSelect)
    }
}

@Composable
private fun BoxScope.DirectionKey(
    icon: ImageVector,
    label: String,
    key: AtvKey,
    enabled: Boolean,
    onKey: (AtvKey) -> Unit,
    alignment: Alignment
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.88f else 1f, label = "dpadPress")

    Box(
        modifier = Modifier
            .align(alignment)
            .padding(6.dp)
            .size(76.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                if (pressed) StreamCastColors.PrimaryViolet.copy(alpha = 0.3f) else Color.Transparent
            )
            .pointerInput(enabled, key) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        pressed = true
                        view.tick()
                        onKey(key)

                        // Repeat while held. Cancelled as soon as the finger
                        // lifts, so a tap sends exactly one key.
                        val repeat: Job = scope.launch {
                            delay(REPEAT_DELAY_MS)
                            while (isActive) {
                                onKey(key)
                                view.tick()
                                delay(REPEAT_INTERVAL_MS)
                            }
                        }
                        try {
                            tryAwaitRelease()
                        } finally {
                            repeat.cancel()
                            pressed = false
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = StreamCastColors.TextPrimary,
            modifier = Modifier.size(34.dp)
        )
    }
}

@Composable
private fun BoxScope.SelectKey(
    enabled: Boolean,
    onSelect: () -> Unit,
    onLongSelect: () -> Unit
) {
    val view = LocalView.current
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, label = "okPress")

    Box(
        modifier = Modifier
            .align(Alignment.Center)
            .size(104.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                if (pressed) StreamCastColors.PrimaryViolet else StreamCastColors.SurfaceHighest
            )
            .border(1.dp, StreamCastColors.Outline.copy(alpha = 0.8f), CircleShape)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        pressed = true
                        try {
                            tryAwaitRelease()
                        } finally {
                            pressed = false
                        }
                    },
                    onTap = {
                        view.tick(strong = true)
                        onSelect()
                    },
                    onLongPress = {
                        view.tick(strong = true)
                        onLongSelect()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "OK",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}

/** Switches between the gesture pad and the button pad. */
@Composable
fun PadModeToggle(
    trackpadSelected: Boolean,
    onSelect: (trackpad: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(StreamCastColors.SurfaceLow)
            .padding(3.dp)
    ) {
        ToggleHalf(
            label = "Trackpad",
            selected = trackpadSelected,
            onClick = { onSelect(true) },
            modifier = Modifier.weight(1f)
        )
        ToggleHalf(
            label = "D-pad",
            selected = !trackpadSelected,
            onClick = { onSelect(false) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ToggleHalf(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) StreamCastColors.SurfaceHighest else Color.Transparent)
            .pointerInput(selected) {
                detectTapGestures(onTap = {
                    view.tick()
                    onClick()
                })
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else StreamCastColors.TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/** A tap must not be read as a hold, so the first repeat waits this long. */
private const val REPEAT_DELAY_MS = 420L

/** Roughly the platform's own key-repeat rate for a held direction. */
private const val REPEAT_INTERVAL_MS = 130L
