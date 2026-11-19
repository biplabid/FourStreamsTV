package com.streamcast.mobile.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.streamcast.mobile.AppGraph
import com.streamcast.mobile.cast.AndroidCastAndRemoteManager
import com.streamcast.shared.cast.RemoteKeyCode
import com.streamcast.shared.model.CastPlaybackState
import com.streamcast.shared.model.Channel
import com.streamcast.shared.model.DiscoveryState
import com.streamcast.shared.model.TvDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CastViewModel(
    private val castManager: AndroidCastAndRemoteManager
) : ViewModel() {

    val discoveryState: StateFlow<DiscoveryState> = castManager.discoveryState
    val connectedDevice: StateFlow<TvDevice?> = castManager.connectedDevice
    val castingChannel: StateFlow<Channel?> = castManager.castingChannel
    val playbackState: StateFlow<CastPlaybackState> = castManager.playbackState
    val castVolume: StateFlow<Double> = castManager.castVolume
    val lastError: StateFlow<String?> = castManager.lastError

    private val _hasDiscoveryPermission = MutableStateFlow(false)
    val hasDiscoveryPermission: StateFlow<Boolean> = _hasDiscoveryPermission.asStateFlow()

    private val _isWifiConnected = MutableStateFlow(true)
    val isWifiConnected: StateFlow<Boolean> = _isWifiConnected.asStateFlow()

    private val _showCastDialog = MutableStateFlow(false)
    val showCastDialog: StateFlow<Boolean> = _showCastDialog.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /** The channel the user wanted on the TV, held until a session exists. */
    private var pendingChannel: Channel? = null

    fun initialise() = castManager.initialise()

    /**
     * The permission that gates local-network discovery differs by OS version:
     * NEARBY_WIFI_DEVICES from Android 13, fine location before that.
     */
    val requiredPermission: String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.NEARBY_WIFI_DEVICES
        } else {
            Manifest.permission.ACCESS_FINE_LOCATION
        }

    fun refreshEnvironment(context: Context) {
        _isWifiConnected.value = castManager.isWifiConnected()
        _hasDiscoveryPermission.value = ContextCompat.checkSelfPermission(
            context,
            requiredPermission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun onPermissionResult(context: Context, granted: Boolean) {
        refreshEnvironment(context)
        if (granted && _isWifiConnected.value) {
            startDiscovery()
        } else if (!granted) {
            _message.value =
                "Local network permission is needed to find Cast devices on your Wi-Fi."
        }
    }

    /** Opens the picker; [channelToCast] is loaded as soon as a TV connects. */
    fun openCastDialog(channelToCast: Channel? = null) {
        pendingChannel = channelToCast
        _showCastDialog.value = true
        startDiscovery()
    }

    fun closeCastDialog() {
        _showCastDialog.value = false
        castManager.stopDiscovery()
    }

    fun startDiscovery() {
        castManager.clearError()
        castManager.startDiscovery()
    }

    fun stopDiscovery() = castManager.stopDiscovery()

    fun connectTo(device: TvDevice) {
        viewModelScope.launch {
            castManager.connect(device).fold(
                onSuccess = {
                    _message.value = "Connected to ${device.name}"
                    pendingChannel?.let { channel ->
                        pendingChannel = null
                        castManager.castChannel(channel)
                        _message.value = "Playing ${channel.name} on ${device.name}"
                    }
                    _showCastDialog.value = false
                },
                onFailure = { error ->
                    _message.value = error.message ?: "Could not connect to ${device.name}"
                }
            )
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            val name = connectedDevice.value?.name
            castManager.disconnect()
            _message.value = name?.let { "Disconnected from $it" } ?: "Disconnected"
        }
    }

    /**
     * Casts a channel. With no TV connected this opens the picker instead, so
     * the tap is never silently dropped.
     */
    fun castChannel(channel: Channel) {
        viewModelScope.launch {
            if (connectedDevice.value == null) {
                openCastDialog(channel)
            } else {
                castManager.castChannel(channel)
                _message.value = "Playing ${channel.name} on ${connectedDevice.value?.name}"
            }
        }
    }

    fun sendKey(key: RemoteKeyCode) {
        viewModelScope.launch {
            castManager.sendKey(key).onFailure { error ->
                _message.value = error.message ?: "That control is not available."
            }
        }
    }

    fun setVolume(level: Double) = castManager.setVolume(level)

    fun consumeMessage() {
        _message.value = null
    }

    fun clearError() = castManager.clearError()

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CastViewModel(AppGraph.castManager(context)) as T
    }
}
