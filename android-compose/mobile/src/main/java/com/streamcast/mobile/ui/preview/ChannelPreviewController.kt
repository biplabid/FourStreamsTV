package com.streamcast.mobile.ui.preview

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.streamcast.shared.model.Channel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Drives the logo-to-video dissolve on the channel card that is currently
 * centred in the browser.
 *
 * ## Why only one card previews at a time
 * Each preview is a live HLS stream. Two hard limits make "preview everything
 * on screen" unworkable:
 *
 *  - **Hardware decoders.** A phone has a handful of concurrent video decoder
 *    instances. Exceeding them makes MediaCodec throw, and the failure mode is
 *    every preview dying at once, not a graceful degrade.
 *  - **Bandwidth.** Six simultaneous live streams is 15-30 Mbps sustained, and
 *    on mobile data that is somebody's bill.
 *
 * [MAX_CONCURRENT_PREVIEWS] documents the pool size. Raising it means holding a
 * list of players here rather than one; the card API already takes a per-card
 * player so nothing else would need to change.
 *
 * ## Sequence
 * ```
 * card becomes centred
 *   -> 350ms settle   (a fling passes over dozens of cards; ignore them)
 *   -> player prepares, muted, rendering into the card behind the logo
 *   -> logo holds for LOGO_DWELL_MS
 *   -> on first rendered frame, crossfade logo out / video in
 * ```
 * The player starts during the logo dwell rather than after it, so the video is
 * already rendering when the dissolve begins - otherwise the fade would reveal
 * a black rectangle and then pop. If the stream never produces a frame (dead
 * link, geo-block, timeout) the logo simply stays put.
 */
@Stable
@UnstableApi
class ChannelPreviewController(
    private val context: Context,
    private val scope: CoroutineScope
) {

    /** The channel currently being previewed, or null. */
    var activeChannelId: String? by mutableStateOf(null)
        private set

    /** True once the video is rendering and the dissolve should run. */
    var isRevealed: Boolean by mutableStateOf(false)
        private set

    /**
     * The last channel that never produced a frame. Public playlists are full
     * of dead links, so callers that pick a channel themselves (the home hero)
     * can watch this and roll again instead of sitting on a stuck logo.
     */
    var failedChannelId: String? by mutableStateOf(null)
        private set

    /**
     * Must be Compose state, not a plain var. The player is created ~350ms
     * after focus, inside a coroutine. If that assignment does not trigger
     * recomposition the card never re-reads [playerFor], the PlayerView is
     * never attached, and with no surface the decoder cannot render a frame -
     * so onRenderedFirstFrame never fires and the dissolve never starts.
     */
    private var player: ExoPlayer? by mutableStateOf(null)

    private var job: Job? = null
    private val firstFrameRendered = MutableStateFlow(false)

    private val listener = object : Player.Listener {
        override fun onRenderedFirstFrame() {
            firstFrameRendered.value = true
        }

        override fun onPlayerError(error: PlaybackException) {
            // Dead streams are routine in public playlists. Keep the logo up
            // and move on silently rather than flashing an error in a tile.
            firstFrameRendered.value = false
        }
    }

    /** The player to attach, but only for the card that owns the preview. */
    fun playerFor(channelId: String): ExoPlayer? =
        if (channelId == activeChannelId) player else null

    /**
     * Points the preview at [channel], or tears it down when null. Repeated
     * calls for the same channel are ignored, so this is safe to call from a
     * recomposing LaunchedEffect.
     */
    fun focus(channel: Channel?) {
        if (channel == null) {
            stop()
            return
        }
        if (channel.id == activeChannelId) return

        job?.cancel()
        releasePlayer()

        activeChannelId = channel.id
        isRevealed = false
        failedChannelId = null
        firstFrameRendered.value = false

        job = scope.launch {
            // Let a fling settle before committing a decoder to anything.
            delay(SETTLE_MS)

            val exo = createPlayer()
            player = exo
            exo.setMediaItem(MediaItem.fromUri(channel.streamUrl))
            exo.prepare()
            exo.playWhenReady = true
            Log.d(TAG, "preparing preview for ${channel.name}")

            // Hold the logo for its full dwell, then wait for real video.
            delay(LOGO_DWELL_MS)
            val rendered = withTimeoutOrNull(FIRST_FRAME_TIMEOUT_MS) {
                firstFrameRendered.first { it }
            }

            if (rendered == true && activeChannelId == channel.id) {
                Log.d(TAG, "revealing ${channel.name}")
                isRevealed = true
            } else {
                // No frame arrived; drop the decoder rather than hold it open.
                Log.d(TAG, "no frame from ${channel.name}; keeping logo")
                failedChannelId = channel.id
                releasePlayer()
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        activeChannelId = null
        isRevealed = false
        firstFrameRendered.value = false
        releasePlayer()
    }

    fun release() = stop()

    private fun createPlayer(): ExoPlayer {
        // Small buffers: a preview needs to start fast, and never needs to
        // survive a long stall - it will be torn down on the next scroll.
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 1_000,
                /* maxBufferMs = */ 6_000,
                /* bufferForPlaybackMs = */ 500,
                /* bufferForPlaybackAfterRebufferMs = */ 1_000
            )
            .build()

        return ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build()
            .apply {
                volume = 0f                       // previews are always silent
                repeatMode = Player.REPEAT_MODE_OFF
                // Silencing with volume alone still decodes the audio track.
                // Disabling it outright saves a codec instance and the battery
                // cost of decoding samples nobody will ever hear.
                trackSelectionParameters = trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                    .build()
                // Never take audio focus: a preview must not pause the user's music.
                setAudioAttributes(audioAttributes, /* handleAudioFocus = */ false)
                addListener(listener)
            }
    }

    private fun releasePlayer() {
        player?.let { exo ->
            exo.removeListener(listener)
            exo.stop()
            exo.release()
        }
        player = null
    }

    private companion object {
        const val TAG = "StreamCastPreview"

        /** Raise this only alongside a real player pool. See the class note. */
        const val MAX_CONCURRENT_PREVIEWS = 1

        const val SETTLE_MS = 350L
        const val LOGO_DWELL_MS = 5_000L
        const val FIRST_FRAME_TIMEOUT_MS = 12_000L

        val audioAttributes: androidx.media3.common.AudioAttributes =
            androidx.media3.common.AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build()
    }
}

/**
 * The video half of a preview tile.
 *
 * Callers draw their own logo on top at the inverse alpha; this renders the
 * stream underneath. The view is attached for the whole preview, not only once
 * revealed: the decoder needs a surface before it can produce the first frame
 * that triggers the dissolve.
 */
@OptIn(UnstableApi::class)
@Composable
fun PreviewVideoLayer(
    player: ExoPlayer?,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    if (player == null) return
    AndroidView(
        modifier = modifier.alpha(alpha),
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                setKeepContentOnPlayerReset(true)
            }
        },
        update = { view -> view.player = player },
        onRelease = { view -> view.player = null }
    )
}

/**
 * Creates a controller bound to the composition, releasing its decoder when the
 * screen leaves. Never leave an ExoPlayer alive past its screen - it holds a
 * decoder and a socket open.
 */
@UnstableApi
@Composable
fun rememberChannelPreviewController(scope: CoroutineScope): ChannelPreviewController {
    val context = LocalContext.current
    val controller = remember(context) { ChannelPreviewController(context, scope) }
    DisposableEffect(controller) {
        onDispose { controller.release() }
    }
    return controller
}
