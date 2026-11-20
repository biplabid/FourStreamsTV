package com.streamcast.mobile.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamcast.mobile.ui.theme.StreamCastColors
import com.streamcast.shared.atv.AtvAppLink
import com.streamcast.shared.atv.AtvKey
import com.streamcast.shared.atv.RemoteProto
import kotlin.math.abs
import kotlinx.coroutines.launch

/**
 * The swipe surface.
 *
 * ## Why a trackpad rather than only a D-pad
 * A TV interface is a grid you travel across, and travelling four rows down a
 * grid means four deliberate taps on a button the size of a fingernail, each
 * one needing the eye to come back down to the phone. A swipe is one gesture
 * with the eye still on the television, and holding the drag keeps emitting
 * directions - so a long journey across a row costs one movement.
 *
 * Directions are emitted per [STEP_PX] of travel rather than once per gesture,
 * which is what makes a continuous drag scroll continuously. Each emission is
 * confirmed with a haptic tick, because without that feedback there is no way
 * to tell how many steps the drag actually sent.
 */
@Composable
fun TrackpadSurface(
    enabled: Boolean,
    onDirection: (AtvKey) -> Unit,
    onSelect: () -> Unit,
    onLongSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    // A ripple that follows the finger, so the pad feels alive under the thumb.
    val rippleAlpha = remember { Animatable(0f) }
    var rippleCentre by remember { mutableStateOf(Offset.Zero) }

    fun pulse(at: Offset) {
        rippleCentre = at
        scope.launch {
            rippleAlpha.snapTo(0.5f)
            rippleAlpha.animateTo(0f, tween(420, easing = FastOutSlowInEasing))
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.verticalGradient(
                    listOf(StreamCastColors.SurfaceSecondary, StreamCastColors.SurfaceLow)
                )
            )
            .border(
                BorderStroke(1.dp, StreamCastColors.Outline.copy(alpha = 0.6f)),
                RoundedCornerShape(32.dp)
            )
            .alpha(if (enabled) 1f else 0.45f)
            .then(
                if (!enabled) Modifier else Modifier
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { at ->
                                pulse(at)
                                view.tick(strong = true)
                                onSelect()
                            },
                            onLongPress = { at ->
                                pulse(at)
                                view.tick(strong = true)
                                onLongSelect()
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        // Travel is accumulated across the whole drag and spent
                        // one step at a time, so a fast flick sends several
                        // moves and a slow drag sends them as the finger passes
                        // each threshold.
                        var travelX = 0f
                        var travelY = 0f

                        detectDragGestures(
                            onDragStart = { at -> pulse(at) },
                            onDragEnd = { travelX = 0f; travelY = 0f },
                            onDragCancel = { travelX = 0f; travelY = 0f }
                        ) { change, drag ->
                            change.consume()
                            travelX += drag.x
                            travelY += drag.y

                            // One axis at a time: a diagonal smear should pick
                            // the direction the user meant, not fire both.
                            if (abs(travelX) >= STEP_PX || abs(travelY) >= STEP_PX) {
                                val horizontal = abs(travelX) > abs(travelY)
                                val key = when {
                                    horizontal && travelX > 0 -> AtvKey.DPAD_RIGHT
                                    horizontal -> AtvKey.DPAD_LEFT
                                    travelY > 0 -> AtvKey.DPAD_DOWN
                                    else -> AtvKey.DPAD_UP
                                }
                                onDirection(key)
                                view.tick()
                                if (horizontal) travelX = 0f else travelY = 0f
                            }
                        }
                    }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Touch ripple
        if (rippleAlpha.value > 0f) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .graphicsLayer {
                        translationX = rippleCentre.x - size.width / 2f
                        translationY = rippleCentre.y - size.height / 2f
                        alpha = rippleAlpha.value
                    }
                    .background(
                        Brush.radialGradient(
                            listOf(StreamCastColors.PrimaryViolet.copy(alpha = 0.55f), Color.Transparent)
                        ),
                        CircleShape
                    )
            )
        }

        // Direction hints, deliberately quiet: they teach the gesture once and
        // then get out of the way.
        DirectionHint(Icons.Rounded.KeyboardArrowUp, Alignment.TopCenter)
        DirectionHint(Icons.Rounded.KeyboardArrowDown, Alignment.BottomCenter)
        DirectionHint(Icons.Rounded.KeyboardArrowLeft, Alignment.CenterStart)
        DirectionHint(Icons.Rounded.KeyboardArrowRight, Alignment.CenterEnd)

        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(StreamCastColors.SurfaceHighest.copy(alpha = 0.55f))
                .border(1.dp, StreamCastColors.Outline.copy(alpha = 0.8f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "OK",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.DirectionHint(
    icon: ImageVector,
    alignment: Alignment
) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = StreamCastColors.TextSecondary.copy(alpha = 0.35f),
        modifier = Modifier
            .align(alignment)
            .padding(10.dp)
            .size(26.dp)
    )
}

/** Back / Home / Menu, the three keys every Android TV screen depends on. */
@Composable
fun NavigationRow(
    enabled: Boolean,
    onKey: (AtvKey) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RemoteButton(
            icon = Icons.Rounded.ArrowBack,
            label = "Back",
            enabled = enabled,
            onClick = { onKey(AtvKey.BACK) },
            modifier = Modifier.weight(1f)
        )
        RemoteButton(
            icon = Icons.Rounded.Home,
            label = "Home",
            enabled = enabled,
            onClick = { onKey(AtvKey.HOME) },
            modifier = Modifier.weight(1f)
        )
        RemoteButton(
            icon = Icons.Rounded.Menu,
            label = "Menu",
            enabled = enabled,
            onClick = { onKey(AtvKey.MENU) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun TransportRow(
    enabled: Boolean,
    onKey: (AtvKey) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RemoteButton(
            icon = Icons.Rounded.FastRewind,
            label = "Rewind",
            enabled = enabled,
            onClick = { onKey(AtvKey.MEDIA_REWIND) },
            modifier = Modifier.weight(1f)
        )
        RemoteButton(
            icon = Icons.Rounded.PlayArrow,
            label = "Play / pause",
            enabled = enabled,
            prominent = true,
            onClick = { onKey(AtvKey.MEDIA_PLAY_PAUSE) },
            modifier = Modifier.weight(1f)
        )
        RemoteButton(
            icon = Icons.Rounded.FastForward,
            label = "Fast forward",
            enabled = enabled,
            onClick = { onKey(AtvKey.MEDIA_FAST_FORWARD) },
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Volume, showing the TV's own level.
 *
 * The bar is a readout, not a slider. The protocol lets the TV tell us its
 * volume but gives no way to set it to an arbitrary value - only step up and
 * down - so a draggable control would promise something it cannot deliver.
 * What it does do is track the physical remote: press volume on that and this
 * bar moves.
 */
@Composable
fun VolumeControl(
    enabled: Boolean,
    volume: RemoteProto.VolumeInfo?,
    onKey: (AtvKey) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RemoteButton(
                icon = Icons.Rounded.VolumeDown,
                label = "Volume down",
                enabled = enabled,
                onClick = { onKey(AtvKey.VOLUME_DOWN) },
                modifier = Modifier.weight(1f)
            )
            RemoteButton(
                icon = if (volume?.muted == true) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                label = "Mute",
                enabled = enabled,
                tint = if (volume?.muted == true) StreamCastColors.Error else null,
                onClick = { onKey(AtvKey.VOLUME_MUTE) },
                modifier = Modifier.weight(1f)
            )
            RemoteButton(
                icon = Icons.Rounded.VolumeUp,
                label = "Volume up",
                enabled = enabled,
                onClick = { onKey(AtvKey.VOLUME_UP) },
                modifier = Modifier.weight(1f)
            )
        }

        // Only shown once the TV has actually reported a level; an empty bar
        // would read as "volume is zero" rather than "not known yet".
        AnimatedVisibility(
            visible = volume != null && volume.max > 0,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            val info = volume ?: return@AnimatedVisibility
            val fraction = (info.level.toFloat() / info.max.toFloat()).coerceIn(0f, 1f)

            Column(modifier = Modifier.padding(top = 12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(StreamCastColors.SurfaceHighest)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (info.muted) 0f else fraction)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        StreamCastColors.PrimaryViolet,
                                        StreamCastColors.SecondaryCyan
                                    )
                                )
                            )
                    )
                }
                Text(
                    text = if (info.muted) "Muted" else "${info.level} of ${info.max}",
                    color = StreamCastColors.TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

/**
 * Search, keyboard and the app grid.
 *
 * Voice lives on its own hold-to-talk control rather than here, because it is
 * the one action with a duration.
 */
@Composable
fun InputRow(
    enabled: Boolean,
    onSearch: () -> Unit,
    onKeyboard: () -> Unit,
    onAllApps: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RemoteButton(
            icon = Icons.Rounded.Search,
            label = "Search on TV",
            enabled = enabled,
            onClick = onSearch,
            modifier = Modifier.weight(1f)
        )
        RemoteButton(
            icon = Icons.Rounded.Keyboard,
            label = "Type on TV",
            enabled = enabled,
            onClick = onKeyboard,
            modifier = Modifier.weight(1f)
        )
        RemoteButton(
            icon = Icons.Rounded.Apps,
            label = "All apps",
            enabled = enabled,
            onClick = onAllApps,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Shortcuts that open an app on the TV.
 *
 * These go out as deep links rather than package launches, so one that is not
 * installed quietly does nothing. They are offered as conveniences, not as a
 * list of what the TV has.
 */
@Composable
fun AppShortcutRow(
    enabled: Boolean,
    links: List<AtvAppLink>,
    currentApp: String?,
    onLaunch: (AtvAppLink) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(links, key = { it.packageName }) { link ->
            val active = currentApp != null && currentApp.startsWith(link.packageName.substringBeforeLast('.'))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(74.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(enabled = enabled) { onLaunch(link) }
                    .padding(vertical = 10.dp)
                    .alpha(if (enabled) 1f else 0.4f)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (active) StreamCastColors.PrimaryViolet.copy(alpha = 0.25f)
                            else StreamCastColors.SurfaceHighest
                        )
                        .border(
                            1.dp,
                            if (active) StreamCastColors.PrimaryViolet else Color.Transparent,
                            RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = link.label.take(2).uppercase(),
                        color = if (active) StreamCastColors.PrimaryFixedDim else StreamCastColors.TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = link.label,
                    color = StreamCastColors.TextSecondary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** A numeric pad, for typing a channel number straight onto the TV. */
@Composable
fun ChannelPad(
    enabled: Boolean,
    onKey: (AtvKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
        listOf(null, '0', null)
    )
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { digit ->
                    if (digit == null) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        DigitKey(
                            digit = digit,
                            enabled = enabled,
                            onClick = { AtvKey.forDigit(digit)?.let(onKey) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DigitKey(
    digit: Char,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(StreamCastColors.SurfaceHighest)
            .clickable(enabled = enabled) {
                view.tick()
                onClick()
            }
            .alpha(if (enabled) 1f else 0.4f),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = digit.toString(),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/** The standard pill button the remote is built from. */
@Composable
fun RemoteButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    prominent: Boolean = false,
    tint: Color? = null
) {
    val view = LocalView.current
    Box(
        modifier = modifier
            .height(if (prominent) 56.dp else 52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (prominent) StreamCastColors.PrimaryViolet else StreamCastColors.SurfaceHighest
            )
            .clickable(enabled = enabled) {
                view.tick()
                onClick()
            }
            .alpha(if (enabled) 1f else 0.4f),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint ?: if (prominent) Color.White else StreamCastColors.TextPrimary,
            modifier = Modifier.size(if (prominent) 26.dp else 22.dp)
        )
    }
}

/** The power key, held apart from the rest so it is hard to hit by accident. */
@Composable
fun PowerButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(StreamCastColors.ErrorContainer.copy(alpha = 0.55f))
            .clickable(enabled = enabled) {
                view.tick(strong = true)
                onClick()
            }
            .alpha(if (enabled) 1f else 0.4f),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.PowerSettingsNew,
            contentDescription = "Power",
            tint = StreamCastColors.OnErrorContainer,
            modifier = Modifier.size(19.dp)
        )
    }
}

/**
 * A short haptic tick.
 *
 * The remote sends into the void - nothing on the phone confirms a key arrived
 * - so touch is the only immediate feedback there is, and without it the
 * trackpad in particular feels broken.
 */
internal fun android.view.View.tick(strong: Boolean = false) {
    performHapticFeedback(
        if (strong) {
            android.view.HapticFeedbackConstants.VIRTUAL_KEY
        } else {
            android.view.HapticFeedbackConstants.KEYBOARD_TAP
        },
        android.view.HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
    )
}

/** Travel, in pixels, that counts as one step of the D-pad. */
private const val STEP_PX = 46f
