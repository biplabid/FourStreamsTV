package com.streamcast.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamcast.mobile.ui.theme.StreamCastColors
import com.streamcast.shared.model.TvDevice

@Composable
fun HomeScreen(
    connectedDevice: TvDevice? = null,
    onBrowseChannels: () -> Unit,
    onCategoryClick: (String) -> Unit = {},
    onOpenDiscovery: () -> Unit,
    onDisconnect: () -> Unit = {},
    onOpenPlayer: (String) -> Unit,
    onOpenRemote: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StreamCastColors.Background)
    ) {
        StreamCastTopHeader(
            connectedDevice = connectedDevice,
            onOpenDiscovery = onOpenDiscovery,
            onSearchClick = onBrowseChannels
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Dynamic Connection Status Banner
            item {
                ActiveCastBanner(
                    connectedDevice = connectedDevice,
                    onConnectClick = onOpenDiscovery,
                    onDisconnectClick = onDisconnect,
                    onRemoteClick = onOpenRemote
                )
            }

            // 2. Continue Watching / Live Channels Carousel with Live In-Card Video Previews
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LivePulseDot()
                            Text(
                                text = "Continue Watching",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = StreamCastColors.TextPrimary
                            )
                        }

                        Text(
                            text = "See All",
                            style = MaterialTheme.typography.labelMedium,
                            color = StreamCastColors.SecondaryCyan,
                            modifier = Modifier.clickable { onBrowseChannels() }
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(StreamCastMockData.Channels.take(4)) { item ->
                            ContinueWatchingCard(
                                item = item,
                                onOpenPlayer = onOpenPlayer,
                                onCast = { onOpenDiscovery() }
                            )
                        }
                    }
                }
            }

            // 3. Category Quick Shortcuts (Grid)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Categories",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = StreamCastColors.TextPrimary
                        )
                        Text(
                            text = "8 Genres",
                            style = MaterialTheme.typography.labelSmall,
                            color = StreamCastColors.TextSecondary
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val shortcuts = StreamCastMockData.CategoryShortcuts
                        for (i in shortcuts.indices step 2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    CategoryCard(
                                        shortcut = shortcuts[i],
                                        isSelected = i == 0,
                                        onClick = { onCategoryClick(shortcuts[i].category.name) }
                                    )
                                }
                                if (i + 1 < shortcuts.size) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        CategoryCard(
                                            shortcut = shortcuts[i + 1],
                                            isSelected = false,
                                            onClick = { onCategoryClick(shortcuts[i + 1].category.name) }
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // 4. Trending in India (Vertical List)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
                                contentDescription = null,
                                tint = StreamCastColors.LiveGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Trending in India",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = StreamCastColors.TextPrimary
                            )
                        }

                        Text(
                            text = "Live Bitrate Monitor",
                            style = MaterialTheme.typography.labelSmall,
                            color = StreamCastColors.TextSecondary
                        )
                    }
                }
            }

            items(StreamCastMockData.Channels.take(5)) { item ->
                TrendingChannelRow(
                    item = item,
                    onOpenPlayer = onOpenPlayer,
                    onCast = { onOpenDiscovery() }
                )
            }
        }
    }
}

@Composable
private fun ContinueWatchingCard(
    item: LiveChannelItem,
    onOpenPlayer: (String) -> Unit,
    onCast: (String) -> Unit
) {
    val isPreviewChannel = item.channel.id == "republic_bharat" || item.channel.id == "dd_national_hd" || item.channel.id == "aaj_tak_hd"

    Column(
        modifier = Modifier
            .width(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(StreamCastColors.CardSurface)
            .clickable { onOpenPlayer(item.channel.id) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(
                            StreamCastColors.PrimaryViolet.copy(alpha = 0.35f),
                            StreamCastColors.SurfaceHighest
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isPreviewChannel) {
                ChannelCardVideoPreview(
                    streamUrl = item.channel.streamUrl,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Cast,
                        contentDescription = null,
                        tint = StreamCastColors.SecondaryCyan,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = item.channel.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = StreamCastColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Red.copy(alpha = 0.9f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LivePulseDot()
                    Text(
                        text = "LIVE",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.channel.quality ?: "1080p",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = Color.White
                    )
                }
            }
        }

        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.channel.name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = StreamCastColors.SecondaryCyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { onCast(item.channel.id) },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Cast,
                        contentDescription = "Cast",
                        tint = StreamCastColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = item.currentShow,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = StreamCastColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = item.viewerCountText,
                style = MaterialTheme.typography.labelSmall,
                color = StreamCastColors.TextSecondary
            )
        }
    }
}
