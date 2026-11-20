package com.streamcast.mobile.atv

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Finds Android TVs on the local network over mDNS.
 *
 * Devices running the Android TV Remote Service advertise
 * `_androidtvremote2._tcp`, which is a stronger signal than Cast discovery for
 * this purpose: a Chromecast dongle answers Cast but has no remote service,
 * while a Google TV box answers both.
 */
class AtvDiscovery(context: Context) {

    private val nsdManager =
        context.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager

    private val _devices = MutableStateFlow<List<AtvDevice>>(emptyList())
    val devices: StateFlow<List<AtvDevice>> = _devices.asStateFlow()

    private val _searching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = _searching.asStateFlow()

    private var listener: NsdManager.DiscoveryListener? = null

    /**
     * NsdManager resolves one service at a time on older releases; overlapping
     * calls fail with FAILURE_ALREADY_ACTIVE. Requests are queued instead.
     */
    private val resolveQueue = ConcurrentLinkedQueue<NsdServiceInfo>()
    private val resolving = AtomicBoolean(false)

    fun start() {
        if (listener != null) return

        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                _searching.value = true
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                if (service.serviceType.trimEnd('.').endsWith(SERVICE_TYPE.trimEnd('.'))) {
                    resolveQueue.add(service)
                    pumpResolveQueue()
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                val name = service.serviceName ?: return
                _devices.value = _devices.value.map {
                    if (it.name == name) it.copy(online = false) else it
                }
            }

            override fun onDiscoveryStopped(serviceType: String) {
                _searching.value = false
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.w(TAG, "Discovery failed to start: $errorCode")
                _searching.value = false
                listener = null
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.w(TAG, "Discovery failed to stop: $errorCode")
                _searching.value = false
                listener = null
            }
        }

        listener = discoveryListener
        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            // Thrown on some devices when Wi-Fi is off entirely.
            Log.w(TAG, "Could not start mDNS discovery", e)
            listener = null
            _searching.value = false
        }
    }

    fun stop() {
        val current = listener ?: return
        listener = null
        runCatching { nsdManager.stopServiceDiscovery(current) }
        resolveQueue.clear()
        resolving.set(false)
        _searching.value = false
    }

    /** Clears what was found, keeping nothing but what the store knows. */
    fun clearFound() {
        _devices.value = emptyList()
    }

    private fun pumpResolveQueue() {
        if (!resolving.compareAndSet(false, true)) return
        val next = resolveQueue.poll()
        if (next == null) {
            resolving.set(false)
            return
        }

        nsdManager.resolveService(next, object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.d(TAG, "Could not resolve ${serviceInfo.serviceName}: $errorCode")
                resolving.set(false)
                pumpResolveQueue()
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                @Suppress("DEPRECATION")
                val address = serviceInfo.host?.hostAddress
                if (address != null) {
                    record(
                        AtvDevice(
                            host = address,
                            name = serviceInfo.serviceName?.takeIf { it.isNotBlank() } ?: address,
                            port = AtvRemoteSession.REMOTE_PORT,
                            online = true
                        )
                    )
                }
                resolving.set(false)
                pumpResolveQueue()
            }
        })
    }

    private fun record(device: AtvDevice) {
        val existing = _devices.value
        _devices.value = if (existing.any { it.host == device.host }) {
            existing.map { if (it.host == device.host) device else it }
        } else {
            existing + device
        }
    }

    private companion object {
        const val TAG = "AtvDiscovery"

        /** Advertised by the Android TV Remote Service. */
        const val SERVICE_TYPE = "_androidtvremote2._tcp."
    }
}
