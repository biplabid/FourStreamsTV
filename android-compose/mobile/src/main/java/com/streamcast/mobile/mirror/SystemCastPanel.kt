package com.streamcast.mobile.mirror

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Opens the system's own screen-mirroring picker.
 *
 * This is the only supported way for an app to get a Miracast session started.
 * `DisplayManager.connectWifiDisplay` exists but is hidden API behind
 * `CONFIGURE_WIFI_DISPLAY`, a signature permission - no third-party app can
 * hold it. So we hand the user to the panel the OEM already ships, they pick
 * the television there, and [MirrorDisplays] notices the result.
 *
 * Needs no permission of any kind.
 */
object SystemCastPanel {

    /**
     * Tried in order. The first entry is the documented one and resolves on
     * stock Android and on Samsung; the Samsung-specific dialog is a nicer
     * landing spot where it exists, but it is an OEM intent and may vanish
     * without notice, so it is never the only option.
     */
    private val CANDIDATES = listOf(
        "android.settings.CAST_SETTINGS",
        "com.samsung.wfd.LAUNCH_WFD_PICKER_DLG",
        "android.settings.WIFI_DISPLAY_SETTINGS",
        "android.settings.DISPLAY_SETTINGS"
    )

    /**
     * @return true if some panel opened. False means this phone has no
     *   mirroring UI at all, which is a real outcome: Google's own Pixels
     *   dropped Wi-Fi Display support and offer nothing here.
     */
    fun open(context: Context): Boolean {
        for (action in CANDIDATES) {
            val intent = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
                Log.d(TAG, "opened $action")
                return true
            } catch (e: ActivityNotFoundException) {
                Log.d(TAG, "$action not present")
            } catch (e: SecurityException) {
                // Some OEM panels are not exported to third-party callers.
                Log.d(TAG, "$action refused: ${e.message}")
            }
        }
        Log.w(TAG, "no screen-mirroring panel on this device")
        return false
    }

    private const val TAG = "SystemCastPanel"
}
