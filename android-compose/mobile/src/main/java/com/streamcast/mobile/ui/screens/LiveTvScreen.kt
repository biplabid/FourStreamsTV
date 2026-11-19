package com.streamcast.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.util.UnstableApi
import com.streamcast.mobile.ui.preview.rememberChannelPreviewController
import com.streamcast.mobile.ui.theme.StreamCastColors
import com.streamcast.mobile.viewmodel.CastViewModel
import com.streamcast.mobile.viewmodel.ChannelsViewModel
import com.streamcast.shared.model.ChannelCategory
import kotlin.math.abs

@OptIn(UnstableApi::class)
@Composable
fun LiveTvScreen(
    channelsViewModel: ChannelsViewModel,
    castViewModel: CastViewModel,
    initialCategoryName: String?,
    onOpenPlayer: (String) -> Unit
) {
    val playlist by channelsViewModel.playlist.collectAsState()
    val filtered by channelsViewModel.filteredChannels.collectAsState()
    val selectedCategory by channelsViewModel.selectedCategory.collectAsState()
    val searchQuery by channelsViewModel.searchQuery.collectAsState()
    val favouriteIds by channelsViewModel.favouriteIds.collectAsState()
    val counts by channelsViewModel.categoryCounts.collectAsState()
    val available by channelsViewModel.availableCategories.collectAsState()

    val settings by channelsViewModel.settings.collectAsState()
    val connectedDevice by castViewModel.connectedDevice.collectAsState()

    var isGridView by rememberSaveable { mutableStateOf(true) }

    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val previewController = rememberChannelPreviewController(scope)

    /**
     * "In focus" on a touch screen means the card sitting closest to the middle
     * of the viewport - there is no D-pad focus to borrow. derivedStateOf keeps
     * this off the recomposition path for every scroll pixel.
     */
    val focusedChannel by remember(filtered) {
        derivedStateOf {
            val info = gridState.layoutInfo
            if (info.visibleItemsInfo.isEmpty()) {
                null
            } else {
                val viewportCentre = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                val nearest = info.visibleItemsInfo.minByOrNull { item ->
                    abs(item.offset.y + item.size.height / 2f - viewportCentre)
                }
                nearest?.index?.let(filtered::getOrNull)
            }
        }
    }

    // Previews are pointless while casting (the TV is the screen that matters)
    // and are the single most data-hungry thing the app does, so they are opt-out.
    val previewsAllowed = settings.livePreviews && isGridView && connectedDevice == null

    LaunchedEffect(focusedChannel?.id, previewsAllowed) {
        previewController.focus(if (previewsAllowed) focusedChannel else null)
    }

    // Never leave a decoder running behind a backgrounded app.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) previewController.stop()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Deep link from a home-screen genre tile
    LaunchedEffect(initialCategoryName) {
        if (initialCategoryName != null) {
            channelsViewModel.selectCategoryByName(initialCategoryName)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Live TV",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(
                        text = if (playlist.isLoading && playlist.channels.isEmpty()) {
                            "Loading…"
                        } else {
                            "${filtered.size} of ${playlist.channels.size} channels"
                        },
                        color = StreamCastColors.TextSecondary,
                        fontSize = 11.sp
                    )
                }
                IconToggle(
                    icon = if (isGridView) Icons.Rounded.ViewList else Icons.Rounded.GridView,
                    description = if (isGridView) "Switch to list view" else "Switch to grid view",
                    onClick = { isGridView = !isGridView }
                )
                IconToggle(
                    icon = Icons.Rounded.Refresh,
                    description = "Refresh playlist",
                    onClick = { channelsViewModel.refresh() },
                    busy = playlist.isLoading
                )
            }

            ChannelSearchField(
                query = searchQuery,
                onQueryChange = channelsViewModel::setSearchQuery
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    CategoryChip(
                        label = "All",
                        selected = selectedCategory == ChannelCategory.ALL,
                        trailingCount = playlist.channels.size.takeIf { it > 0 },
                        onClick = { channelsViewModel.selectCategory(ChannelCategory.ALL) }
                    )
                }
                items(available, key = { it.name }) { category ->
                    CategoryChip(
                        label = category.displayName,
                        selected = selectedCategory == category,
                        trailingCount = counts[category],
                        onClick = { channelsViewModel.selectCategory(category) }
                    )
                }
            }
        }

        when {
            playlist.isLoading && playlist.channels.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = StreamCastColors.PrimaryViolet)
                }
            }

            filtered.isEmpty() -> {
                Box(modifier = Modifier.padding(16.dp)) {
                    StatePlaceholder(
                        title = if (playlist.channels.isEmpty()) {
                            "No channels loaded"
                        } else {
                            "Nothing matches that"
                        },
                        message = if (playlist.channels.isEmpty()) {
                            playlist.errorMessage
                                ?: "Pull a playlist in from Settings, or try refreshing."
                        } else {
                            "Try a different genre, or clear the search box."
                        },
                        icon = Icons.Rounded.SearchOff,
                        actionLabel = if (playlist.channels.isEmpty()) "Retry" else "Clear filters",
                        onAction = {
                            if (playlist.channels.isEmpty()) {
                                channelsViewModel.refresh()
                            } else {
                                channelsViewModel.setSearchQuery("")
                                channelsViewModel.selectCategory(ChannelCategory.ALL)
                            }
                        }
                    )
                }
            }

            isGridView -> {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { channel ->
                        ChannelGridCard(
                            channel = channel,
                            isFavourite = favouriteIds.contains(channel.id),
                            onClick = { onOpenPlayer(channel.id) },
                            onCast = { castViewModel.castChannel(channel) },
                            onToggleFavourite = { channelsViewModel.toggleFavourite(channel.id) },
                            previewPlayer = previewController.playerFor(channel.id),
                            previewRevealed = previewController.isRevealed &&
                                previewController.activeChannelId == channel.id
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.id }) { channel ->
                        ChannelListRow(
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
    }
}

@Composable
private fun IconToggle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    busy: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(StreamCastColors.SurfaceHighest)
            .clickable(enabled = !busy, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (busy) {
            CircularProgressIndicator(
                color = StreamCastColors.PrimaryFixedDim,
                strokeWidth = 2.dp,
                modifier = Modifier.size(16.dp)
            )
        } else {
            Icon(
                icon,
                contentDescription = description,
                tint = StreamCastColors.TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
