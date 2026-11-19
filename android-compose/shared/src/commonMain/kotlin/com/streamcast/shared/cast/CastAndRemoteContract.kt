package com.streamcast.shared.cast

import com.streamcast.shared.model.Channel
import com.streamcast.shared.model.DiscoveryState
import com.streamcast.shared.model.TvDevice
import kotlinx.coroutines.flow.StateFlow

enum class RemoteKeyCode {
    DPAD_UP,
    DPAD_DOWN,
    DPAD_LEFT,
    DPAD_RIGHT,
    DPAD_CENTER,
    BACK,
    HOME,
    MENU,
    POWER,
    VOLUME_UP,
    VOLUME_DOWN,
    VOLUME_MUTE,
    MEDIA_PLAY_PAUSE,
    MEDIA_STOP,
    CHANNEL_UP,
    CHANNEL_DOWN,
    VOICE_ASSIST
}

/**
 * Contract backed by Google Cast SDK (CastContext / MediaRouter2) for casting
 * and Android TV Remote v2 TLS pairing for Wi-Fi remote commands.
 */
interface CastAndRemoteController {
    val discoveryState: StateFlow<DiscoveryState>
    val connectedDevice: StateFlow<TvDevice?>

    fun startDiscovery()
    fun stopDiscovery()
    suspend fun connect(device: TvDevice): Result<Unit>
    suspend fun disconnect()
    suspend fun castChannel(channel: Channel): Result<Unit>
    suspend fun sendKey(keyCode: RemoteKeyCode): Result<Unit>
    suspend fun sendText(text: String): Result<Unit>
}
