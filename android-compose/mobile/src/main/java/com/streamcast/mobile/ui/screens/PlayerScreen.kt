package com.streamcast.mobile.ui.screens

import android.view.ViewGroup
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.CastConnected
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.streamcast.mobile.ui.theme.StreamCastColors
import com.streamcast.mobile.viewmodel.CastViewModel
import com.streamcast.mobile.viewmodel.ChannelsViewModel
import com.streamcast.shared.model.CastPlaybackState
import com.streamcast.shared.model.Channel

/**
 * In-app playback with ExoPlayer, plus a handoff to the TV.
 *
 * When a Cast session is active the local player is released and the TV becomes
 * the playback surface - running both at once would double the bandwidth and
 * play the same audio twice.
 */
@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    channelId: String,
    channelsViewModel: ChannelsViewModel,
    castViewModel: CastViewModel,
    onBack: () -> Unit,
    onOpenRemote: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val playlist by channelsViewModel.playlist.collectAsState()
    val favouriteIds by channelsViewModel.favouriteIds.collectAsState()
    val connectedDevice by castViewModel.connectedDevice.collectAsState()
    val castPlayback by castViewModel.playbackState.collectAsState()
    val castError by castViewModel.lastError.collectAsState()
    val settings by channelsViewModel.settings.collectAsState()

    var activeChannelId by remember(channelId) { mutableStateOf(channelId) }
    val channel = remember(activeChannelId, playlist.channels) {
        channelsViewModel.channelById(activeChannelId)
    }

    if (channel == null) {
        MissingChannel(onBack = onBack)
        return
    }

    val isCasting = connectedDevice != null
    val related = remember(channel.id, playlist.channels) {
        channelsViewModel.relatedChannels(channel, limit = 12)
    }

    // Record a watch once per channel
    LaunchedEffect(channel.id) {
        channelsViewModel.recordWatched(channel.id)
        if (isCasting && settings.preferCastWhenConnected) {
            castViewModel.castChannel(channel)
        }
    }

    var playerError by remember(channel.id) { mutableStateOf<String?>(null) }
    var isBuffering by remember(channel.id) { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(true) }
    // Bumping this forces the player to be rebuilt, which is how Retry works.
    var retryToken by remember(channel.id) { mutableIntStateOf(0) }

    // Only hold a local player when we are not casting.
    val exoPlayer = remember(channel.id, isCasting, retryToken) {
        if (isCasting) {
            null
        } else {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(channel.streamUrl))
                playWhenReady = settings.autoPlayOnOpen
                prepare()
            }
        }
    }

    // Surface real playback failures instead of showing a frozen black box.
    DisposableEffect(exoPlayer) {
        val player = exoPlayer
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                playerError = describePlaybackError(error)
            }
        }
        player?.addListener(listener)
        onDispose {
            player?.removeListener(listener)
            player?.release()
        }
    }

    // Pause when the app goes to the background; keep casting running.
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer?.pause()
                Lifecycle.Event.ON_STOP -> exoPlayer?.playWhenReady = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(isMuted, exoPlayer) {
        exoPlayer?.volume = if (isMuted) 0f else 1f
    }

    fun switchTo(target: Channel) {
        activeChannelId = target.id
        if (isCasting) {
            castViewModel.castChannel(target)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
            ) {
                when {
                    isCasting -> CastingSurface(
                        channel = channel,
                        deviceName = connectedDevice?.name.orEmpty(),
                        state = castPlayback,
                        error = castError
                    )

                    playerError != null -> PlaybackErrorSurface(
                        message = playerError!!,
                        onRetry = {
                            playerError = null
                            isBuffering = true
                            retryToken++
                        },
                        onCast = { castViewModel.castChannel(channel) }
                    )

                    else -> {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    useController = true
                                    setShowNextButton(false)
                                    setShowPreviousButton(false)
                                    setKeepContentOnPlayerReset(true)
                                }
                            },
                            update = { view -> view.player = exoPlayer }
                        )
                        if (isBuffering) {
                            CircularProgressIndicator(
                                color = StreamCastColors.PrimaryViolet,
                                modifier = Modifier.align(Alignment.Center).size(34.dp)
                            )
                        }
                        LiveBadge(
                            modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }
        }

        item {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ChannelLogo(channel = channel, modifier = Modifier.size(50.dp, 36.dp))
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                        Text(
                            text = channel.name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = channelSubtitle(channel),
                            color = StreamCastColors.TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = { channelsViewModel.toggleFavourite(channel.id) }) {
                        val fav = favouriteIds.contains(channel.id)
                        Icon(
                            imageVector = if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = if (fav) "Remove favourite" else "Add favourite",
                            tint = if (fav) StreamCastColors.Error else StreamCastColors.TextSecondary
                        )
                    }
                }

                if (channel.isGeoRestricted) {
                    InfoNote(
                        "This channel is marked geo-blocked in the playlist. It may only play " +
                            "from an Indian IP address."
                    )
                }

                // Transport controls. When casting these drive the TV.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircleControl(
                        icon = Icons.Rounded.SkipPrevious,
                        description = "Previous channel"
                    ) {
                        channelsViewModel.adjacentChannel(channel, forward = false)
                            ?.let(::switchTo)
                    }
                    CircleControl(
                        icon = if (isCasting) {
                            if (castPlayback == CastPlaybackState.PLAYING) Icons.Rounded.Pause
                            else Icons.Rounded.PlayArrow
                        } else {
                            if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow
                        },
                        description = "Play or pause",
                        primary = true
                    ) {
                        if (isCasting) {
                            castViewModel.sendKey(
                                com.streamcast.shared.cast.RemoteKeyCode.MEDIA_PLAY_PAUSE
                            )
                        } else {
                            exoPlayer?.let { it.playWhenReady = !it.playWhenReady }
                        }
                    }
                    CircleControl(
                        icon = Icons.Rounded.SkipNext,
                        description = "Next channel"
                    ) {
                        channelsViewModel.adjacentChannel(channel, forward = true)
                            ?.let(::switchTo)
                    }
                    CircleControl(
                        icon = if (isMuted) Icons.AutoMirrored.Rounded.VolumeOff
                        else Icons.AutoMirrored.Rounded.VolumeUp,
                        description = "Mute"
                    ) {
                        if (isCasting) {
                            castViewModel.sendKey(
                                com.streamcast.shared.cast.RemoteKeyCode.VOLUME_MUTE
                            )
                        } else {
                            isMuted = !isMuted
                        }
                    }
                    Box(modifier = Modifier.weight(1f))
                    CircleControl(
                        icon = if (isCasting) Icons.Rounded.CastConnected else Icons.Rounded.Cast,
                        description = if (isCasting) "Cast settings" else "Cast to TV",
                        primary = isCasting
                    ) {
                        if (isCasting) onOpenRemote() else castViewModel.castChannel(channel)
                    }
                }
            }
        }

        if (related.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "More in ${channel.category.displayName}",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(related, key = { it.id }) { item ->
                        ChannelRailCard(
                            channel = item,
                            isFavourite = favouriteIds.contains(item.id),
                            onClick = { switchTo(item) },
                            onCast = { castViewModel.castChannel(item) },
                            onToggleFavourite = { channelsViewModel.toggleFavourite(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CastingSurface(
    channel: Channel,
    deviceName: String,
    state: CastPlaybackState,
    error: String?
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Rounded.CastConnected,
            contentDescription = null,
            tint = StreamCastColors.PrimaryFixedDim,
            modifier = Modifier.size(38.dp)
        )
        Text(
            text = when (state) {
                CastPlaybackState.LOADING -> "Starting ${channel.name}…"
                CastPlaybackState.PLAYING -> "Playing on $deviceName"
                CastPlaybackState.PAUSED -> "Paused on $deviceName"
                CastPlaybackState.FAILED -> "Could not play on $deviceName"
                CastPlaybackState.IDLE -> "Connected to $deviceName"
            },
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 10.dp)
        )
        Text(
            text = error ?: "Playing on the TV. The phone screen stays free.",
            color = if (error != null) StreamCastColors.Error else StreamCastColors.TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 4.dp, start = 12.dp, end = 12.dp)
        )
        if (state == CastPlaybackState.LOADING) {
            CircularProgressIndicator(
                color = StreamCastColors.PrimaryViolet,
                strokeWidth = 2.dp,
                modifier = Modifier.padding(top = 12.dp).size(20.dp)
            )
        }
    }
}

@Composable
private fun PlaybackErrorSurface(
    message: String,
    onRetry: () -> Unit,
    onCast: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = StreamCastColors.Error,
            modifier = Modifier.size(32.dp)
        )
        Text(
            text = "Stream unavailable",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = message,
            color = StreamCastColors.TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
        Row(
            modifier = Modifier.padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(StreamCastColors.PrimaryViolet)
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 18.dp, vertical = 9.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Refresh,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Text("Retry", color = Color.White, fontSize = 12.sp)
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(StreamCastColors.SurfaceHighest)
                    .clickable(onClick = onCast)
                    .padding(horizontal = 18.dp, vertical = 9.dp)
            ) {
                Text("Try on TV", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun MissingChannel(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        StatePlaceholder(
            title = "Channel unavailable",
            message = "This channel is no longer in the loaded playlist. It may have been " +
                "removed upstream since you last refreshed.",
            icon = Icons.Rounded.ErrorOutline,
            actionLabel = "Go back",
            onAction = onBack
        )
    }
}

@Composable
private fun InfoNote(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(StreamCastColors.SurfaceSecondary)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = StreamCastColors.TextSecondary,
            modifier = Modifier.size(15.dp)
        )
        Text(text = text, color = StreamCastColors.TextSecondary, fontSize = 11.sp)
    }
}

@Composable
private fun CircleControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    primary: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(if (primary) 50.dp else 42.dp)
            .clip(CircleShape)
            .background(
                if (primary) StreamCastColors.PrimaryViolet else StreamCastColors.SurfaceHighest
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (primary) Color.White else StreamCastColors.TextPrimary,
            modifier = Modifier.size(if (primary) 24.dp else 19.dp)
        )
    }
}

/**
 * Turns an ExoPlayer error code into something a viewer can act on. IPTV
 * streams fail constantly, and "source error" alone is not actionable.
 */
private fun describePlaybackError(error: PlaybackException): String = when (error.errorCode) {
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
        "Could not reach the stream server. Check your connection."

    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
        "The broadcaster rejected the request. This stream is likely offline or geo-blocked."

    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
        "The stream URL no longer exists. Refresh the playlist."

    PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED ->
        "This stream uses plain HTTP, which the system blocked."

    PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED ->
        "The stream format could not be read."

    PlaybackException.ERROR_CODE_DECODING_FAILED,
    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ->
        "This device cannot decode this stream."

    else -> error.localizedMessage ?: "Playback failed (code ${error.errorCode})."
}
