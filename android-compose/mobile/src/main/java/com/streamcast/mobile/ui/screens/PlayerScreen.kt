package com.streamcast.mobile.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.ViewGroup
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import com.streamcast.mobile.guide.ChannelInfo
import kotlinx.coroutines.launch
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.rounded.ScreenShare
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.streamcast.mobile.AppGraph
import com.streamcast.mobile.ui.adaptive.LocalWindowLayout
import com.streamcast.mobile.mirror.ExternalDisplay
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
    onOpenRemote: () -> Unit,
    startFullscreen: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Screen mirroring to a TV that has no Cast receiver.
    val mirror = remember { AppGraph.mirror(context) }
    val activity = remember(context) { context.findActivity() }
    val externalDisplay by mirror.externalDisplay.collectAsState()
    val mirrorError by mirror.lastError.collectAsState()

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

    /*
     * Hand the picture to the television.
     *
     * Only possible while we still own the decoder: when Cast has the stream
     * there is no local player, and the TV is being fed directly anyway.
     */
    val isMirroring = externalDisplay != null && exoPlayer != null

    DisposableEffect(externalDisplay, exoPlayer, activity) {
        mirror.present(activity, exoPlayer)
        onDispose { mirror.dismiss() }
    }

    // Wi-Fi Display dies with the phone's screen, so the picture on the TV
    // depends on this flag staying set for as long as we are mirroring.
    DisposableEffect(isMirroring, activity) {
        val window = activity?.window
        if (isMirroring) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    // Pause when the app goes to the background; keep casting running.
    DisposableEffect(lifecycleOwner, exoPlayer, isMirroring) {
        val observer = LifecycleEventObserver { _, event ->
            // The point of putting the picture on a TV is that the phone has
            // stopped being the screen, so leaving the app must not stop
            // playback the way it does for local viewing.
            if (isMirroring) return@LifecycleEventObserver
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

    val windowLayout = LocalWindowLayout.current

    // ---- Full screen ---------------------------------------------------------
    var fullscreen by rememberSaveable { mutableStateOf(startFullscreen) }

    // A phone turned on its side goes straight to full screen; the page around
    // the video has no room there anyway.
    LaunchedEffect(windowLayout.isShortLandscape) {
        if (windowLayout.isShortLandscape) fullscreen = true
    }

    // On a phone, full screen also means landscape. A tablet is left in
    // whatever orientation the user is holding it.
    val isPhone = LocalConfiguration.current.smallestScreenWidthDp < 600
    DisposableEffect(fullscreen, isPhone, activity) {
        if (fullscreen && isPhone) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        onDispose {
            if (isPhone) activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    ImmersiveSystemBars(activity = activity, enabled = fullscreen)

    // Opened straight into full screen from a preview: Back returns to the
    // browser. Entered from the player page: Back just leaves full screen.
    BackHandler(enabled = fullscreen && !startFullscreen) { fullscreen = false }

    val exitFullscreen: () -> Unit = {
        if (startFullscreen) onBack() else fullscreen = false
    }

    // Mirrors the PlayerView controller, so the overlay shows and hides with it.
    var controlsVisible by remember { mutableStateOf(true) }

    // ---- Channel guide -------------------------------------------------------
    val epg = remember { AppGraph.epg(context) }
    val infoRepository = remember { AppGraph.channelInfo(context) }
    val epgStatus by epg.status.collectAsState()
    val schedules by epg.schedules.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(playlist.channels) { epg.ensureLoaded(playlist.channels) }

    var channelInfo by remember(channel.id) { mutableStateOf<ChannelInfo?>(null) }
    var infoLoading by remember(channel.id) { mutableStateOf(true) }
    LaunchedEffect(channel.id) {
        channelInfo = infoRepository.infoFor(channel, playlist.channels)
        infoLoading = false
    }

    val schedule = schedules[channel.id].orEmpty()
    val onNow = schedule.firstOrNull { it.isOnAt(System.currentTimeMillis()) }

    val aboutPanel: @Composable (Modifier, Boolean) -> Unit = { modifier, scrollable ->
        AboutChannelCard(
            channel = channel,
            info = channelInfo,
            loading = infoLoading,
            onNow = onNow,
            modifier = modifier,
            scrollable = scrollable
        )
    }
    val schedulePanel: @Composable (Modifier) -> Unit = { modifier ->
        ScheduleCard(
            channel = channel,
            programmes = schedule,
            status = epgStatus,
            onRetry = { scope.launch { epg.ensureLoaded(playlist.channels, force = true) } },
            modifier = modifier
        )
    }

    // The PlayerView is re-attached, not rebuilt, when this moves between
    // layouts on rotation: the ExoPlayer above is remembered independently.
    val videoSurface: @Composable (Modifier) -> Unit = { modifier ->
        Box(modifier = modifier.background(Color.Black)) {
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

                isMirroring -> MirroringSurface(
                    channel = channel,
                    display = externalDisplay!!,
                    isBuffering = isBuffering,
                    error = mirrorError,
                    onOpenPanel = { mirror.openSystemPanel(context) }
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
                                setShowSubtitleButton(true)
                                setControllerVisibilityListener(
                                    PlayerView.ControllerVisibilityListener { visibility ->
                                        controlsVisible = visibility == android.view.View.VISIBLE
                                    }
                                )
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
                    // In full screen the overlay row owns the top edge.
                    if (!fullscreen) {
                        LiveBadge(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 14.dp, end = 52.dp)
                        )
                    }
                }
            }

            if (fullscreen) {
                FullscreenOverlay(
                    visible = controlsVisible || exoPlayer == null || playerError != null,
                    channel = channel,
                    isMuted = isMuted,
                    isCasting = isCasting,
                    onExit = exitFullscreen,
                    onPrevious = {
                        channelsViewModel.adjacentChannel(channel, forward = false)?.let(::switchTo)
                    },
                    onNext = {
                        channelsViewModel.adjacentChannel(channel, forward = true)?.let(::switchTo)
                    },
                    onToggleMute = {
                        if (isCasting) {
                            castViewModel.sendKey(com.streamcast.shared.cast.RemoteKeyCode.VOLUME_MUTE)
                        } else {
                            isMuted = !isMuted
                        }
                    },
                    onCast = { castViewModel.castChannel(channel) }
                )
            } else {
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
                IconButton(
                    onClick = { fullscreen = true },
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                ) {
                    Icon(
                        Icons.Rounded.Fullscreen,
                        contentDescription = "Full screen",
                        tint = Color.White
                    )
                }
            }
        }
    }

    val details: @Composable () -> Unit = {
        Column(
            modifier = Modifier.padding(horizontal = windowLayout.gutter, vertical = 16.dp),
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

    val relatedRail: @Composable () -> Unit = {
        Column {
            SectionHeader(
                title = "More in ${channel.category.displayName}",
                modifier = Modifier.padding(horizontal = windowLayout.gutter)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = windowLayout.gutter, vertical = 10.dp),
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

    when {
        fullscreen -> videoSurface(Modifier.fillMaxSize())

        windowLayout.useTwoPane && windowLayout.heightDp >= TALL_ENOUGH_FOR_FIXED_VIDEO -> {
            // Landscape tablet. Left: the video stays put, with the channel
            // controls under it and About + Schedule filling the rest of the
            // height, each scrolling on its own. Right: the rest of the genre.
            Row(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.weight(1.7f).fillMaxHeight()) {
                    videoSurface(Modifier.fillMaxWidth().aspectRatio(16f / 9f))
                    details()
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(start = windowLayout.gutter, end = windowLayout.gutter, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        aboutPanel(Modifier.weight(1f).fillMaxHeight(), true)
                        schedulePanel(Modifier.weight(1.25f).fillMaxHeight())
                    }
                }
                if (related.isNotEmpty()) {
                    RelatedList(
                        channel = channel,
                        related = related,
                        favouriteIds = favouriteIds,
                        onSwitch = ::switchTo,
                        onCast = { castViewModel.castChannel(it) },
                        onToggleFavourite = { channelsViewModel.toggleFavourite(it.id) },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }

        windowLayout.useTwoPane -> {
            // Short landscape (a phone that has left full screen): everything
            // scrolls, since a fixed video would leave no room below it.
            Row(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.weight(1.7f).fillMaxHeight(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item { videoSurface(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) }
                    item { details() }
                    item { aboutPanel(Modifier.padding(horizontal = windowLayout.gutter).fillMaxWidth(), false) }
                    item {
                        schedulePanel(
                            Modifier
                                .padding(horizontal = windowLayout.gutter, vertical = 14.dp)
                                .fillMaxWidth()
                                .height(SCHEDULE_HEIGHT_IN_LIST)
                        )
                    }
                }
                if (related.isNotEmpty()) {
                    RelatedList(
                        channel = channel,
                        related = related,
                        favouriteIds = favouriteIds,
                        onSwitch = ::switchTo,
                        onCast = { castViewModel.castChannel(it) },
                        onToggleFavourite = { channelsViewModel.toggleFavourite(it.id) },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }

        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    // On a portrait tablet a full-width 16:9 frame is fine;
                    // it is only ever as tall as about a third of the screen.
                    videoSurface(Modifier.fillMaxWidth().aspectRatio(16f / 9f))
                }
                item { details() }
                if (related.isNotEmpty()) {
                    item { relatedRail() }
                }
                item {
                    if (windowLayout.isCompact) {
                        aboutPanel(
                            Modifier.padding(horizontal = windowLayout.gutter, vertical = 8.dp).fillMaxWidth(),
                            false
                        )
                    } else {
                        // Portrait tablet: About and Schedule side by side.
                        Row(
                            modifier = Modifier
                                .padding(horizontal = windowLayout.gutter, vertical = 8.dp)
                                .fillMaxWidth()
                                .height(SCHEDULE_HEIGHT_IN_LIST),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            aboutPanel(Modifier.weight(1f).fillMaxHeight(), true)
                            schedulePanel(Modifier.weight(1.25f).fillMaxHeight())
                        }
                    }
                }
                if (windowLayout.isCompact) {
                    item {
                        schedulePanel(
                            Modifier
                                .padding(horizontal = windowLayout.gutter, vertical = 8.dp)
                                .fillMaxWidth()
                                .height(SCHEDULE_HEIGHT_IN_LIST)
                        )
                    }
                }
            }
        }
    }
}

/** Below this, a landscape window is too short to pin the video and fill under it. */
private const val TALL_ENOUGH_FOR_FIXED_VIDEO = 600

/** Height given to the schedule when it sits inside a scrolling page. */
private val SCHEDULE_HEIGHT_IN_LIST = 420.dp

/** "More in <genre>" as a vertical list, beside the video on a wide screen. */
@Composable
private fun RelatedList(
    channel: Channel,
    related: List<Channel>,
    favouriteIds: Collection<String>,
    onSwitch: (Channel) -> Unit,
    onCast: (Channel) -> Unit,
    onToggleFavourite: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.background(StreamCastColors.SurfaceLow),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            SectionHeader(title = "More in ${channel.category.displayName}")
        }
        items(related, key = { it.id }) { item ->
            ChannelListRow(
                channel = item,
                isFavourite = favouriteIds.contains(item.id),
                onClick = { onSwitch(item) },
                onCast = { onCast(item) },
                onToggleFavourite = { onToggleFavourite(item) }
            )
        }
    }
}

/**
 * Controls laid over the full-screen picture, shown and hidden together with
 * the player's own controller: leave full screen, the channel name, channel
 * up/down, mute and cast. Play/pause, the live seek bar, quality, audio and
 * subtitle tracks come from the player's controller underneath.
 */
@Composable
private fun BoxScope.FullscreenOverlay(
    visible: Boolean,
    channel: Channel,
    isMuted: Boolean,
    isCasting: Boolean,
    onExit: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleMute: () -> Unit,
    onCast: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent))
                )
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onExit) {
                Icon(Icons.Rounded.FullscreenExit, contentDescription = "Exit full screen", tint = Color.White)
            }
            ChannelLogo(channel = channel, modifier = Modifier.size(44.dp, 32.dp))
            Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
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
                    maxLines = 1
                )
            }
            IconButton(onClick = onPrevious) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous channel", tint = Color.White)
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Rounded.SkipNext, contentDescription = "Next channel", tint = Color.White)
            }
            IconButton(onClick = onToggleMute) {
                Icon(
                    if (isMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                    contentDescription = "Mute",
                    tint = Color.White
                )
            }
            IconButton(onClick = onCast) {
                Icon(
                    if (isCasting) Icons.Rounded.CastConnected else Icons.Rounded.Cast,
                    contentDescription = "Cast to TV",
                    tint = Color.White
                )
            }
        }
    }
}

/**
 * Hides the status and navigation bars while [enabled], restoring them when it
 * stops being enabled or the screen leaves. Swiping from an edge brings them
 * back temporarily, so the viewer is never trapped.
 */
@Composable
private fun ImmersiveSystemBars(activity: Activity?, enabled: Boolean) {
    DisposableEffect(activity, enabled) {
        val window = activity?.window
        if (window == null || !enabled) return@DisposableEffect onDispose { }
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { controller.show(WindowInsetsCompat.Type.systemBars()) }
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

/**
 * What the phone shows while the picture is on a mirrored television.
 *
 * Deliberately not a video view. The player has exactly one video surface and
 * it is on the TV; a second surface here would take it straight back off
 * again. So the phone shows state and gets out of the way.
 */
@Composable
private fun MirroringSurface(
    channel: Channel,
    display: ExternalDisplay,
    isBuffering: Boolean,
    error: String?,
    onOpenPanel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.AutoMirrored.Rounded.ScreenShare,
            contentDescription = null,
            tint = StreamCastColors.SecondaryCyan,
            modifier = Modifier.size(38.dp)
        )
        Text(
            text = "${channel.name} on ${display.name}",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 10.dp)
        )
        Text(
            text = error ?: "Full screen at ${display.resolutionLabel}. " +
                "Your phone is free - it is not being copied to the TV.",
            color = if (error != null) StreamCastColors.Error else StreamCastColors.TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 4.dp, start = 12.dp, end = 12.dp)
        )
        if (isBuffering) {
            CircularProgressIndicator(
                color = StreamCastColors.SecondaryCyan,
                strokeWidth = 2.dp,
                modifier = Modifier.padding(top = 12.dp).size(20.dp)
            )
        }
        Text(
            text = "Mirroring settings",
            color = StreamCastColors.PrimaryFixedDim,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .padding(top = 14.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onOpenPanel)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

/**
 * The Activity behind a Compose [Context].
 *
 * A [android.app.Presentation] is a Dialog, and a Dialog needs a real
 * Activity window token - the application context throws BadTokenException.
 */
private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
