package com.streamcast.mobile.cast

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.mediarouter.media.MediaControlIntent
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.streamcast.shared.cast.CastAndRemoteController
import com.streamcast.shared.cast.RemoteKeyCode
import com.streamcast.shared.model.CastPlaybackState
import com.streamcast.shared.model.CastProtocol
import com.streamcast.shared.model.Channel
import com.streamcast.shared.model.DiscoveryState
import com.streamcast.shared.model.TvCapabilities
import com.streamcast.shared.model.TvDevice
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.Executors

/**
 * Google Cast integration.
 *
 * Design rules this class follows, each of which was a defect in the previous
 * version:
 *
 *  - [CastContext] is obtained asynchronously. Reading `Task.result` before the
 *    task completes throws, and swallowing that exception left casting silently
 *    dead.
 *  - Connection state is derived **only** from [SessionManagerListener]. Marking
 *    a device connected the moment a route is selected reports success for
 *    pairings that later fail.
 *  - Media loads report their outcome. A failed `load()` surfaces as
 *    [CastPlaybackState.FAILED] with the receiver's status text.
 *  - No dependency on UI or mock data. The channel to cast is always supplied
 *    by the caller.
 */
class AndroidCastAndRemoteManager(
    context: Context
) : CastAndRemoteController {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val wifiManager =
        appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    private var multicastLock: WifiManager.MulticastLock? = null
    private var castContext: CastContext? = null
    private var mediaRouter: MediaRouter? = null
    private var mediaRouterCallback: MediaRouter.Callback? = null

    private val _discoveryState = MutableStateFlow<DiscoveryState>(DiscoveryState.Idle)
    override val discoveryState: StateFlow<DiscoveryState> = _discoveryState.asStateFlow()

    private val _connectedDevice = MutableStateFlow<TvDevice?>(null)
    override val connectedDevice: StateFlow<TvDevice?> = _connectedDevice.asStateFlow()

    private val _castingChannel = MutableStateFlow<Channel?>(null)
    val castingChannel: StateFlow<Channel?> = _castingChannel.asStateFlow()

    private val _playbackState = MutableStateFlow(CastPlaybackState.IDLE)
    val playbackState: StateFlow<CastPlaybackState> = _playbackState.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _castVolume = MutableStateFlow(0.0)
    val castVolume: StateFlow<Double> = _castVolume.asStateFlow()

    /** Discovered routes, keyed by MediaRouter route id. */
    private val routesById = linkedMapOf<String, MediaRouter.RouteInfo>()

    /** Set while a connect() is in flight so the session listener can complete it. */
    private var pendingConnection: CompletableDeferred<Unit>? = null

    /** Queued until a session exists, then loaded automatically. */
    private var pendingChannel: Channel? = null

    private var isInitialised = false

    private val remoteMediaCallback = object : RemoteMediaClient.Callback() {
        override fun onStatusUpdated() {
            val client = currentSession()?.remoteMediaClient ?: return
            _playbackState.value = when (client.playerState) {
                MediaStatus.PLAYER_STATE_PLAYING -> CastPlaybackState.PLAYING
                MediaStatus.PLAYER_STATE_PAUSED -> CastPlaybackState.PAUSED
                MediaStatus.PLAYER_STATE_BUFFERING,
                MediaStatus.PLAYER_STATE_LOADING -> CastPlaybackState.LOADING
                MediaStatus.PLAYER_STATE_IDLE -> {
                    if (client.mediaStatus?.idleReason == MediaStatus.IDLE_REASON_ERROR) {
                        _lastError.value = "The TV could not play this stream."
                        CastPlaybackState.FAILED
                    } else {
                        CastPlaybackState.IDLE
                    }
                }
                else -> _playbackState.value
            }
        }
    }

    private val sessionListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarting(session: CastSession) {
            _playbackState.value = CastPlaybackState.LOADING
        }

        override fun onSessionStarted(session: CastSession, sessionId: String) {
            handleSessionAvailable(session)
        }

        override fun onSessionResuming(session: CastSession, sessionId: String) = Unit

        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            handleSessionAvailable(session)
        }

        override fun onSessionStartFailed(session: CastSession, error: Int) {
            failConnection("Could not connect to the TV (Cast error $error).")
        }

        override fun onSessionResumeFailed(session: CastSession, error: Int) {
            failConnection("Could not resume the Cast session (error $error).")
        }

        override fun onSessionEnding(session: CastSession) = Unit

        override fun onSessionEnded(session: CastSession, error: Int) {
            try {
                session.remoteMediaClient?.unregisterCallback(remoteMediaCallback)
            } catch (_: Exception) {
                // Session already torn down.
            }
            _connectedDevice.value = null
            _castingChannel.value = null
            _playbackState.value = CastPlaybackState.IDLE
            pendingConnection?.complete(Unit)
            pendingConnection = null
            recomputeDiscoveryState()
        }

        override fun onSessionSuspended(session: CastSession, reason: Int) {
            _playbackState.value = CastPlaybackState.IDLE
        }
    }

    /**
     * Must be called from the main thread, typically once from the Activity.
     * Obtaining CastContext initialises Play Services and triggers the
     * Cast SDK's own discovery.
     */
    fun initialise() {
        if (isInitialised) return
        isInitialised = true
        mediaRouter = MediaRouter.getInstance(appContext)

        CastContext.getSharedInstance(appContext, Executors.newSingleThreadExecutor())
            .addOnSuccessListener { context ->
                castContext = context
                mainHandler.post {
                    context.sessionManager.addSessionManagerListener(
                        sessionListener,
                        CastSession::class.java
                    )
                    // Adopt a session that survived a configuration change.
                    context.sessionManager.currentCastSession
                        ?.takeIf { it.isConnected }
                        ?.let(::handleSessionAvailable)
                }
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "CastContext unavailable", error)
                _lastError.value =
                    "Google Cast is unavailable on this device (${error.message})."
                _discoveryState.value = DiscoveryState.Failed(
                    "Google Cast support is missing or Google Play services needs an update."
                )
            }
    }

    fun release() {
        stopDiscovery()
        try {
            castContext?.sessionManager
                ?.removeSessionManagerListener(sessionListener, CastSession::class.java)
        } catch (_: Exception) {
            // Nothing to clean up.
        }
    }

    private fun handleSessionAvailable(session: CastSession) {
        val castDevice = session.castDevice
        val device = TvDevice(
            id = castDevice?.deviceId ?: "cast-receiver",
            name = castDevice?.friendlyName ?: "Cast receiver",
            deviceType = castDevice?.modelName ?: "Google Cast",
            protocol = CastProtocol.GOOGLE_CAST,
            ipAddress = castDevice?.inetAddress?.hostAddress ?: "Local network",
            capabilities = TvCapabilities(
                power = false,
                voiceSearch = false,
                volumeControl = true,
                numericKeypad = false,
                textInput = false
            )
        )

        _connectedDevice.value = device
        _discoveryState.value = DiscoveryState.Connected(device)
        _castVolume.value = runCatching { session.volume }.getOrDefault(0.0)
        _lastError.value = null

        try {
            session.remoteMediaClient?.registerCallback(remoteMediaCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Could not register media callback", e)
        }

        pendingConnection?.complete(Unit)
        pendingConnection = null

        // Load whichever channel the user asked for before the session existed.
        pendingChannel?.let { channel ->
            pendingChannel = null
            loadOnReceiver(session, channel)
        }
    }

    private fun failConnection(reason: String) {
        _connectedDevice.value = null
        _playbackState.value = CastPlaybackState.FAILED
        _lastError.value = reason
        _discoveryState.value = DiscoveryState.Failed(reason)
        pendingConnection?.complete(Unit)
        pendingConnection = null
    }

    fun isWifiConnected(): Boolean {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        // Ethernet-over-USB and Wi-Fi both allow local discovery; cellular does not.
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    override fun startDiscovery() {
        if (!isWifiConnected()) {
            _discoveryState.value = DiscoveryState.Failed(
                "Connect this phone to the same Wi-Fi network as your TV."
            )
            return
        }

        if (_connectedDevice.value == null) {
            _discoveryState.value = DiscoveryState.Searching
        }

        acquireMulticastLock()

        mainHandler.post {
            val router = mediaRouter ?: MediaRouter.getInstance(appContext).also {
                mediaRouter = it
            }

            val selector = MediaRouteSelector.Builder()
                .addControlCategory(
                    CastMediaControlIntent.categoryForCast(
                        StreamCastOptionsProvider.RECEIVER_APPLICATION_ID
                    )
                )
                .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
                .build()

            // Replace any previous callback so repeated scans do not stack up.
            mediaRouterCallback?.let { runCatching { router.removeCallback(it) } }

            val callback = object : MediaRouter.Callback() {
                override fun onRouteAdded(router: MediaRouter, route: MediaRouter.RouteInfo) =
                    trackRoute(route)

                override fun onRouteChanged(router: MediaRouter, route: MediaRouter.RouteInfo) =
                    trackRoute(route)

                override fun onRouteRemoved(router: MediaRouter, route: MediaRouter.RouteInfo) {
                    routesById.remove(route.id)
                    recomputeDiscoveryState()
                }
            }
            mediaRouterCallback = callback

            router.addCallback(
                selector,
                callback,
                MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN
            )

            routesById.clear()
            router.routes.forEach(::trackRoute)
            recomputeDiscoveryState()
        }
    }

    override fun stopDiscovery() {
        mainHandler.post {
            mediaRouterCallback?.let { callback ->
                runCatching { mediaRouter?.removeCallback(callback) }
            }
            mediaRouterCallback = null
        }
        releaseMulticastLock()
    }

    private fun trackRoute(route: MediaRouter.RouteInfo) {
        if (!isCastReceiver(route)) {
            routesById.remove(route.id)
            return
        }
        routesById[route.id] = route
        recomputeDiscoveryState()
    }

    /**
     * Filters out this phone's own speakers, Bluetooth audio sinks and any
     * route that cannot actually receive a stream.
     */
    @Suppress("DEPRECATION")
    private fun isCastReceiver(route: MediaRouter.RouteInfo): Boolean {
        if (route.isDefault) return false
        if (route.isBluetooth) return false
        if (!route.isEnabled) return false
        if (route.playbackType != MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE) return false
        return true
    }

    private fun routeToDevice(route: MediaRouter.RouteInfo): TvDevice {
        val description = route.description?.takeIf { it.isNotBlank() }
        return TvDevice(
            id = route.id,
            name = route.name.ifBlank { "Cast receiver" },
            deviceType = description ?: "Google Cast",
            protocol = CastProtocol.GOOGLE_CAST,
            ipAddress = description ?: "Local network",
            capabilities = TvCapabilities(
                power = false,
                voiceSearch = false,
                volumeControl = true,
                numericKeypad = false,
                textInput = false
            )
        )
    }

    private fun recomputeDiscoveryState() {
        val connected = _connectedDevice.value
        if (connected != null) {
            _discoveryState.value = DiscoveryState.Connected(connected)
            return
        }
        // Do not overwrite a Connecting/Failed state with a route list update.
        when (_discoveryState.value) {
            is DiscoveryState.Connecting, is DiscoveryState.Failed -> return
            else -> Unit
        }
        val devices = routesById.values.map(::routeToDevice)
        _discoveryState.value = if (devices.isEmpty()) {
            DiscoveryState.Empty
        } else {
            DiscoveryState.Found(devices)
        }
    }

    /**
     * Selects the route and waits for the Cast framework to confirm the
     * session. Returns failure if the receiver never reports a session, so the
     * UI never shows a connection that does not exist.
     */
    override suspend fun connect(device: TvDevice): Result<Unit> {
        val route = routesById[device.id]
            ?: return Result.failure(
                IllegalStateException("${device.name} is no longer on the network.")
            )

        _discoveryState.value = DiscoveryState.Connecting(device)
        _lastError.value = null

        val deferred = CompletableDeferred<Unit>()
        pendingConnection = deferred

        mainHandler.post {
            runCatching { mediaRouter?.selectRoute(route) }
                .onFailure { failConnection("Could not select ${device.name}: ${it.message}") }
        }

        val completed = withTimeoutOrNull(CONNECT_TIMEOUT_MS) { deferred.await() }
        pendingConnection = null

        return when {
            completed == null -> {
                val message =
                    "${device.name} did not respond within ${CONNECT_TIMEOUT_MS / 1000} seconds."
                failConnection(message)
                Result.failure(IllegalStateException(message))
            }
            _connectedDevice.value != null -> Result.success(Unit)
            else -> Result.failure(
                IllegalStateException(_lastError.value ?: "Connection to ${device.name} failed.")
            )
        }
    }

    override suspend fun disconnect() {
        pendingChannel = null
        mainHandler.post {
            runCatching {
                castContext?.sessionManager?.endCurrentSession(true)
                mediaRouter?.let { it.selectRoute(it.defaultRoute) }
            }
        }
    }

    /**
     * Sends a channel to the receiver. If no session exists yet the channel is
     * queued and loaded as soon as one is established.
     */
    override suspend fun castChannel(channel: Channel): Result<Unit> {
        _castingChannel.value = channel
        _lastError.value = null

        val session = currentSession()
        if (session == null || !session.isConnected) {
            pendingChannel = channel
            return Result.success(Unit)
        }

        mainHandler.post { loadOnReceiver(session, channel) }
        return Result.success(Unit)
    }

    private fun loadOnReceiver(session: CastSession, channel: Channel) {
        val client = session.remoteMediaClient
        if (client == null) {
            _playbackState.value = CastPlaybackState.FAILED
            _lastError.value = "The TV is connected but not ready to receive media."
            return
        }

        if (!channel.streamUrl.startsWith("http", ignoreCase = true)) {
            _playbackState.value = CastPlaybackState.FAILED
            _lastError.value = "${channel.name} has no playable stream URL."
            return
        }

        _playbackState.value = CastPlaybackState.LOADING
        _castingChannel.value = channel

        val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_GENERIC).apply {
            putString(MediaMetadata.KEY_TITLE, channel.name)
            putString(
                MediaMetadata.KEY_SUBTITLE,
                listOfNotNull(
                    channel.category.displayName,
                    channel.language,
                    channel.quality
                ).joinToString(" • ")
            )
            channel.logoUrl?.let { addImage(com.google.android.gms.common.images.WebImage(
                android.net.Uri.parse(it)
            )) }
        }

        val mediaInfo = MediaInfo.Builder(channel.streamUrl)
            .setStreamType(MediaInfo.STREAM_TYPE_LIVE)
            .setContentType(contentTypeFor(channel.streamUrl))
            .setMetadata(metadata)
            .build()

        val request = MediaLoadRequestData.Builder()
            .setMediaInfo(mediaInfo)
            .setAutoplay(true)
            .build()

        client.load(request).setResultCallback { result ->
            if (result.status.isSuccess) {
                _playbackState.value = CastPlaybackState.PLAYING
            } else {
                _playbackState.value = CastPlaybackState.FAILED
                val detail = result.status.statusMessage?.takeIf { it.isNotBlank() }
                    ?: "status ${result.status.statusCode}"
                _lastError.value =
                    "The TV refused ${channel.name} ($detail). The stream may be offline or geo-blocked."
                Log.w(TAG, "Cast load failed for ${channel.name}: $detail")
            }
        }
    }

    /**
     * The receiver picks its pipeline from the content type, so an HLS manifest
     * must not be announced as an MP4 (or vice versa).
     */
    private fun contentTypeFor(url: String): String {
        val path = url.substringBefore('?').lowercase()
        return when {
            path.endsWith(".m3u8") || path.endsWith(".m3u") -> "application/x-mpegurl"
            path.endsWith(".mpd") -> "application/dash+xml"
            path.endsWith(".mp4") -> "video/mp4"
            path.endsWith(".webm") -> "video/webm"
            path.endsWith(".ts") -> "video/mp2t"
            // Unknown extensions are overwhelmingly HLS in IPTV playlists.
            else -> "application/x-mpegurl"
        }
    }

    private fun currentSession(): CastSession? =
        runCatching { castContext?.sessionManager?.currentCastSession }.getOrNull()

    /**
     * Cast has no D-pad channel, so only transport and volume keys can be
     * honoured. [sendKey] reports failure for anything else rather than
     * pretending it worked.
     */
    override suspend fun sendKey(keyCode: RemoteKeyCode): Result<Unit> {
        val session = currentSession()
        if (session == null || !session.isConnected) {
            return Result.failure(IllegalStateException("No TV connected."))
        }
        val client = session.remoteMediaClient
            ?: return Result.failure(IllegalStateException("The TV is not ready for commands."))

        return when (keyCode) {
            RemoteKeyCode.MEDIA_PLAY_PAUSE -> {
                mainHandler.post {
                    runCatching { if (client.isPlaying) client.pause() else client.play() }
                }
                Result.success(Unit)
            }
            RemoteKeyCode.MEDIA_STOP -> {
                mainHandler.post { runCatching { client.stop() } }
                Result.success(Unit)
            }
            RemoteKeyCode.VOLUME_UP -> adjustVolume(session, VOLUME_STEP)
            RemoteKeyCode.VOLUME_DOWN -> adjustVolume(session, -VOLUME_STEP)
            RemoteKeyCode.VOLUME_MUTE -> {
                mainHandler.post {
                    runCatching { session.isMute = !session.isMute }
                }
                Result.success(Unit)
            }
            RemoteKeyCode.SEEK_TO_LIVE -> {
                mainHandler.post {
                    runCatching {
                        client.mediaStatus?.liveSeekableRange?.endTime?.let { client.seek(it) }
                    }
                }
                Result.success(Unit)
            }
            else -> Result.failure(
                UnsupportedOperationException(
                    "Google Cast cannot send ${keyCode.name}. D-pad and power keys need the " +
                        "Android TV Remote v2 protocol, which is not implemented."
                )
            )
        }
    }

    private fun adjustVolume(session: CastSession, delta: Double): Result<Unit> {
        mainHandler.post {
            runCatching {
                val target = (session.volume + delta).coerceIn(0.0, 1.0)
                session.volume = target
                session.isMute = false
                _castVolume.value = target
            }
        }
        return Result.success(Unit)
    }

    fun setVolume(level: Double) {
        val session = currentSession() ?: return
        mainHandler.post {
            runCatching {
                val target = level.coerceIn(0.0, 1.0)
                session.volume = target
                _castVolume.value = target
            }
        }
    }

    override suspend fun sendText(text: String): Result<Unit> = Result.failure(
        UnsupportedOperationException(
            "Text entry requires the Android TV Remote v2 protocol, which is not implemented."
        )
    )

    fun clearError() {
        _lastError.value = null
    }

    private fun acquireMulticastLock() {
        runCatching {
            if (multicastLock == null) {
                multicastLock = wifiManager?.createMulticastLock("StreamCastDiscovery")?.apply {
                    setReferenceCounted(false)
                }
            }
            multicastLock?.takeIf { !it.isHeld }?.acquire()
        }
    }

    private fun releaseMulticastLock() {
        runCatching {
            multicastLock?.takeIf { it.isHeld }?.release()
        }
    }

    private companion object {
        const val TAG = "StreamCastCast"
        const val CONNECT_TIMEOUT_MS = 20_000L
        const val VOLUME_STEP = 0.05
    }
}
