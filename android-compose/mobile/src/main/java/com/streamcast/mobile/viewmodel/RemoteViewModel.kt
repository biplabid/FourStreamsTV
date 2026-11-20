package com.streamcast.mobile.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.streamcast.mobile.AppGraph
import com.streamcast.mobile.atv.AtvDevice
import com.streamcast.mobile.atv.AtvSessionState
import com.streamcast.shared.atv.AtvKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Screen state for the Android TV remote.
 *
 * The controller is process-wide: a session is one TCP connection the TV will
 * not duplicate, so it must outlive this ViewModel and survive navigation away
 * from the remote tab and back.
 */
class RemoteViewModel(application: Application) : AndroidViewModel(application) {

    private val controller = AppGraph.atvRemote(application)

    val devices = controller.devices
    val searching = controller.searching
    val activeDevice = controller.activeDevice
    val sessionState = controller.sessionState
    val volume = controller.volume
    val currentApp = controller.currentApp
    val tvInfo = controller.tvInfo
    val textFieldLabel = controller.textFieldLabel
    val pairing = controller.pairing
    val voiceState = controller.voiceState
    val voiceAmplitude = controller.voiceAmplitude

    private val _message = MutableStateFlow<String?>(null)

    /** A one-off error for the UI to show and then clear. */
    val message: StateFlow<String?> = _message.asStateFlow()

    /** True while a connection attempt is outstanding. */
    private val _connecting = MutableStateFlow(false)
    val connecting: StateFlow<Boolean> = _connecting.asStateFlow()

    val isReady: Boolean get() = sessionState.value is AtvSessionState.Ready

    init {
        // Reconnecting to the last TV on open is what makes this feel like a
        // remote rather than an app: pick it up, press a button.
        controller.preferredDevice()?.let { device ->
            viewModelScope.launch { connect(device) }
        }
    }

    fun startDiscovery() = controller.startDiscovery()

    fun stopDiscovery() = controller.stopDiscovery()

    fun connect(device: AtvDevice) {
        if (_connecting.value) return
        viewModelScope.launch {
            _connecting.value = true
            val result = controller.connect(device)
            _connecting.value = false
            result.exceptionOrNull()?.let { _message.value = it.message }
        }
    }

    fun disconnect() = controller.disconnect()

    fun beginPairing(device: AtvDevice) {
        viewModelScope.launch { controller.beginPairing(device) }
    }

    fun submitPairingCode(code: String) {
        viewModelScope.launch { controller.submitPairingCode(code) }
    }

    fun cancelPairing() = controller.cancelPairing()

    /** Connects straight after a successful pairing, so the remote just works. */
    fun connectAfterPairing(device: AtvDevice) {
        controller.cancelPairing()
        connect(device)
    }

    fun forget(device: AtvDevice) = controller.forget(device)

    fun resetIdentity() {
        viewModelScope.launch {
            controller.resetIdentity()
            _message.value = "Forgot this phone's identity. Pair again to reconnect."
        }
    }

    // ----------------------------------------------------------- commands

    fun key(key: AtvKey) = controller.key(key)

    fun beginLongPress(key: AtvKey) = controller.beginLongPress(key)

    fun endLongPress(key: AtvKey) = controller.endLongPress(key)

    fun sendText(text: String) = controller.text(text)

    fun launchApp(uri: String) = controller.launchApp(uri)

    fun supports(feature: Int): Boolean = controller.supports(feature)

    /**
     * Starts hold-to-talk.
     *
     * The permission check is passed in rather than read here, because only
     * the Activity can ask for it and the answer may arrive after this call.
     */
    fun startVoice(hasMicrophonePermission: Boolean) {
        viewModelScope.launch { controller.startVoice(hasMicrophonePermission) }
    }

    fun stopVoice() = controller.stopVoice()

    fun clearVoiceState() = controller.clearVoiceState()

    fun consumeMessage() {
        _message.value = null
    }

    override fun onCleared() {
        // The session deliberately outlives this ViewModel; only discovery,
        // which costs battery and multicast traffic, is stopped here.
        controller.stopDiscovery()
        super.onCleared()
    }
}
