package com.streamcast.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamcast.mobile.ui.theme.StreamCastColors
import com.streamcast.mobile.viewmodel.CastViewModel
import com.streamcast.mobile.viewmodel.ChannelsViewModel
import com.streamcast.shared.model.CastPlaybackState
import com.streamcast.shared.model.ChannelCategory
import com.streamcast.shared.model.PlaylistSource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    channelsViewModel: ChannelsViewModel,
    castViewModel: CastViewModel,
    onOpenPlayer: (String) -> Unit,
    onBrowseChannels: () -> Unit,
    onOpenCategory: (ChannelCategory) -> Unit,
    onOpenRemote: () -> Unit
) {
    val playlist by channelsViewModel.playlist.collectAsState()
    val popular by channelsViewModel.popularChannels.collectAsState()
    val recents by channelsViewModel.recentChannels.collectAsState()
    val favouriteIds by channelsViewModel.favouriteIds.collectAsState()
    val counts by channelsViewModel.categoryCounts.collectAsState()

    val connectedDevice by castViewModel.connectedDevice.collectAsState()
    val castingChannel by castViewModel.castingChannel.collectAsState()
    val castPlayback by castViewModel.playbackState.collectAsState()

    val categories = ChannelCategory.browseOrder.filter { (counts[it] ?: 0) > 0 }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "StreamCast",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
                Text(
                    text = playlistSummary(
                        channelCount = playlist.channels.size,
                        isLoading = playlist.isLoading,
                        source = playlist.source,
                        fetchedAtMillis = playlist.fetchedAtMillis
                    ),
                    color = StreamCastColors.TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // Cast status / call to action
        item {
            val device = connectedDevice
            if (device != null) {
                ActiveCastBanner(
                    device = device,
                    channel = castingChannel,
                    statusText = castStatusText(castPlayback),
                    onDisconnect = { castViewModel.disconnect() },
                    onOpenRemote = onOpenRemote
                )
            } else {
                ConnectTvPrompt(onClick = { castViewModel.openCastDialog() })
            }
        }

        // Playlist error, shown without hiding whatever channels we already have
        playlist.errorMessage?.let { error ->
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(StreamCastColors.ErrorContainer.copy(alpha = 0.35f))
                        .clickable { channelsViewModel.refresh() }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Rounded.CloudOff,
                        contentDescription = null,
                        tint = StreamCastColors.Error,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = error,
                        color = StreamCastColors.OnErrorContainer,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Rounded.Refresh,
                        contentDescription = "Retry",
                        tint = StreamCastColors.Error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // First load, nothing cached yet
        if (playlist.channels.isEmpty()) {
            item {
                if (playlist.isLoading) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = StreamCastColors.PrimaryViolet)
                        Text(
                            text = "Loading Indian channels…",
                            color = StreamCastColors.TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    StatePlaceholder(
                        title = "No channels loaded",
                        message = "StreamCast could not load a playlist. Check your connection, " +
                            "or set a different playlist URL in Settings.",
                        icon = Icons.Rounded.CloudOff,
                        actionLabel = "Try again",
                        onAction = { channelsViewModel.refresh() }
                    )
                }
            }
            return@LazyColumn
        }

        if (recents.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Continue watching",
                    actionLabel = "Clear",
                    onActionClick = { channelsViewModel.clearRecents() }
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(recents, key = { it.id }) { channel ->
                        ChannelRailCard(
                            channel = channel,
                            isFavourite = favouriteIds.contains(channel.id),
                            onClick = { onOpenPlayer(channel.id) },
                            onCast = { castViewModel.castChannel(channel) },
                            onToggleFavourite = { channelsViewModel.toggleFavourite(channel.id) }
                        )
                    }
                }
            }
        }

        if (categories.isNotEmpty()) {
            item { SectionHeader(title = "Browse by genre") }
            item {
                LazyHorizontalGrid(
                    rows = GridCells.Fixed(2),
                    modifier = Modifier.height(190.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(categories, key = { it.name }) { category ->
                        CategoryCard(
                            category = category,
                            channelCount = counts[category] ?: 0,
                            icon = iconForCategory(category),
                            onClick = { onOpenCategory(category) },
                            modifier = Modifier.width(150.dp)
                        )
                    }
                }
            }
        }

        if (popular.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Popular channels",
                    subtitle = "National and widely available streams",
                    actionLabel = "See all",
                    onActionClick = onBrowseChannels
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(popular, key = { it.id }) { channel ->
                        ChannelRailCard(
                            channel = channel,
                            isFavourite = favouriteIds.contains(channel.id),
                            onClick = { onOpenPlayer(channel.id) },
                            onCast = { castViewModel.castChannel(channel) },
                            onToggleFavourite = { channelsViewModel.toggleFavourite(channel.id) }
                        )
                    }
                }
            }
        }

        item {
            PillButton(
                text = "Browse all ${playlist.channels.size} channels",
                onClick = onBrowseChannels,
                filled = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ConnectTvPrompt(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(StreamCastColors.SurfaceSecondary)
            .clickable(onClick = onClick)
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
                Icons.Rounded.Cast,
                contentDescription = null,
                tint = StreamCastColors.PrimaryFixedDim,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Cast to your TV",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Text(
                text = "Find Chromecast and Google TV devices on your Wi-Fi",
                color = StreamCastColors.TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

private fun castStatusText(state: CastPlaybackState): String = when (state) {
    CastPlaybackState.IDLE -> "Connected"
    CastPlaybackState.LOADING -> "Buffering"
    CastPlaybackState.PLAYING -> "Playing"
    CastPlaybackState.PAUSED -> "Paused"
    CastPlaybackState.FAILED -> "Playback failed"
}

private fun playlistSummary(
    channelCount: Int,
    isLoading: Boolean,
    source: PlaylistSource,
    fetchedAtMillis: Long
): String = when {
    isLoading && channelCount == 0 -> "Loading playlist…"
    isLoading -> "$channelCount channels • refreshing…"
    channelCount == 0 -> "No channels loaded"
    else -> {
        val origin = when (source) {
            PlaylistSource.NETWORK -> "updated"
            PlaylistSource.CACHE -> "cached"
            PlaylistSource.NONE -> ""
        }
        val stamp = if (fetchedAtMillis > 0) {
            SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(fetchedAtMillis))
        } else {
            ""
        }
        listOf("$channelCount live channels", origin, stamp)
            .filter { it.isNotBlank() }
            .joinToString(" • ")
    }
}
