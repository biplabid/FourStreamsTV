package com.streamcast.mobile.cast

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.nsd.NsdManager
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import androidx.mediarouter.media.MediaControlIntent
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.streamcast.mobile.ui.screens.StreamCastMockData
import com.streamcast.shared.cast.CastAndRemoteController
import com.streamcast.shared.cast.RemoteKeyCode
import com.streamcast.shared.model.CastProtocol
import com.streamcast.shared.model.Channel
import com.streamcast.shared.model.DiscoveryState
import com.streamcast.shared.model.TvCapabilities
import com.streamcast.shared.model.TvDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors

data class PhysicalCastDevice(
    val device: TvDevice,
    val mediaRoute: MediaRouter.RouteInfo? = null
)

class AndroidCastAndRemoteManager(
    private val context: Context
) : CastAndRemoteController {

    private val mainHandler = Handler(Looper.getMainLooper())

    private val mediaRouter: MediaRouter by lazy { MediaRouter.getInstance(context) }
    private val wifiManager: WifiManager? = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private var multicastLock: WifiManager.MulticastLock? = null

    private var castContext: CastContext? = null

    private val _discoveryState = MutableStateFlow<DiscoveryState>(DiscoveryState.Idle)
    override val discoveryState: StateFlow<DiscoveryState> = _discoveryState.asStateFlow()

    private val _connectedDevice = MutableStateFlow<TvDevice?>(null)
    override val connectedDevice: StateFlow<TvDevice?> = _connectedDevice.asStateFlow()

    private val _currentCastingChannel = MutableStateFlow<Channel?>(null)
    val currentCastingChannel: StateFlow<Channel?> = _currentCastingChannel.asStateFlow()

    private val physicalDevicesMap = mutableMapOf<String, PhysicalCastDevice>()
    private var mediaRouterCallback: MediaRouter.Callback? = null
    private var castSessionListener: SessionManagerListener<CastSession>? = null

    init {
        mainHandler.post {
            try {
                castContext = CastContext.getSharedInstance(context, Executors.newSingleThreadExecutor()).result
                setupCastSessionListener()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupCastSessionListener() {
        castSessionListener = object : SessionManagerListener<CastSession> {
            override fun onSessionStarted(session: CastSession, sessionId: String) {
                val deviceName = session.castDevice?.friendlyName ?: "Cast Receiver"
                val tvDevice = TvDevice(
                    id = session.castDevice?.deviceId ?: "cast_device",
                    name = deviceName,
                    deviceType = "Google Cast TV",
                    protocol = CastProtocol.GOOGLE_CAST,
                    ipAddress = session.castDevice?.inetAddress?.hostAddress ?: "Local Wi-Fi",
                    capabilities = TvCapabilities(true, true, true, true, true)
                )
                _connectedDevice.value = tvDevice
                _discoveryState.value = DiscoveryState.Connected(tvDevice)

                // Trigger real stream playback on the TV immediately upon connection
                val channelToCast = _currentCastingChannel.value ?: StreamCastMockData.Channels.first().channel
                sendRealStreamToCastReceiver(session, channelToCast)
            }

            override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
                val deviceName = session.castDevice?.friendlyName ?: "Cast Receiver"
                val tvDevice = TvDevice(
                    id = session.castDevice?.deviceId ?: "cast_device",
                    name = deviceName,
                    deviceType = "Google Cast TV",
                    protocol = CastProtocol.GOOGLE_CAST,
                    ipAddress = session.castDevice?.inetAddress?.hostAddress ?: "Local Wi-Fi",
                    capabilities = TvCapabilities(true, true, true, true, true)
                )
                _connectedDevice.value = tvDevice
                _discoveryState.value = DiscoveryState.Connected(tvDevice)

                val channelToCast = _currentCastingChannel.value ?: StreamCastMockData.Channels.first().channel
                sendRealStreamToCastReceiver(session, channelToCast)
            }

            override fun onSessionEnding(session: CastSession) {}

            override fun onSessionEnded(session: CastSession, error: Int) {
                _connectedDevice.value = null
                _currentCastingChannel.value = null
                updateDiscoveryState()
            }

            override fun onSessionStartFailed(session: CastSession, error: Int) {
                _connectedDevice.value = null
                _discoveryState.value = DiscoveryState.Failed("Connection failed (Error code $error)")
            }

            override fun onSessionResumeFailed(session: CastSession, error: Int) {}
            override fun onSessionStarting(session: CastSession) {}
            override fun onSessionResuming(session: CastSession, sessionId: String) {}
            override fun onSessionSuspended(session: CastSession, reason: Int) {}
        }

        try {
            castSessionListener?.let { listener ->
                castContext?.sessionManager?.addSessionManagerListener(listener, CastSession::class.java)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isWifiConnected(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    override fun startDiscovery() {
        if (!isWifiConnected()) {
            _discoveryState.value = DiscoveryState.Failed("Wi-Fi is not connected. Please connect to your home Wi-Fi.")
            return
        }

        _discoveryState.value = DiscoveryState.Searching

        try {
            if (multicastLock == null) {
                multicastLock = wifiManager?.createMulticastLock("StreamCastMulticastLock")
                multicastLock?.setReferenceCounted(true)
            }
            multicastLock?.acquire()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        physicalDevicesMap.clear()

        // Scan real MediaRouter routes on local network
        mainHandler.post {
            try {
                val selector = MediaRouteSelector.Builder()
                    .addControlCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO)
                    .addControlCategory(MediaControlIntent.CATEGORY_LIVE_VIDEO)
                    .addControlCategory(CastMediaControlIntent.categoryForCast(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID))
                    .build()

                mediaRouterCallback = object : MediaRouter.Callback() {
                    override fun onRouteAdded(router: MediaRouter, route: MediaRouter.RouteInfo) {
                        addRouteDevice(route)
                    }

                    override fun onRouteChanged(router: MediaRouter, route: MediaRouter.RouteInfo) {
                        addRouteDevice(route)
                    }

                    override fun onRouteRemoved(router: MediaRouter, route: MediaRouter.RouteInfo) {
                        val id = route.id.lowercase().replace(" ", "_")
                        physicalDevicesMap.remove(id)
                        updateDiscoveryState()
                    }
                }

                mediaRouter.addCallback(
                    selector,
                    mediaRouterCallback!!,
                    MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN
                )

                for (route in mediaRouter.routes) {
                    if (!route.isDefault && !route.isBluetooth) {
                        addRouteDevice(route)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun addRouteDevice(route: MediaRouter.RouteInfo) {
        if (route.isDefault || route.isBluetooth) return
        val routeName = route.name.ifBlank { "Physical TV Receiver" }
        val id = route.id.ifBlank { routeName.lowercase().replace(" ", "_") }

        val tvDevice = TvDevice(
            id = id,
            name = routeName,
            deviceType = if (routeName.contains("TV", ignoreCase = true) || routeName.contains("Xstream", ignoreCase = true)) "Google TV / Smart Receiver" else "Chromecast Device",
            protocol = CastProtocol.GOOGLE_CAST,
            ipAddress = route.description ?: "Local Wi-Fi Network",
            capabilities = TvCapabilities(true, true, true, true, true)
        )

        physicalDevicesMap[id] = PhysicalCastDevice(device = tvDevice, mediaRoute = route)
        updateDiscoveryState()
    }

    private fun updateDiscoveryState() {
        val list = physicalDevicesMap.values.map { it.device }
        if (_connectedDevice.value != null) {
            _discoveryState.value = DiscoveryState.Connected(_connectedDevice.value!!)
        } else if (list.isNotEmpty()) {
            _discoveryState.value = DiscoveryState.Found(list)
        } else {
            _discoveryState.value = DiscoveryState.Empty
        }
    }

    override fun stopDiscovery() {
        mainHandler.post {
            try {
                mediaRouterCallback?.let { mediaRouter.removeCallback(it) }
                mediaRouterCallback = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun connect(device: TvDevice): Result<Unit> {
        _discoveryState.value = DiscoveryState.Connecting(device)

        return try {
            val physicalItem = physicalDevicesMap[device.id]
            val route = physicalItem?.mediaRoute

            mainHandler.post {
                if (route != null) {
                    mediaRouter.selectRoute(route)
                }
            }

            _connectedDevice.value = device
            _discoveryState.value = DiscoveryState.Connected(device)

            val channelToCast = _currentCastingChannel.value ?: StreamCastMockData.Channels.first().channel
            castChannel(channelToCast)

            Result.success(Unit)
        } catch (e: Exception) {
            _discoveryState.value = DiscoveryState.Failed("Connection failed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun disconnect() {
        mainHandler.post {
            try {
                castContext?.sessionManager?.endCurrentSession(true)
                mediaRouter.selectRoute(mediaRouter.defaultRoute)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        _connectedDevice.value = null
        _currentCastingChannel.value = null
        updateDiscoveryState()
    }

    override suspend fun castChannel(channel: Channel): Result<Unit> {
        _currentCastingChannel.value = channel

        mainHandler.post {
            try {
                val currentSession = castContext?.sessionManager?.currentCastSession
                if (currentSession != null) {
                    sendRealStreamToCastReceiver(currentSession, channel)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return Result.success(Unit)
    }

    private fun sendRealStreamToCastReceiver(session: CastSession, channel: Channel) {
        try {
            val remoteMediaClient = session.remoteMediaClient ?: return
            val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE)
            metadata.putString(MediaMetadata.KEY_TITLE, channel.name)
            metadata.putString(MediaMetadata.KEY_SUBTITLE, "Live IPTV Stream • ${channel.category.displayName}")

            // Valid live stream URL sent to TV receiver for actual video playback
            val liveUrl = if (channel.streamUrl.startsWith("http")) channel.streamUrl else "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"

            val mediaInfo = MediaInfo.Builder(liveUrl)
                .setStreamType(MediaInfo.STREAM_TYPE_LIVE)
                .setContentType("application/x-mpegurl")
                .setMetadata(metadata)
                .build()

            val loadRequest = MediaLoadRequestData.Builder()
                .setMediaInfo(mediaInfo)
                .setAutoplay(true)
                .build()

            remoteMediaClient.load(loadRequest)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun sendKey(keyCode: RemoteKeyCode): Result<Unit> {
        if (_connectedDevice.value == null) {
            return Result.failure(IllegalStateException("No TV device connected."))
        }
        mainHandler.post {
            try {
                val remoteMediaClient = castContext?.sessionManager?.currentCastSession?.remoteMediaClient
                when (keyCode) {
                    RemoteKeyCode.MEDIA_PLAY_PAUSE -> {
                        if (remoteMediaClient?.isPlaying == true) remoteMediaClient.pause() else remoteMediaClient?.play()
                    }
                    RemoteKeyCode.VOLUME_UP -> {
                        val session = castContext?.sessionManager?.currentCastSession
                        session?.let { it.volume = (it.volume + 0.05).coerceAtMost(1.0) }
                    }
                    RemoteKeyCode.VOLUME_DOWN -> {
                        val session = castContext?.sessionManager?.currentCastSession
                        session?.let { it.volume = (it.volume - 0.05).coerceAtLeast(0.0) }
                    }
                    RemoteKeyCode.VOLUME_MUTE -> {
                        val session = castContext?.sessionManager?.currentCastSession
                        session?.let { it.isMute = !it.isMute }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return Result.success(Unit)
    }

    override suspend fun sendText(text: String): Result<Unit> {
        return Result.success(Unit)
    }
}
