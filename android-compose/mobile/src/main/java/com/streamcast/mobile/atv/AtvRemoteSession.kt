package com.streamcast.mobile.atv

import android.util.Log
import com.streamcast.shared.atv.AtvKey
import com.streamcast.shared.atv.RemoteProto
import com.streamcast.shared.atv.VarintFrameBuffer
import com.streamcast.shared.atv.frameMessage
import java.net.InetSocketAddress
import java.net.Socket
import java.security.cert.X509Certificate
import javax.net.ssl.SSLSocket
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** Where a remote session currently stands. */
sealed interface AtvSessionState {
    data object Idle : AtvSessionState
    data object Connecting : AtvSessionState

    /** Handshake done; the TV is taking commands. */
    data class Ready(val screenOn: Boolean) : AtvSessionState

    /** Finished, cleanly or otherwise. [reason] is null for a deliberate hang-up. */
    data class Disconnected(val reason: String?) : AtvSessionState
}

/**
 * A live connection to the Android TV Remote Service, on port 6466.
 *
 * ## Handshake
 * ```
 * <- RemoteConfigure   the TV lists the features it supports
 * -> RemoteConfigure   we reply with the intersection of those and ours
 * <- RemoteSetActive
 * -> RemoteSetActive   confirming the same set
 * <- RemoteStart       ready; `started` also tells us if the screen is on
 * ```
 * Asking for a capability the TV did not offer gets the socket closed with no
 * explanation, so the feature mask is negotiated rather than assumed.
 *
 * ## Staying alive
 * The TV pings roughly every five seconds and hangs up after three unanswered
 * pings. Those pings are the only liveness signal in the protocol, so a gap in
 * them is how a TV that was switched off at the wall is detected - nothing else
 * reports it.
 */
class AtvRemoteSession(
    private val credentials: AtvCredentials,
    private val host: String,
    private val pinnedServerCertificate: ByteArray?,
    private val packageName: String,
    private val appVersion: String,
    private val scope: CoroutineScope,
    /**
     * What to ask the TV for. Voice is only included once the microphone
     * permission exists, because negotiating it changes what the search key
     * does - see [RemoteProto.Feature.REQUESTED_WITH_VOICE].
     */
    private val requestedFeatures: Int = RemoteProto.Feature.REQUESTED_BASE
) {

    private val _state = MutableStateFlow<AtvSessionState>(AtvSessionState.Idle)
    val state: StateFlow<AtvSessionState> = _state.asStateFlow()

    private val _volume = MutableStateFlow<RemoteProto.VolumeInfo?>(null)
    val volume: StateFlow<RemoteProto.VolumeInfo?> = _volume.asStateFlow()

    private val _currentApp = MutableStateFlow<String?>(null)
    val currentApp: StateFlow<String?> = _currentApp.asStateFlow()

    private val _deviceInfo = MutableStateFlow<RemoteProto.TvDeviceInfo?>(null)
    val deviceInfo: StateFlow<RemoteProto.TvDeviceInfo?> = _deviceInfo.asStateFlow()

    /** Set when the TV asks for text, so the UI can offer a keyboard. */
    private val _textFieldLabel = MutableStateFlow<String?>(null)
    val textFieldLabel: StateFlow<String?> = _textFieldLabel.asStateFlow()

    /** The certificate the TV presented, for pinning on first connection. */
    @Volatile
    var observedServerCertificate: X509Certificate? = null
        private set

    private var socket: SSLSocket? = null
    private var readJob: Job? = null
    private var writeJob: Job? = null
    private var watchdogJob: Job? = null

    private val outbox = Channel<ByteArray>(capacity = 64)
    private val ready = CompletableDeferred<Unit>()

    @Volatile
    private var activeFeatures = requestedFeatures

    /** Resolved when the TV answers KEYCODE_SEARCH with a voice session id. */
    @Volatile
    private var voiceBegin: CompletableDeferred<Int>? = null

    @Volatile
    private var voiceSessionId: Int? = null

    @Volatile
    private var lastMessageAt = 0L

    // The TV tells us which field is focused; text edits are addressed to it.
    @Volatile
    private var imeCounter = 0

    @Volatile
    private var fieldCounter = 0

    /**
     * Opens the connection and returns once the TV reports it is ready.
     *
     * A failure here is usually one of three things: the TV is off, it is on a
     * different network, or it has forgotten our certificate - the last of
     * which needs pairing again rather than a retry.
     */
    suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        if (_state.value is AtvSessionState.Ready) return@withContext Result.success(Unit)
        _state.value = AtvSessionState.Connecting

        val (context, trust) = credentials.sslContext(pinnedServerCertificate)

        val raw = Socket()
        try {
            raw.connect(InetSocketAddress(host, REMOTE_PORT), CONNECT_TIMEOUT_MS)
        } catch (e: Exception) {
            return@withContext fail("The TV is not responding. Is it switched on?", e)
        }

        val tls = try {
            (context.socketFactory.createSocket(raw, host, REMOTE_PORT, true) as SSLSocket).apply {
                soTimeout = SOCKET_READ_TIMEOUT_MS
                startHandshake()
            }
        } catch (e: Exception) {
            runCatching { raw.close() }
            val message = if (pinnedServerCertificate != null && e.message?.contains("not the TV") == true) {
                e.message!!
            } else {
                "The TV would not accept this phone. It may have forgotten the pairing - " +
                    "pair again to fix it."
            }
            return@withContext fail(message, e)
        }

        socket = tls
        observedServerCertificate = trust.seenCertificate
        lastMessageAt = System.currentTimeMillis()

        startWriter(tls)
        startReader(tls)
        startWatchdog()

        // The TV opens the conversation, so there is nothing to send yet.
        val started = withTimeoutOrNull(HANDSHAKE_TIMEOUT_MS) { ready.await() }
        if (started == null) {
            teardown("The TV did not finish the handshake")
            return@withContext Result.failure(
                AtvRemoteException("The TV accepted the connection but never became ready")
            )
        }

        Result.success(Unit)
    }

    /** Ends the session. Safe to call more than once. */
    fun disconnect() {
        if (_state.value is AtvSessionState.Disconnected) return
        teardown(reason = null)
    }

    // ------------------------------------------------------------ commands

    fun sendKey(key: AtvKey, direction: RemoteProto.Direction = RemoteProto.Direction.SHORT) {
        if (!hasFeature(RemoteProto.Feature.KEY)) {
            Log.w(TAG, "TV did not negotiate key support; dropping ${key.name}")
            return
        }
        enqueue(RemoteProto.keyInject(key.code, direction))
    }

    /** A press-and-hold, as the TV understands it: a start now, an end later. */
    fun beginLongPress(key: AtvKey) = sendKey(key, RemoteProto.Direction.START_LONG)

    fun endLongPress(key: AtvKey) = sendKey(key, RemoteProto.Direction.END_LONG)

    /**
     * Types [text] into whatever field has focus on the TV.
     *
     * Does nothing useful if no field is focused - there is nowhere for the
     * characters to go, and the protocol gives no error for it.
     */
    fun sendText(text: String) {
        if (text.isEmpty()) return
        if (!hasFeature(RemoteProto.Feature.IME)) {
            Log.w(TAG, "TV did not negotiate IME support; dropping text")
            return
        }
        enqueue(RemoteProto.imeInsertText(text, imeCounter, fieldCounter))
    }

    fun launchApp(uri: String) {
        if (!hasFeature(RemoteProto.Feature.APP_LINK)) {
            Log.w(TAG, "TV did not negotiate app links; dropping $uri")
            return
        }
        enqueue(RemoteProto.appLinkLaunch(uri))
    }

    fun hasFeature(feature: Int): Boolean = activeFeatures and feature != 0

    // --------------------------------------------------------------- voice

    /**
     * Opens a voice session and returns its id, or null if the TV would not
     * grant one.
     *
     * The handshake is indirect: there is no "start voice" message. Pressing
     * KEYCODE_SEARCH is what asks, and a TV that negotiated the VOICE feature
     * answers with RemoteVoiceBegin carrying a session id. We echo that id back
     * in our own RemoteVoiceBegin, and only then will it accept audio.
     *
     * A TV that did not negotiate voice simply opens its search screen and
     * never replies, which is why this times out rather than waiting.
     */
    suspend fun startVoice(): Int? {
        if (!hasFeature(RemoteProto.Feature.VOICE)) {
            Log.w(TAG, "TV did not negotiate voice support")
            return null
        }
        if (voiceSessionId != null) {
            Log.w(TAG, "A voice session is already open")
            return null
        }

        val pending = CompletableDeferred<Int>()
        voiceBegin = pending

        sendKey(AtvKey.SEARCH)
        val id = withTimeoutOrNull(VOICE_BEGIN_TIMEOUT_MS) { pending.await() }
        voiceBegin = null

        if (id == null) {
            Log.w(TAG, "The TV did not open a voice session")
            return null
        }

        enqueue(RemoteProto.voiceBegin(id))
        voiceSessionId = id
        Log.d(TAG, "Voice session $id open")
        return id
    }

    /**
     * Streams one piece of microphone audio.
     *
     * Chunks larger than the protocol's ceiling get the connection closed, and
     * at least one device rejects very small ones, so the data is split and
     * padded to stay inside both bounds.
     */
    fun sendVoiceChunk(samples: ByteArray) {
        val id = voiceSessionId ?: return

        var offset = 0
        while (offset < samples.size) {
            val end = minOf(offset + RemoteProto.Voice.MAX_CHUNK_BYTES, samples.size)
            var piece = samples.copyOfRange(offset, end)
            offset = end

            if (piece.size < RemoteProto.Voice.MIN_CHUNK_BYTES) {
                // Pad with silence rather than drop the tail of what was said.
                piece = piece.copyOf(RemoteProto.Voice.MIN_CHUNK_BYTES)
            }
            enqueue(RemoteProto.voicePayload(id, piece))
        }
    }

    /** Closes the session so the TV acts on what it heard. */
    fun endVoice() {
        val id = voiceSessionId ?: return
        voiceSessionId = null
        enqueue(RemoteProto.voiceEnd(id))
        Log.d(TAG, "Voice session $id closed")
    }

    val isVoiceOpen: Boolean get() = voiceSessionId != null

    private fun enqueue(message: ByteArray) {
        if (_state.value !is AtvSessionState.Ready) {
            Log.w(TAG, "Not ready; dropping a command")
            return
        }
        outbox.trySend(message)
    }

    // ------------------------------------------------------------- loops

    private fun startWriter(tls: SSLSocket) {
        writeJob = scope.launch(Dispatchers.IO) {
            // One writer keeps frames whole: two coroutines writing to the same
            // stream would interleave bytes and desynchronise the TV.
            for (message in outbox) {
                try {
                    tls.outputStream.apply {
                        write(frameMessage(message))
                        flush()
                    }
                } catch (e: Exception) {
                    if (isActive) teardown("Lost the connection to the TV")
                    return@launch
                }
            }
        }
    }

    private fun startReader(tls: SSLSocket) {
        readJob = scope.launch(Dispatchers.IO) {
            val frames = VarintFrameBuffer()
            val chunk = ByteArray(16 * 1024)

            while (isActive) {
                val read = try {
                    tls.inputStream.read(chunk)
                } catch (e: java.net.SocketTimeoutException) {
                    // No traffic for a while. The watchdog decides if that is fatal.
                    continue
                } catch (e: Exception) {
                    if (isActive) teardown("Lost the connection to the TV")
                    return@launch
                }

                if (read < 0) {
                    teardown("The TV closed the connection")
                    return@launch
                }

                lastMessageAt = System.currentTimeMillis()
                val parsed = try {
                    frames.append(chunk, read).map(RemoteProto::parse)
                } catch (e: Exception) {
                    teardown("The TV sent something unreadable")
                    return@launch
                }
                parsed.forEach(::handle)
            }
        }
    }

    /**
     * Treats a silent connection as a dead one.
     *
     * TCP will happily hold a socket open to a TV that has been unplugged, so
     * the absence of the TV's own pings is the only timely signal that it has
     * gone away.
     */
    private fun startWatchdog() {
        watchdogJob = scope.launch {
            while (isActive) {
                delay(WATCHDOG_INTERVAL_MS)
                val silence = System.currentTimeMillis() - lastMessageAt
                if (silence > IDLE_TIMEOUT_MS) {
                    teardown("The TV stopped responding")
                    return@launch
                }
            }
        }
    }

    private fun handle(incoming: RemoteProto.Incoming) {
        when (incoming) {
            is RemoteProto.Incoming.Configure -> {
                _deviceInfo.value = incoming.deviceInfo
                // Only ask for what the device actually offers.
                activeFeatures = requestedFeatures and incoming.supportedFeatures
                Log.d(
                    TAG,
                    "TV ${incoming.deviceInfo.vendor} ${incoming.deviceInfo.model} " +
                        "supports 0x${incoming.supportedFeatures.toString(16)}, " +
                        "using 0x${activeFeatures.toString(16)}"
                )
                send(
                    RemoteProto.configure(
                        activeFeatures = activeFeatures,
                        model = ThisDevice.model,
                        vendor = ThisDevice.vendor,
                        packageName = packageName,
                        appVersion = appVersion
                    )
                )
            }

            RemoteProto.Incoming.SetActive -> send(RemoteProto.setActive(activeFeatures))

            is RemoteProto.Incoming.Start -> {
                _state.value = AtvSessionState.Ready(screenOn = incoming.started)
                if (!ready.isCompleted) ready.complete(Unit)
            }

            is RemoteProto.Incoming.PingRequest -> send(RemoteProto.pingResponse(incoming.val1))

            is RemoteProto.Incoming.Volume -> _volume.value = incoming.info

            is RemoteProto.Incoming.ImeKeyInject -> {
                incoming.appPackage?.takeIf { it.isNotBlank() }?.let { _currentApp.value = it }
                if (incoming.imeCounter != 0) imeCounter = incoming.imeCounter
                if (incoming.fieldCounter != 0) fieldCounter = incoming.fieldCounter
                _textFieldLabel.value = incoming.fieldLabel
            }

            is RemoteProto.Incoming.ImeShowRequest -> {
                if (incoming.fieldCounter != 0) fieldCounter = incoming.fieldCounter
                _textFieldLabel.value = incoming.fieldLabel
            }

            is RemoteProto.Incoming.ImeBatchEdit -> {
                // Fallback source for the counters when no key inject arrived.
                if (imeCounter == 0) imeCounter = incoming.imeCounter
                if (fieldCounter == 0) fieldCounter = incoming.fieldCounter
            }

            is RemoteProto.Incoming.VoiceBegin -> {
                // The TV grants a session id in response to KEYCODE_SEARCH.
                voiceBegin?.takeIf { !it.isCompleted }?.complete(incoming.sessionId)
            }

            is RemoteProto.Incoming.Error -> Log.w(TAG, incoming.detail)

            is RemoteProto.Incoming.Unknown ->
                Log.d(TAG, "Ignoring message with fields ${incoming.fields}")
        }
    }

    /** Handshake replies bypass the readiness check that [enqueue] applies. */
    private fun send(message: ByteArray) {
        outbox.trySend(message)
    }

    private fun teardown(reason: String?) {
        if (_state.value is AtvSessionState.Disconnected) return
        _state.value = AtvSessionState.Disconnected(reason)
        reason?.let { Log.w(TAG, "Session ended: $it") }

        watchdogJob?.cancel()
        readJob?.cancel()
        writeJob?.cancel()
        outbox.close()
        runCatching { socket?.close() }
        socket = null
        if (!ready.isCompleted) {
            ready.completeExceptionally(AtvRemoteException(reason ?: "Disconnected"))
        }
    }

    private fun fail(message: String, cause: Throwable): Result<Unit> {
        _state.value = AtvSessionState.Disconnected(message)
        Log.w(TAG, message, cause)
        return Result.failure(AtvRemoteException(message, cause))
    }

    companion object {
        private const val TAG = "AtvRemote"

        /** The Android TV Remote Service takes commands here. */
        const val REMOTE_PORT = 6466

        private const val CONNECT_TIMEOUT_MS = 6_000
        private const val HANDSHAKE_TIMEOUT_MS = 10_000L

        /** Longer than the ping interval, so a normal gap is not a timeout. */
        private const val SOCKET_READ_TIMEOUT_MS = 20_000
        private const val WATCHDOG_INTERVAL_MS = 4_000L

        /** Three missed pings, matching the TV's own tolerance. */
        private const val IDLE_TIMEOUT_MS = 16_000L

        /**
         * How long to wait for the TV to grant a voice session. A TV that
         * answers at all answers immediately; this only bounds the case where
         * KEYCODE_SEARCH opened an ordinary search screen instead.
         */
        private const val VOICE_BEGIN_TIMEOUT_MS = 2_500L
    }
}

class AtvRemoteException(message: String, cause: Throwable? = null) : Exception(message, cause)
