package com.streamcast.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.CastConnected
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SensorsOff
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamcast.mobile.ui.theme.StreamCastColors
import com.streamcast.mobile.viewmodel.CastViewModel
import com.streamcast.mobile.viewmodel.ChannelsViewModel
import com.streamcast.shared.cast.RemoteKeyCode
import com.streamcast.shared.model.CastPlaybackState

/**
 * Cast media remote.
 *
 * Deliberately **not** a TV D-pad. Google Cast carries transport and volume
 * commands only; arrow keys, power and voice search belong to the Android TV
 * Remote v2 protocol, which this app does not implement. Rather than render
 * buttons that do nothing, the unsupported controls are described honestly at
 * the bottom of the screen.
 */
@Composable
fun WifiRemoteScreen(
    channelsViewModel: ChannelsViewModel,
    castViewModel: CastViewModel,
    onFindTv: () -> Unit
) {
    val device by castViewModel.connectedDevice.collectAsState()
    val channel by castViewModel.castingChannel.collectAsState()
    val playback by castViewModel.playbackState.collectAsState()
    val volume by castViewModel.castVolume.collectAsState()
    val error by castViewModel.lastError.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "Cast remote",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Text(
                text = "Controls the stream playing on your TV",
                color = StreamCastColors.TextSecondary,
                fontSize = 11.sp
            )
        }

        val connected = device

        if (connected == null) {
            StatePlaceholder(
                title = "No TV connected",
                message = "Connect to a Chromecast or Google TV on your Wi-Fi network to " +
                    "control playback from here.",
                icon = Icons.Rounded.SensorsOff,
                actionLabel = "Find a TV",
                onAction = onFindTv
            )
            UnsupportedControlsNote()
            return@Column
        }

        // Connected device summary
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(StreamCastColors.SurfaceSecondary)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(StreamCastColors.PrimaryViolet.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.CastConnected,
                    contentDescription = null,
                    tint = StreamCastColors.PrimaryFixedDim,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = connected.name,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${connected.deviceType} • ${connected.protocol.label}",
                    color = StreamCastColors.TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Now playing
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(StreamCastColors.CardSurface)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "NOW PLAYING",
                color = StreamCastColors.TextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = channel?.name ?: "Nothing sent yet",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = when (playback) {
                    CastPlaybackState.IDLE -> "Idle"
                    CastPlaybackState.LOADING -> "Buffering…"
                    CastPlaybackState.PLAYING -> "Playing"
                    CastPlaybackState.PAUSED -> "Paused"
                    CastPlaybackState.FAILED -> "Playback failed"
                },
                color = if (playback == CastPlaybackState.FAILED) {
                    StreamCastColors.Error
                } else {
                    StreamCastColors.LiveGreen
                },
                fontSize = 11.sp
            )
            if (error != null) {
                Text(text = error!!, color = StreamCastColors.Error, fontSize = 11.sp)
            }
        }

        // Transport row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RemoteButton(
                icon = Icons.Rounded.SkipPrevious,
                label = "Previous",
                enabled = channel != null
            ) {
                channelsViewModel.adjacentChannel(channel, forward = false)?.let {
                    castViewModel.castChannel(it)
                }
            }
            RemoteButton(
                icon = if (playback == CastPlaybackState.PLAYING) {
                    Icons.Rounded.Pause
                } else {
                    Icons.Rounded.PlayArrow
                },
                label = if (playback == CastPlaybackState.PLAYING) "Pause" else "Play",
                primary = true,
                enabled = channel != null
            ) {
                castViewModel.sendKey(RemoteKeyCode.MEDIA_PLAY_PAUSE)
            }
            RemoteButton(
                icon = Icons.Rounded.SkipNext,
                label = "Next",
                enabled = channel != null
            ) {
                channelsViewModel.adjacentChannel(channel, forward = true)?.let {
                    castViewModel.castChannel(it)
                }
            }
            RemoteButton(
                icon = Icons.Rounded.Stop,
                label = "Stop",
                enabled = channel != null
            ) {
                castViewModel.sendKey(RemoteKeyCode.MEDIA_STOP)
            }
        }

        // Volume
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(StreamCastColors.SurfaceSecondary)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "TV volume",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${(volume * 100).toInt()}%",
                    color = StreamCastColors.TextSecondary,
                    fontSize = 11.sp
                )
            }
            Slider(
                value = volume.toFloat(),
                onValueChange = { castViewModel.setVolume(it.toDouble()) },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = StreamCastColors.PrimaryViolet,
                    activeTrackColor = StreamCastColors.PrimaryViolet,
                    inactiveTrackColor = StreamCastColors.SurfaceBright
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RemoteButton(
                    icon = Icons.AutoMirrored.Rounded.VolumeDown,
                    label = "Down"
                ) { castViewModel.sendKey(RemoteKeyCode.VOLUME_DOWN) }
                RemoteButton(
                    icon = Icons.AutoMirrored.Rounded.VolumeOff,
                    label = "Mute"
                ) { castViewModel.sendKey(RemoteKeyCode.VOLUME_MUTE) }
                RemoteButton(
                    icon = Icons.AutoMirrored.Rounded.VolumeUp,
                    label = "Up"
                ) { castViewModel.sendKey(RemoteKeyCode.VOLUME_UP) }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton(
                text = "Switch TV",
                onClick = onFindTv,
                filled = false,
                modifier = Modifier.weight(1f)
            )
            PillButton(
                text = "Disconnect",
                onClick = { castViewModel.disconnect() },
                filled = true,
                modifier = Modifier.weight(1f)
            )
        }

        UnsupportedControlsNote()
    }
}

/**
 * States plainly which TV controls this app cannot offer and why. The previous
 * build rendered a full D-pad, power button and voice key that were wired to
 * nothing at all.
 */
@Composable
private fun UnsupportedControlsNote() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(StreamCastColors.SurfaceLow)
            .border(1.dp, StreamCastColors.Outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            Icons.Rounded.Info,
            contentDescription = null,
            tint = StreamCastColors.TextSecondary,
            modifier = Modifier.size(16.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = "Why there is no D-pad",
                color = StreamCastColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )
            Text(
                text = "Google Cast only carries play, pause, stop and volume. Arrow keys, the " +
                    "power button, the home key and voice search run over the separate Android " +
                    "TV Remote v2 protocol, which StreamCast does not implement yet.",
                color = StreamCastColors.TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun RemoteButton(
    icon: ImageVector,
    label: String,
    primary: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(if (primary) 60.dp else 48.dp)
                .clip(CircleShape)
                .background(
                    when {
                        !enabled -> StreamCastColors.SurfaceHighest.copy(alpha = 0.35f)
                        primary -> StreamCastColors.PrimaryViolet
                        else -> StreamCastColors.SurfaceHighest
                    }
                )
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = when {
                    !enabled -> StreamCastColors.TextSecondary.copy(alpha = 0.4f)
                    primary -> Color.White
                    else -> StreamCastColors.TextPrimary
                },
                modifier = Modifier.size(if (primary) 28.dp else 21.dp)
            )
        }
        Text(
            text = label,
            color = if (enabled) StreamCastColors.TextSecondary
            else StreamCastColors.TextSecondary.copy(alpha = 0.4f),
            fontSize = 10.sp
        )
    }
}

/** Kept for the empty-state call to action on the home screen. */
@Composable
fun CastUnavailableHint(onFindTv: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(StreamCastColors.SurfaceSecondary)
            .clickable(onClick = onFindTv)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.Cast,
            contentDescription = null,
            tint = StreamCastColors.PrimaryFixedDim,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = "Connect a TV to enable the remote",
            color = StreamCastColors.TextSecondary,
            fontSize = 12.sp
        )
    }
}
