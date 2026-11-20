package com.streamcast.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamcast.mobile.ui.theme.StreamCastColors
import com.streamcast.mobile.viewmodel.ChannelsViewModel
import com.streamcast.shared.data.PlaylistRepository
import com.streamcast.shared.model.PlaylistSource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(channelsViewModel: ChannelsViewModel) {
    val settings by channelsViewModel.settings.collectAsState()
    val playlist by channelsViewModel.playlist.collectAsState()
    val favouriteIds by channelsViewModel.favouriteIds.collectAsState()

    var urlDraft by remember(settings.playlistUrl) { mutableStateOf(settings.playlistUrl) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )

        // Playlist source
        SettingsCard(title = "Playlist source") {
            Text(
                text = "FourStreamsTV reads any standard M3U playlist. The default is the " +
                    "iptv-org India list.",
                color = StreamCastColors.TextSecondary,
                fontSize = 11.sp
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(StreamCastColors.SurfaceLow)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = urlDraft,
                    onValueChange = { urlDraft = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                    cursorBrush = SolidColor(StreamCastColors.PrimaryFixedDim),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.weight(1f).padding(vertical = 12.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton(
                    text = "Load playlist",
                    onClick = { channelsViewModel.updatePlaylistUrl(urlDraft) },
                    enabled = urlDraft.isNotBlank() && urlDraft != settings.playlistUrl,
                    modifier = Modifier.weight(1f)
                )
                PillButton(
                    text = "Reset",
                    onClick = {
                        urlDraft = PlaylistRepository.DEFAULT_PLAYLIST_URL
                        channelsViewModel.resetPlaylistUrl()
                    },
                    filled = false,
                    modifier = Modifier.weight(1f)
                )
            }

            InfoRow(
                "Legal note: public IPTV aggregators index streams the app does not own. " +
                    "Point this at a playlist you are entitled to use."
            )
        }

        // Playlist status
        SettingsCard(title = "Playlist status") {
            StatusRow("Channels loaded", playlist.channels.size.toString())
            StatusRow(
                "Source",
                when (playlist.source) {
                    PlaylistSource.NETWORK -> "Network"
                    PlaylistSource.CACHE -> "Device cache"
                    PlaylistSource.NONE -> "Not loaded"
                }
            )
            StatusRow(
                "Last updated",
                if (playlist.fetchedAtMillis > 0) {
                    SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault())
                        .format(Date(playlist.fetchedAtMillis))
                } else {
                    "Never"
                }
            )
            playlist.errorMessage?.let {
                StatusRow("Last error", it, valueColor = StreamCastColors.Error)
            }
            PillButton(
                text = if (playlist.isLoading) "Refreshing…" else "Refresh now",
                onClick = { channelsViewModel.refresh() },
                enabled = !playlist.isLoading,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Playback
        SettingsCard(title = "Playback") {
            SettingsToggle(
                title = "Autoplay on open",
                subtitle = "Start the stream as soon as a channel opens",
                checked = settings.autoPlayOnOpen,
                onCheckedChange = { value ->
                    channelsViewModel.updateSettings { it.copy(autoPlayOnOpen = value) }
                }
            )
            SettingsToggle(
                title = "Keep screen awake",
                subtitle = "Stop the display sleeping during local playback",
                checked = settings.keepScreenAwake,
                onCheckedChange = { value ->
                    channelsViewModel.updateSettings { it.copy(keepScreenAwake = value) }
                }
            )
            SettingsToggle(
                title = "Prefer TV when connected",
                subtitle = "Opening a channel sends it straight to the connected TV",
                checked = settings.preferCastWhenConnected,
                onCheckedChange = { value ->
                    channelsViewModel.updateSettings { it.copy(preferCastWhenConnected = value) }
                }
            )
            SettingsToggle(
                title = "Live previews while browsing",
                subtitle = "The centred card fades from its logo into the live stream. " +
                    "Uses mobile data when you are not on Wi-Fi.",
                checked = settings.livePreviews,
                onCheckedChange = { value ->
                    channelsViewModel.updateSettings { it.copy(livePreviews = value) }
                }
            )
        }

        // Library
        SettingsCard(title = "Your library") {
            StatusRow("Favourites saved", favouriteIds.size.toString())
            PillButton(
                text = "Clear recently watched",
                onClick = { channelsViewModel.clearRecents() },
                filled = false,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // About
        SettingsCard(title = "About") {
            StatusRow("Version", "1.0")
            StatusRow("Cast receiver", "Default Media Receiver")
            InfoRow(
                "Casting uses the Google Cast default receiver, which cannot send custom HTTP " +
                    "headers. Streams that require a Referer or a specific User-Agent will fail " +
                    "on the TV even when they play on the phone. A custom web receiver would fix " +
                    "those."
            )
            InfoRow(
                "Electronic programme guide data is not available: M3U playlists carry no " +
                    "schedule information."
            )
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(StreamCastColors.SurfaceSecondary)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
        content()
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = StreamCastColors.TextPrimary, fontSize = 13.sp)
            Text(text = subtitle, color = StreamCastColors.TextSecondary, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = StreamCastColors.PrimaryViolet,
                uncheckedThumbColor = StreamCastColors.TextSecondary,
                uncheckedTrackColor = StreamCastColors.SurfaceBright
            )
        )
    }
}

@Composable
private fun StatusRow(
    label: String,
    value: String,
    valueColor: Color = StreamCastColors.TextPrimary
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            text = label,
            color = StreamCastColors.TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1.2f)
        )
    }
}

@Composable
private fun InfoRow(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(StreamCastColors.SurfaceLow)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Rounded.Info,
            contentDescription = null,
            tint = StreamCastColors.TextSecondary,
            modifier = Modifier.size(14.dp)
        )
        Text(text = text, color = StreamCastColors.TextSecondary, fontSize = 10.sp)
    }
}
