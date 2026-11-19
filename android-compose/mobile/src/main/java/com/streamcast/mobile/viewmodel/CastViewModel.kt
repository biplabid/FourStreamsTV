package com.streamcast.mobile.viewmodel

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.streamcast.mobile.cast.AndroidCastAndRemoteManager
import com.streamcast.shared.cast.RemoteKeyCode
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
    val currentCastingChannel: StateFlow<Channel?> = castManager.currentCastingChannel

    private val _hasApprovals = MutableStateFlow(false)
    val hasApprovals: StateFlow<Boolean> = _hasApprovals.asStateFlow()

    private val _isWifiConnected = MutableStateFlow(true)
    val isWifiConnected: StateFlow<Boolean> = _isWifiConnected.asStateFlow()

    private val _showCastDialog = MutableStateFlow(false)
    val showCastDialog: StateFlow<Boolean> = _showCastDialog.asStateFlow()

    private var pendingChannelToCast: Channel? = null

    fun checkApprovals(context: Context) {
        val wifiOk = castManager.isWifiConnected()
        _isWifiConnected.value = wifiOk

        val permissionOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                "android.permission.NEARBY_WIFI_DEVICES"
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }

        _hasApprovals.value = permissionOk && wifiOk
    }

    fun onPermissionsApproved(context: Context) {
        checkApprovals(context)
        if (_hasApprovals.value) {
            startDiscovery()
        }
    }

    fun openCastDialog(channelToCast: Channel? = null) {
        pendingChannelToCast = channelToCast
        _showCastDialog.value = true
        startDiscovery()
    }

    fun closeCastDialog() {
        _showCastDialog.value = false
    }

    fun startDiscovery() {
        castManager.startDiscovery()
    }

    fun stopDiscovery() {
        castManager.stopDiscovery()
    }

    fun selectDeviceToCast(device: TvDevice) {
        viewModelScope.launch {
            val result = castManager.connect(device)
            if (result.isSuccess) {
                pendingChannelToCast?.let { channel ->
                    castManager.castChannel(channel)
                    pendingChannelToCast = null
                }
            }
        }
    }

    fun disconnectDevice() {
        viewModelScope.launch {
            castManager.disconnect()
        }
    }

    fun castChannel(channel: Channel) {
        viewModelScope.launch {
            if (connectedDevice.value == null) {
                openCastDialog(channel)
            } else {
                castManager.castChannel(channel)
            }
        }
    }

    fun sendRemoteKey(key: RemoteKeyCode) {
        viewModelScope.launch {
            castManager.sendKey(key)
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val manager = AndroidCastAndRemoteManager(context.applicationContext)
            return CastViewModel(manager) as T
        }
    }
}
