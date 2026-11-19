package com.streamcast.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.streamcast.mobile.ui.theme.StreamCastColors

@Composable
fun SettingsScreen() {
    var hwAcceleration by remember { mutableStateOf(true) }
    var autoPlay by remember { mutableStateOf(true) }
    var lowLatencyMode by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StreamCastColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = null,
                tint = StreamCastColors.PrimaryViolet,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = "App Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = StreamCastColors.TextPrimary
                )
                Text(
                    text = "StreamCast Player & Cast configuration",
                    style = MaterialTheme.typography.bodySmall,
                    color = StreamCastColors.TextSecondary
                )
            }
        }

        // Section 1: Playback & Streaming
        SettingsSectionHeader("PLAYBACK & STREAMING")

        SettingsToggleItem(
            icon = Icons.Rounded.Speed,
            title = "Hardware Decoder Acceleration",
            subtitle = "Use Direct Media3 H/W decoding for H.265 / HEVC streams",
            checked = hwAcceleration,
            onCheckedChange = { hwAcceleration = it }
        )

        SettingsToggleItem(
            icon = Icons.Rounded.HighQuality,
            title = "Ultra Low Latency HLS Mode",
            subtitle = "Maintain sub-1.5s stream buffer latency for live sports",
            checked = lowLatencyMode,
            onCheckedChange = { lowLatencyMode = it }
        )

        SettingsToggleItem(
            icon = Icons.Rounded.HighQuality,
            title = "Auto-play Live Channel on Tap",
            subtitle = "Instantly play live channel stream when selected",
            checked = autoPlay,
            onCheckedChange = { autoPlay = it }
        )

        // Section 2: Playlist & Server
        SettingsSectionHeader("M3U PLAYLIST MANAGER")

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(StreamCastColors.CardSurface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Link,
                    contentDescription = null,
                    tint = StreamCastColors.SecondaryCyan,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Loaded Playlist URL",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = StreamCastColors.TextPrimary
                )
            }
            Text(
                text = "https://streamcast.app/playlists/indian_channels_v2.m3u8",
                style = MaterialTheme.typography.bodySmall,
                color = StreamCastColors.SecondaryCyan
            )
            Text(
                text = "Last updated: Today at 12:30 PM • 248 Channels Sync Active",
                style = MaterialTheme.typography.labelSmall,
                color = StreamCastColors.TextSecondary
            )
        }

        // Section 3: Cast & TV Devices
        SettingsSectionHeader("GOOGLE CAST & DISCOVERY")

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(StreamCastColors.CardSurface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Cast,
                    contentDescription = null,
                    tint = StreamCastColors.LiveGreen,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Cast Receiver Protocol",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = StreamCastColors.TextPrimary
                )
            }
            Text(
                text = "Google TV Cast Receiver v2 (HLS Passthrough)",
                style = MaterialTheme.typography.bodySmall,
                color = StreamCastColors.TextSecondary
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = StreamCastColors.PrimaryViolet,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(StreamCastColors.CardSurface)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = StreamCastColors.TextSecondary,
                modifier = Modifier.size(22.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = StreamCastColors.TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = StreamCastColors.TextSecondary
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = StreamCastColors.PrimaryViolet,
                uncheckedThumbColor = StreamCastColors.TextSecondary,
                uncheckedTrackColor = StreamCastColors.SurfaceSecondary
            )
        )
    }
}
