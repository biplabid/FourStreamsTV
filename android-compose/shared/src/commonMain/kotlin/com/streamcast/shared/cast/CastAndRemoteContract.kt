package com.streamcast.shared.cast

import com.streamcast.shared.model.Channel
import com.streamcast.shared.model.DiscoveryState
import com.streamcast.shared.model.TvDevice
import kotlinx.coroutines.flow.StateFlow

/**
 * Keys a sender may ask a receiver to act on.
 *
 * Google Cast only carries media transport and volume. The D-pad, power and
 * voice keys belong to the Android TV Remote v2 protocol (TLS certificate
 * pairing on ports 6466/6467), which is a separate implementation; the Cast
 * controller rejects them rather than silently ignoring them.
 */
enum class RemoteKeyCode(val supportedOverCast: Boolean) {
    // Supported over Google Cast
    MEDIA_PLAY_PAUSE(true),
    MEDIA_STOP(true),
    VOLUME_UP(true),
    VOLUME_DOWN(true),
    VOLUME_MUTE(true),
    SEEK_TO_LIVE(true),

    // Require Android TV Remote v2
    DPAD_UP(false),
    DPAD_DOWN(false),
    DPAD_LEFT(false),
    DPAD_RIGHT(false),
    DPAD_CENTER(false),
    BACK(false),
    HOME(false),
    MENU(false),
    POWER(false),
    CHANNEL_UP(false),
    CHANNEL_DOWN(false),
    VOICE_ASSIST(false)
}

/**
 * Discovery, connection and playback control for an external TV receiver.
 *
 * Implemented by [com.streamcast.mobile.cast.AndroidCastAndRemoteManager] on
 * top of the Google Cast SDK and MediaRouter.
 */
interface CastAndRemoteController {
    val discoveryState: StateFlow<DiscoveryState>
    val connectedDevice: StateFlow<TvDevice?>

    fun startDiscovery()
    fun stopDiscovery()

    /** Resolves only once the receiver has confirmed or refused the session. */
    suspend fun connect(device: TvDevice): Result<Unit>
    suspend fun disconnect()

    /** Queues the channel if no session exists yet, then loads it on connect. */
    suspend fun castChannel(channel: Channel): Result<Unit>

    /** Fails for keys the active protocol cannot carry. */
    suspend fun sendKey(keyCode: RemoteKeyCode): Result<Unit>
    suspend fun sendText(text: String): Result<Unit>
}
