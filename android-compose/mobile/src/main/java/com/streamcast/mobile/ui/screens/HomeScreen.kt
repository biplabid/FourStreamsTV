package com.streamcast.mobile.ui.screens

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.streamcast.mobile.ui.preview.PreviewVideoLayer
import com.streamcast.mobile.ui.preview.rememberChannelPreviewController
import com.streamcast.mobile.ui.theme.StreamCastColors
import com.streamcast.mobile.viewmodel.CastViewModel
import com.streamcast.mobile.viewmodel.ChannelsViewModel
import com.streamcast.shared.model.CastPlaybackState
import com.streamcast.shared.model.Channel
import com.streamcast.shared.model.ChannelCategory
import com.streamcast.shared.model.PlaylistSource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(UnstableApi::class)
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
    val favouriteIds by channelsViewModel.favouriteIds.collectAsState()
    val counts by channelsViewModel.categoryCounts.collectAsState()

    val connectedDevice by castViewModel.connectedDevice.collectAsState()
    val castingChannel by castViewModel.castingChannel.collectAsState()
    val castPlayback by castViewModel.playbackState.collectAsState()

    val settings by channelsViewModel.settings.collectAsState()
    val categories = ChannelCategory.browseOrder.filter { (counts[it] ?: 0) > 0 }

    // ---- Featured preview window -------------------------------------------
    val scope = rememberCoroutineScope()
    val heroController = rememberChannelPreviewController(scope)

    /**
     * Random navigation keeps a trail rather than only rolling forward, so
     * Back returns to the channel you just saw instead of throwing you at
     * another stranger. Forward re-walks the trail before drawing anything new.
     */
    var history by remember { mutableStateOf<List<String>>(emptyList()) }
    var cursor by remember { mutableIntStateOf(-1) }

    // Saveable so a configuration change (dark mode, rotation, font scale)
    // does not silently re-randomise the featured channel out from under the
    // viewer - recreation would otherwise reset a plain remember.
    var heroChannelId by rememberSaveable { mutableStateOf<String?>(null) }

    val heroChannel = remember(heroChannelId, playlist.channels) {
        heroChannelId?.let { id -> playlist.channels.firstOrNull { it.id == id } }
    }

    // Channels already proven dead this session; never offered again.
    var exhausted by remember { mutableStateOf(setOf<String>()) }

    /** Draws a fresh channel, appends it to the trail and moves to it. */
    fun rollForward() {
        val next = pickFeatured(playlist.channels, exhausted + history) ?: return
        Log.d(HERO_TAG, "rollForward -> ${next.name}")
        history = history.take(cursor + 1) + next.id
        cursor = history.lastIndex
        heroChannelId = next.id
    }

    fun goBack() {
        if (cursor <= 0) return
        cursor -= 1
        heroChannelId = history[cursor]
    }

    fun goForward() {
        if (cursor < history.lastIndex) {
            cursor += 1
            heroChannelId = history[cursor]
        } else {
            rollForward()
        }
    }

    /** Drops a dead channel out of the trail entirely and draws a replacement. */
    fun replaceCurrent() {
        val replacement = pickFeatured(playlist.channels, exhausted + history) ?: return
        Log.d(HERO_TAG, "replaceCurrent -> ${replacement.name}")
        history = history.toMutableList().also { list ->
            if (cursor in list.indices) list[cursor] = replacement.id else list.add(replacement.id)
        }
        cursor = cursor.coerceAtLeast(0)
        heroChannelId = replacement.id
    }

    // Pick once the playlist arrives; also re-seed the trail after recreation.
    LaunchedEffect(playlist.channels.isNotEmpty()) {
        Log.d(HERO_TAG, "seed effect fired: channels=${playlist.channels.size} id=$heroChannelId")
        if (playlist.channels.isEmpty()) return@LaunchedEffect
        val restored = heroChannelId
        if (restored != null && history.isEmpty()) {
            history = listOf(restored)
            cursor = 0
        } else if (restored == null) {
            rollForward()
        }
    }

    /**
     * A randomly chosen channel is very often a dead link, and a hero stuck on
     * a motionless logo looks broken. On a failed preview, retire that channel
     * and draw again, up to a cap so a bad playlist cannot spin forever.
     */
    LaunchedEffect(heroController.failedChannelId) {
        val failed = heroController.failedChannelId
        Log.d(HERO_TAG, "failure effect fired: failed=$failed current=$heroChannelId")
        if (failed != null && failed == heroChannelId && exhausted.size < MAX_HERO_ROLLS) {
            exhausted = exhausted + failed
            replaceCurrent()
        }
    }

    val heroPreviewAllowed = settings.livePreviews && connectedDevice == null

    LaunchedEffect(heroChannel?.id, heroPreviewAllowed) {
        Log.d(
            HERO_TAG,
            "focus effect fired: channel=${heroChannel?.name} allowed=$heroPreviewAllowed " +
                "device=${connectedDevice?.name}"
        )
        heroController.focus(if (heroPreviewAllowed) heroChannel else null)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) heroController.stop()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    // ------------------------------------------------------------------------

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Sits above everything else; the rest of the home screen is unchanged
        // and simply moves down.
        heroChannel?.let { channel ->
            item {
                FeaturedPreviewWindow(
                    channel = channel,
                    player = heroController.playerFor(channel.id),
                    revealed = heroController.isRevealed &&
                        heroController.activeChannelId == channel.id,
                    isFavourite = favouriteIds.contains(channel.id),
                    onCast = { castViewModel.castChannel(channel) },
                    onWatch = { onOpenPlayer(channel.id) },
                    onPrevious = ::goBack,
                    onNext = ::goForward,
                    canGoBack = cursor > 0,
                    positionLabel = if (history.isEmpty()) "" else "${cursor + 1}/${history.size}",
                    onToggleFavourite = { channelsViewModel.toggleFavourite(channel.id) }
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "FourStreamsTV",
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
                        message = "FourStreamsTV could not load a playlist. Check your connection, " +
                            "or set a different playlist URL in Settings.",
                        icon = Icons.Rounded.CloudOff,
                        actionLabel = "Try again",
                        onAction = { channelsViewModel.refresh() }
                    )
                }
            }
            return@LazyColumn
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

/** How many dead channels to roll past before giving up on the hero. */
private const val MAX_HERO_ROLLS = 8

private const val HERO_TAG = "StreamCastHero"

/**
 * Picks a channel to feature.
 *
 * Genuinely random, with one bias: entries flagged geo-blocked in the playlist
 * are skipped while any others remain, because they fail far more often and a
 * failed pick costs the viewer five seconds of staring at a logo.
 */
private fun pickFeatured(channels: List<Channel>, exclude: Set<String>): Channel? {
    val available = channels.filterNot { it.id in exclude }
    if (available.isEmpty()) return null
    val preferred = available.filterNot { it.isGeoRestricted }
    return (if (preferred.isNotEmpty()) preferred else available).random()
}

/**
 * The featured window at the top of Home: a random live channel that dissolves
 * from its logo into the stream, exactly like a browser card.
 *
 * Tapping the window casts it to the TV, which is the point of the app. Watching
 * it locally stays available through the explicit play button, so the tap target
 * for casting never steals that option.
 */
@OptIn(UnstableApi::class)
@Composable
private fun FeaturedPreviewWindow(
    channel: Channel,
    player: ExoPlayer?,
    revealed: Boolean,
    isFavourite: Boolean,
    onCast: () -> Unit,
    onWatch: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    canGoBack: Boolean,
    positionLabel: String,
    onToggleFavourite: () -> Unit
) {
    val videoAlpha by animateFloatAsState(
        targetValue = if (player != null && revealed) 1f else 0f,
        animationSpec = tween(durationMillis = PREVIEW_DISSOLVE_MS),
        label = "hero-dissolve"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(StreamCastColors.CardSurface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(StreamCastColors.SurfaceLow)
                .clickable(onClick = onCast)
        ) {
            PreviewVideoLayer(
                player = player,
                alpha = videoAlpha,
                modifier = Modifier.fillMaxSize()
            )

            ChannelLogo(
                channel = channel,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(104.dp, 74.dp)
                    .alpha(1f - videoAlpha),
                cornerRadius = 12
            )

            // Keeps the caption legible once video is underneath it.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(74.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LiveBadge()
                MetaChip(text = "FEATURED", tint = StreamCastColors.PrimaryFixedDim)
                if (positionLabel.isNotEmpty()) {
                    MetaChip(text = positionLabel)
                }
            }

            // Scrimmed so the controls stay legible over a bright frame; plain
            // white icons disappear against daytime news studio lighting.
            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScrimmedIconButton(
                    icon = if (isFavourite) Icons.Rounded.Favorite
                    else Icons.Rounded.FavoriteBorder,
                    description = if (isFavourite) "Remove favourite" else "Add favourite",
                    tint = if (isFavourite) StreamCastColors.Error else Color.White,
                    onClick = onToggleFavourite
                )
            }

            // Random navigation: step back through what you have already seen,
            // or forward into a new draw.
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
            ) {
                ScrimmedIconButton(
                    icon = Icons.Rounded.ChevronLeft,
                    description = "Previous channel",
                    enabled = canGoBack,
                    onClick = onPrevious
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
            ) {
                ScrimmedIconButton(
                    icon = Icons.Rounded.ChevronRight,
                    description = "Next random channel",
                    onClick = onNext
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = channel.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = channelSubtitle(channel),
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable(onClick = onWatch),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = "Watch on this phone",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onCast)
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Rounded.Cast,
                contentDescription = null,
                tint = StreamCastColors.PrimaryFixedDim,
                modifier = Modifier.size(17.dp)
            )
            Text(
                text = "Tap to cast this channel to your TV",
                color = StreamCastColors.TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/** Circular dark scrim behind an icon, for controls that sit over video. */
@Composable
private fun ScrimmedIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    tint: Color = Color.White,
    enabled: Boolean = true
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = if (enabled) 0.45f else 0.25f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (enabled) tint else tint.copy(alpha = 0.35f),
            modifier = Modifier.size(18.dp)
        )
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
