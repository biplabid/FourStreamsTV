package com.streamcast.mobile.atv

import android.content.Context
import android.util.Log
import com.streamcast.shared.atv.AtvKey
import com.streamcast.shared.atv.PairingCrypto
import com.streamcast.shared.atv.RemoteProto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Where the pairing wizard currently stands. */
sealed interface PairingPhase {
    data object Idle : PairingPhase
    data class Connecting(val device: AtvDevice) : PairingPhase

    /** The TV is showing a code; waiting for the user to type it. */
    data class AwaitingCode(val device: AtvDevice, val error: String? = null) : PairingPhase

    data class Submitting(val device: AtvDevice) : PairingPhase
    data class Failed(val device: AtvDevice, val message: String) : PairingPhase
    data class Succeeded(val device: AtvDevice) : PairingPhase
}

/** Where hold-to-talk currently stands. */
sealed interface VoiceState {
    data object Idle : VoiceState

    /** Opening the session, possibly reconnecting to negotiate voice first. */
    data object Preparing : VoiceState

    /** Microphone is live and audio is going to the TV. */
    data object Listening : VoiceState

    data object NeedsPermission : VoiceState

    /** The TV would not grant a voice session. */
    data object Unsupported : VoiceState

    data class Failed(val message: String) : VoiceState
}

/**
 * Owns everything to do with driving an Android TV: finding one, pairing with
 * it, holding the session open and sending commands.
 *
 * One instance per process, because the session is a single TCP connection and
 * the TV will not hold two.
 */
class AtvRemoteController(
    context: Context,
    private val scope: CoroutineScope,
    private val packageName: String,
    private val appVersion: String
) {

    private val appContext = context.applicationContext
    private val store = AtvDeviceStore(appContext)
    private val discovery = AtvDiscovery(appContext)

    private val _paired = MutableStateFlow(store.paired())

    /**
     * Everything worth offering: paired TVs first, then anything else found on
     * the network. A paired TV that is also being advertised is marked online
     * rather than listed twice.
     */
    val devices: StateFlow<List<AtvDevice>> =
        combine(_paired, discovery.devices) { paired, found ->
            val foundByHost = found.associateBy { it.host }
            val merged = paired.map { device ->
                val live = foundByHost[device.host]
                device.copy(
                    online = live != null,
                    name = live?.name?.takeIf { it.isNotBlank() } ?: device.name
                )
            }
            val pairedHosts = merged.map { it.host }.toSet()
            merged.sortedByDescending { it.lastConnectedAt } +
                found.filterNot { it.host in pairedHosts }.sortedBy { it.name }
        }.stateIn(scope, SharingStarted.Eagerly, store.paired())

    val searching: StateFlow<Boolean> get() = discovery.searching

    private val _activeDevice = MutableStateFlow<AtvDevice?>(null)
    val activeDevice: StateFlow<AtvDevice?> = _activeDevice.asStateFlow()

    private val _sessionState = MutableStateFlow<AtvSessionState>(AtvSessionState.Idle)
    val sessionState: StateFlow<AtvSessionState> = _sessionState.asStateFlow()

    private val _volume = MutableStateFlow<RemoteProto.VolumeInfo?>(null)
    val volume: StateFlow<RemoteProto.VolumeInfo?> = _volume.asStateFlow()

    private val _currentApp = MutableStateFlow<String?>(null)
    val currentApp: StateFlow<String?> = _currentApp.asStateFlow()

    private val _tvInfo = MutableStateFlow<RemoteProto.TvDeviceInfo?>(null)
    val tvInfo: StateFlow<RemoteProto.TvDeviceInfo?> = _tvInfo.asStateFlow()

    private val _textFieldLabel = MutableStateFlow<String?>(null)
    val textFieldLabel: StateFlow<String?> = _textFieldLabel.asStateFlow()

    private val _pairing = MutableStateFlow<PairingPhase>(PairingPhase.Idle)
    val pairing: StateFlow<PairingPhase> = _pairing.asStateFlow()

    private var session: AtvRemoteSession? = null
    private var mirrorJob: Job? = null
    private var pairingSession: AtvPairingSession? = null

    val isReady: Boolean get() = _sessionState.value is AtvSessionState.Ready

    // ------------------------------------------------------------- voice

    private val recorder = VoiceRecorder(scope)

    /** Microphone loudness, for the hold-to-talk animation. */
    val voiceAmplitude: StateFlow<Float> get() = recorder.amplitude

    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    /**
     * Whether the current session negotiated voice.
     *
     * Set when connecting, because the feature set is fixed at handshake time
     * and cannot be renegotiated on a live connection.
     */
    @Volatile
    private var voiceRequested = false

    // ---------------------------------------------------------- discovery

    fun startDiscovery() = discovery.start()

    fun stopDiscovery() = discovery.stop()

    /** The TV to open the remote on, when the user has not chosen one. */
    fun preferredDevice(): AtvDevice? = store.lastUsed()

    // --------------------------------------------------------- connection

    /**
     * @param withVoice request the VOICE feature. Only pass true once the
     *        microphone permission is held: see
     *        [RemoteProto.Feature.REQUESTED_WITH_VOICE] for why it is not
     *        always on.
     */
    suspend fun connect(device: AtvDevice, withVoice: Boolean = voiceRequested): Result<Unit> {
        val stored = store.find(device.host)
        if (stored?.serverCertificate == null) {
            return Result.failure(
                AtvRemoteException("This TV has not been paired yet")
            )
        }

        disconnect()
        voiceRequested = withVoice

        val credentials = loadCredentials()
        val fresh = AtvRemoteSession(
            credentials = credentials,
            host = device.host,
            pinnedServerCertificate = stored.serverCertificate,
            packageName = packageName,
            appVersion = appVersion,
            scope = scope,
            requestedFeatures = if (withVoice) {
                RemoteProto.Feature.REQUESTED_WITH_VOICE
            } else {
                RemoteProto.Feature.REQUESTED_BASE
            }
        )
        session = fresh
        _activeDevice.value = stored.copy(name = device.name, online = true)
        mirror(fresh)

        val result = fresh.connect()
        if (result.isSuccess) {
            store.save(stored.copy(name = device.name, lastConnectedAt = System.currentTimeMillis()))
            _paired.value = store.paired()
        }
        return result
    }

    fun disconnect() {
        recorder.stop()
        _voiceState.value = VoiceState.Idle
        mirrorJob?.cancel()
        mirrorJob = null
        session?.disconnect()
        session = null
        _volume.value = null
        _currentApp.value = null
        _textFieldLabel.value = null
        _sessionState.value = AtvSessionState.Idle
    }

    /** Keeps the controller's flows pointed at whichever session is current. */
    private fun mirror(target: AtvRemoteSession) {
        mirrorJob?.cancel()
        mirrorJob = scope.launch {
            launch { target.state.collect { _sessionState.value = it } }
            launch { target.volume.collect { _volume.value = it } }
            launch { target.currentApp.collect { _currentApp.value = it } }
            launch { target.deviceInfo.collect { _tvInfo.value = it } }
            launch { target.textFieldLabel.collect { _textFieldLabel.value = it } }
        }
    }

    // ------------------------------------------------------------ pairing

    /**
     * Opens the pairing socket and asks the TV to display a code.
     *
     * The session stays open until [submitPairingCode] or [cancelPairing]; the
     * code on screen is only valid for this TLS session.
     */
    suspend fun beginPairing(device: AtvDevice) {
        cancelPairing()
        _pairing.value = PairingPhase.Connecting(device)

        val credentials = loadCredentials()
        val fresh = AtvPairingSession(
            credentials = credentials,
            host = device.host,
            clientName = CLIENT_NAME
        )
        pairingSession = fresh

        try {
            fresh.start()
            _pairing.value = PairingPhase.AwaitingCode(device)
        } catch (e: Exception) {
            Log.w(TAG, "Pairing could not start", e)
            runCatching { fresh.close() }
            pairingSession = null
            _pairing.value = PairingPhase.Failed(
                device,
                e.message ?: "Could not start pairing with this TV"
            )
        }
    }

    /**
     * Finishes pairing with the code shown on the TV.
     *
     * A mistyped code fails its checksum locally and leaves the wizard on the
     * code screen, so the user can simply try again without the TV having to
     * generate a new code.
     */
    suspend fun submitPairingCode(code: String) {
        val current = _pairing.value
        val device = when (current) {
            is PairingPhase.AwaitingCode -> current.device
            else -> return
        }
        val active = pairingSession ?: run {
            _pairing.value = PairingPhase.Failed(device, "Pairing is no longer in progress")
            return
        }

        _pairing.value = PairingPhase.Submitting(device)
        try {
            val certificate = active.submitCode(code)
            val paired = device.copy(
                name = active.serverName?.takeIf { it.isNotBlank() } ?: device.name,
                serverCertificate = certificate.encoded,
                lastConnectedAt = System.currentTimeMillis()
            )
            store.save(paired)
            _paired.value = store.paired()
            _pairing.value = PairingPhase.Succeeded(paired)
        } catch (e: PairingCrypto.InvalidPairingCode) {
            // Never sent to the TV, so the code on screen is still good.
            _pairing.value = PairingPhase.AwaitingCode(device, e.message)
            return
        } catch (e: Exception) {
            Log.w(TAG, "Pairing failed", e)
            _pairing.value = PairingPhase.Failed(device, e.message ?: "Pairing failed")
        } finally {
            if (_pairing.value !is PairingPhase.AwaitingCode) {
                runCatching { active.close() }
                pairingSession = null
            }
        }
    }

    fun cancelPairing() {
        runCatching { pairingSession?.close() }
        pairingSession = null
        _pairing.value = PairingPhase.Idle
    }

    fun forget(device: AtvDevice) {
        if (_activeDevice.value?.host == device.host) disconnect()
        store.forget(device.host)
        _paired.value = store.paired()
        _activeDevice.value = null
    }

    /**
     * Throws away this app's identity entirely.
     *
     * Needed when a TV has forgotten us but still has a stale entry: a fresh
     * certificate pairs as a new device rather than colliding with the old one.
     */
    suspend fun resetIdentity() = withContext(Dispatchers.IO) {
        disconnect()
        cancelPairing()
        store.forgetAll()
        _paired.value = emptyList()
        AtvCredentials.reset(appContext)
        credentials = null
    }

    // ----------------------------------------------------------- commands

    fun key(key: AtvKey) = session?.sendKey(key)

    fun beginLongPress(key: AtvKey) = session?.beginLongPress(key)

    fun endLongPress(key: AtvKey) = session?.endLongPress(key)

    fun text(value: String) = session?.sendText(value)

    fun launchApp(uri: String) = session?.launchApp(uri)

    fun supports(feature: Int): Boolean = session?.hasFeature(feature) ?: false

    // --------------------------------------------------------------- voice

    /**
     * Begins hold-to-talk.
     *
     * Three things have to be true before audio can flow, and each fails
     * differently: the session must have negotiated VOICE (which is decided at
     * connect time, so this may reconnect first), the TV must grant a session
     * id, and the microphone must open.
     */
    suspend fun startVoice(hasMicrophonePermission: Boolean) {
        if (!hasMicrophonePermission) {
            _voiceState.value = VoiceState.NeedsPermission
            return
        }
        val current = session
        if (current == null || _sessionState.value !is AtvSessionState.Ready) {
            _voiceState.value = VoiceState.Failed("Connect to a TV first")
            return
        }

        // Voice is agreed during the handshake, so a session opened before the
        // permission existed cannot start using it now. Reconnect once, then
        // every later hold is immediate.
        if (!current.hasFeature(RemoteProto.Feature.VOICE)) {
            val device = _activeDevice.value
            if (device == null) {
                _voiceState.value = VoiceState.Failed("Connect to a TV first")
                return
            }
            _voiceState.value = VoiceState.Preparing
            val reconnected = connect(device, withVoice = true)
            if (reconnected.isFailure) {
                _voiceState.value = VoiceState.Failed("Could not reconnect for voice")
                return
            }
            if (session?.hasFeature(RemoteProto.Feature.VOICE) != true) {
                _voiceState.value = VoiceState.Unsupported
                return
            }
        }

        val live = session ?: return
        _voiceState.value = VoiceState.Preparing

        val sessionId = live.startVoice()
        if (sessionId == null) {
            _voiceState.value = VoiceState.Unsupported
            return
        }

        val started = recorder.start { chunk -> live.sendVoiceChunk(chunk) }
        if (!started) {
            live.endVoice()
            _voiceState.value = VoiceState.Failed("Could not open the microphone")
            return
        }
        _voiceState.value = VoiceState.Listening
    }

    /** Ends hold-to-talk; the TV acts on what it heard. */
    fun stopVoice() {
        recorder.stop()
        session?.endVoice()
        _voiceState.value = VoiceState.Idle
    }

    fun clearVoiceState() {
        if (_voiceState.value !is VoiceState.Listening) _voiceState.value = VoiceState.Idle
    }

    // ------------------------------------------------------------ internals

    @Volatile
    private var credentials: AtvCredentials? = null

    /** Generating an RSA key takes a moment, so never on the main thread. */
    private suspend fun loadCredentials(): AtvCredentials =
        credentials ?: withContext(Dispatchers.IO) {
            AtvCredentials.load(appContext).also { credentials = it }
        }

    private companion object {
        const val TAG = "AtvController"

        /** Shown on the TV while pairing and in its list of paired devices. */
        const val CLIENT_NAME = "FourStreamsTV"
    }
}
