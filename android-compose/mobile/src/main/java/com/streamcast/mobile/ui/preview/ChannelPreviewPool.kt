package com.streamcast.mobile.ui.preview

import android.content.Context
import android.media.MediaCodecList
import android.net.ConnectivityManager
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import com.streamcast.shared.model.Channel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.min

/**
 * Builds a muted, low-footprint player for preview tiles.
 *
 * The resolution ceiling is what makes several simultaneous previews possible.
 * A 160dp tile does not need a 1080p ladder rung: capping the track selection
 * cuts both the bitrate pulled off the network and the work the hardware
 * decoder does, by roughly an order of magnitude per tile. DefaultTrackSelector
 * exceeds these constraints rather than failing when a stream offers nothing
 * smaller, so single-variant streams still play.
 */
@UnstableApi
internal fun createPreviewPlayer(
    context: Context,
    maxWidth: Int,
    maxHeight: Int,
    maxBitrate: Int
): ExoPlayer {
    // Small buffers: a preview must start fast and is torn down on the next
    // scroll, so it never needs to survive a long stall.
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
            volume = 0f
            repeatMode = Player.REPEAT_MODE_OFF
            trackSelectionParameters = trackSelectionParameters
                .buildUpon()
                // Silencing with volume alone still decodes audio. Disabling the
                // track outright frees a codec instance per tile - which is the
                // difference between six previews working and six fighting.
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                .setMaxVideoSize(maxWidth, maxHeight)
                .setMaxVideoBitrate(maxBitrate)
                .build()
            // Never take audio focus: a preview must not pause the user's music.
            setAudioAttributes(
                androidx.media3.common.AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                /* handleAudioFocus = */ false
            )
        }
}

/**
 * How many previews this particular phone can actually run.
 *
 * Asked of the hardware rather than guessed: MediaCodec publishes
 * maxSupportedInstances per codec, and exceeding it does not degrade
 * gracefully - allocation throws and tiles die in a heap. Headroom is reserved
 * for the foreground player and anything else on the device holding a decoder.
 */
internal fun deviceVideoDecoderBudget(context: Context): Int {
    val reported = runCatching {
        MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
            .asSequence()
            .filterNot { it.isEncoder }
            .flatMap { info ->
                info.supportedTypes.asSequence()
                    .filter { it.equals("video/avc", ignoreCase = true) }
                    .mapNotNull { type ->
                        runCatching {
                            info.getCapabilitiesForType(type).maxSupportedInstances
                        }.getOrNull()
                    }
            }
            .maxOrNull()
    }.getOrNull() ?: DEFAULT_DECODER_ESTIMATE

    // On mobile data every extra tile is a live stream billed to the user, so
    // the generous budget is reserved for Wi-Fi and other unmetered networks.
    val metered = runCatching {
        (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
            .isActiveNetworkMetered
    }.getOrDefault(true)
    val ceiling = if (metered) METERED_PREVIEW_CEILING else MAX_VISIBLE_PREVIEWS

    val budget = min(ceiling, (reported - DECODER_HEADROOM)).coerceAtLeast(1)
    Log.d(POOL_TAG, "decoder instances reported=$reported, metered=$metered, preview budget=$budget")
    return budget
}

private const val POOL_TAG = "StreamCastPool"
private const val DEFAULT_DECODER_ESTIMATE = 6
/**
 * Decoders left free for the full player, the Home hero and the system. Two
 * proved too few: with the pool at 30 of 32, opening a channel made the
 * platform reclaim a decoder out from under a running preview.
 */
private const val DECODER_HEADROOM = 4

/**
 * Ceiling regardless of what the hardware claims. HLS itself sets no limit on
 * concurrent streams; the practical limits are hardware decoder instances
 * (32 on a Tab S11 Ultra, so 28 after headroom), memory and bandwidth. 28
 * covers every card a landscape tablet shows at once (7 x 4).
 */
const val MAX_VISIBLE_PREVIEWS = 28

/** On metered networks, previews stay at the old conservative level. */
private const val METERED_PREVIEW_CEILING = 6

/** Above this many tiles, each drops to a lighter rendition. */
private const val LIGHT_TILE_THRESHOLD = 8

/**
 * Runs a live preview on every card currently in view.
 *
 * Each visible channel gets its own slot: settle, prepare, hold the logo, then
 * dissolve on the first real frame - the same sequence as a single preview, run
 * concurrently. Slots are created and torn down as cards scroll in and out, and
 * the pool never exceeds [budget] players at once.
 *
 * Starts are staggered so six tiles do not simultaneously fetch a manifest and
 * claim a decoder, which produces a visible stall and makes allocation failures
 * far more likely.
 */
@Stable
@UnstableApi
class ChannelPreviewPool(
    private val context: Context,
    private val scope: CoroutineScope,
    /** Most previews this device runs at once. */
    val budget: Int
) {

    // Many small tiles at once: ask for a lighter rendition per tile so the
    // total bitrate and decode load stay sane.
    private val light = budget > LIGHT_TILE_THRESHOLD
    private val tileWidth = if (light) 426 else TILE_WIDTH
    private val tileHeight = if (light) 240 else TILE_HEIGHT
    private val tileBitrate = if (light) 700_000 else TILE_BITRATE

    private inner class Slot(val channel: Channel) {
        var player: ExoPlayer? by mutableStateOf(null)
        var revealed: Boolean by mutableStateOf(false)
        var job: Job? = null
        val firstFrame = MutableStateFlow(false)

        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                firstFrame.value = true
            }

            override fun onPlayerError(error: PlaybackException) {
                // Dead links are routine; the tile keeps its logo.
                Log.d(POOL_TAG, "preview error on ${channel.name}: ${error.errorCodeName}")
                firstFrame.value = false
            }
        }

        fun start(staggerIndex: Int) {
            job = scope.launch {
                delay(SETTLE_MS + staggerIndex * STAGGER_MS)

                val exo = runCatching {
                    createPreviewPlayer(context, tileWidth, tileHeight, tileBitrate)
                }.getOrElse { error ->
                    // Decoder exhaustion lands here. One tile stays on its logo
                    // rather than taking the whole grid down.
                    Log.w(POOL_TAG, "could not create preview player", error)
                    return@launch
                }

                exo.addListener(listener)
                player = exo
                exo.setMediaItem(MediaItem.fromUri(channel.streamUrl))
                exo.prepare()
                exo.playWhenReady = true

                delay(LOGO_DWELL_MS)
                val rendered = withTimeoutOrNull(FIRST_FRAME_TIMEOUT_MS) {
                    firstFrame.first { it }
                }

                if (rendered == true) {
                    revealed = true
                } else {
                    // Release the decoder so a live tile elsewhere can have it.
                    Log.d(POOL_TAG, "no frame from ${channel.name}; releasing slot")
                    releasePlayer()
                }
            }
        }

        fun releasePlayer() {
            player?.let { exo ->
                runCatching {
                    exo.removeListener(listener)
                    exo.stop()
                    exo.release()
                }
            }
            player = null
        }

        fun dispose() {
            job?.cancel()
            job = null
            revealed = false
            releasePlayer()
        }
    }

    private val slots = mutableStateMapOf<String, Slot>()

    fun playerFor(channelId: String): ExoPlayer? = slots[channelId]?.player

    fun isRevealed(channelId: String): Boolean = slots[channelId]?.revealed == true

    /**
     * Declares the channels currently on screen. Slots for channels that have
     * scrolled away are released; newly visible ones start. Channels already
     * previewing are left alone, so scrolling by one row does not restart the
     * five tiles that stayed put.
     */
    fun setVisible(channels: List<Channel>) {
        val wanted = channels.take(budget).associateBy { it.id }

        val gone = slots.keys - wanted.keys
        gone.forEach { id ->
            slots.remove(id)?.dispose()
        }

        var staggerIndex = 0
        wanted.forEach { (id, channel) ->
            if (!slots.containsKey(id)) {
                val slot = Slot(channel)
                slots[id] = slot
                slot.start(staggerIndex++)
            }
        }
    }

    fun clear() {
        slots.values.forEach { it.dispose() }
        slots.clear()
    }

    private companion object {
        const val SETTLE_MS = 350L
        const val STAGGER_MS = 110L
        const val LOGO_DWELL_MS = 5_000L
        const val FIRST_FRAME_TIMEOUT_MS = 12_000L

        /** Preview tiles are ~160dp wide; 360p is already generous. */
        const val TILE_WIDTH = 640
        const val TILE_HEIGHT = 360
        const val TILE_BITRATE = 1_200_000
    }
}

@UnstableApi
@Composable
fun rememberChannelPreviewPool(scope: CoroutineScope): ChannelPreviewPool {
    val context = LocalContext.current
    val pool = remember(context) {
        ChannelPreviewPool(context, scope, deviceVideoDecoderBudget(context))
    }
    DisposableEffect(pool) {
        onDispose { pool.clear() }
    }
    return pool
}
