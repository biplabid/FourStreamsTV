package com.streamcast.mobile.atv

import android.content.Context
import android.util.Base64
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/**
 * An Android TV this app knows about.
 *
 * [host] is the identity rather than the mDNS name: the service name changes
 * when a user renames their TV, but pairing is bound to an address and a
 * certificate.
 */
data class AtvDevice(
    val host: String,
    val name: String,
    val port: Int = AtvRemoteSession.REMOTE_PORT,
    /** Present once paired. Null means discovered but not yet usable. */
    val serverCertificate: ByteArray? = null,
    val lastConnectedAt: Long = 0L,
    /** True when seen on the network during this discovery run. */
    val online: Boolean = false
) {
    val isPaired: Boolean get() = serverCertificate != null

    // host is the identity; the generated equals would compare the certificate
    // array by reference and make two loads of the same device unequal.
    override fun equals(other: Any?): Boolean = other is AtvDevice && other.host == host

    override fun hashCode(): Int = host.hashCode()
}

/**
 * Remembers paired TVs and the certificate each one presented.
 *
 * The certificate is the whole point of storing anything: it is what lets a
 * later connection verify it is talking to the same TV the user stood in front
 * of, rather than whatever has that address today.
 */
class AtvDeviceStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("atv_devices", Context.MODE_PRIVATE)

    fun paired(): List<AtvDevice> {
        val raw = prefs.getString(KEY_DEVICES, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i -> fromJson(array.getJSONObject(i)) }
        }.getOrElse {
            Log.w(TAG, "Paired device list unreadable; starting fresh", it)
            emptyList()
        }
    }

    fun find(host: String): AtvDevice? = paired().firstOrNull { it.host == host }

    fun save(device: AtvDevice) {
        val updated = paired().filterNot { it.host == device.host } + device
        persist(updated)
    }

    fun forget(host: String) {
        persist(paired().filterNot { it.host == host })
    }

    fun forgetAll() = persist(emptyList())

    /** The TV to offer on opening the remote: the one used most recently. */
    fun lastUsed(): AtvDevice? = paired().maxByOrNull { it.lastConnectedAt }

    private fun persist(devices: List<AtvDevice>) {
        val array = JSONArray()
        devices.forEach { array.put(toJson(it)) }
        prefs.edit().putString(KEY_DEVICES, array.toString()).apply()
    }

    private fun toJson(device: AtvDevice) = JSONObject().apply {
        put("host", device.host)
        put("name", device.name)
        put("port", device.port)
        put("lastConnectedAt", device.lastConnectedAt)
        device.serverCertificate?.let {
            put("serverCertificate", Base64.encodeToString(it, Base64.NO_WRAP))
        }
    }

    private fun fromJson(json: JSONObject): AtvDevice? {
        val host = json.optString("host").takeIf { it.isNotBlank() } ?: return null
        return AtvDevice(
            host = host,
            name = json.optString("name", host),
            port = json.optInt("port", AtvRemoteSession.REMOTE_PORT),
            serverCertificate = json.optString("serverCertificate")
                .takeIf { it.isNotBlank() }
                ?.let { Base64.decode(it, Base64.NO_WRAP) },
            lastConnectedAt = json.optLong("lastConnectedAt", 0L)
        )
    }

    private companion object {
        const val TAG = "AtvDeviceStore"
        const val KEY_DEVICES = "paired_devices_v1"
    }
}
