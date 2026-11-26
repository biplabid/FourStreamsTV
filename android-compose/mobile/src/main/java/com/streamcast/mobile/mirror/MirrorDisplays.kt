package com.streamcast.mobile.mirror

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A screen this phone is currently driving that is not the phone's own panel:
 * a Miracast / Wi-Fi Display television, a Samsung Smart View session, or an
 * HDMI adapter.
 */
data class ExternalDisplay(
    val displayId: Int,
    val name: String,
    val widthPx: Int,
    val heightPx: Int
) {
    val resolutionLabel: String get() = "$widthPx x $heightPx"
}

/**
 * Watches for a second screen appearing.
 *
 * Android gives third-party apps no way to *start* a Miracast session - the
 * APIs that connect a Wi-Fi Display (`DisplayManager.connectWifiDisplay`) are
 * hidden and guarded by a signature permission, which is why every "screen
 * mirroring" app on the Play Store is really just a shortcut into the system
 * Cast panel. See [SystemCastPanel].
 *
 * What an app *can* do is notice once the user has connected one, and that is
 * worth a great deal: a mirrored display shows up here as a presentation
 * display, and the app can then render video onto it directly at the
 * television's own resolution instead of letting the system scale a copy of
 * the phone's screen. That is the difference between a sharp 1080p picture
 * with no status bar and a letterboxed photograph of a phone.
 */
class MirrorDisplays(context: Context) {

    private val displayManager = context.applicationContext
        .getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

    private val handler = Handler(Looper.getMainLooper())

    private val _current = MutableStateFlow<ExternalDisplay?>(null)

    /** The external screen in use, or null when the phone is on its own. */
    val current: StateFlow<ExternalDisplay?> = _current.asStateFlow()

    private var registered = false

    private val listener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = refresh()
        override fun onDisplayRemoved(displayId: Int) = refresh()
        override fun onDisplayChanged(displayId: Int) = refresh()
    }

    fun start() {
        if (!registered) {
            displayManager.registerDisplayListener(listener, handler)
            registered = true
        }
        refresh()
    }

    fun stop() {
        if (registered) {
            runCatching { displayManager.unregisterDisplayListener(listener) }
            registered = false
        }
    }

    /**
     * The live [Display] for [current], re-read from the system each time.
     *
     * Deliberately not cached: a Display handle outlives the screen it
     * describes, and showing a Presentation on a stale one throws.
     */
    fun liveDisplay(): Display? =
        _current.value
            ?.let { runCatching { displayManager.getDisplay(it.displayId) }.getOrNull() }
            ?.takeIf { it.isValid }

    fun refresh() {
        val picked = runCatching {
            displayManager
                .getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
                .firstOrNull(::isUsable)
        }.getOrNull()

        val next = picked?.let { display ->
            val mode = display.mode
            ExternalDisplay(
                displayId = display.displayId,
                name = display.name?.takeIf { it.isNotBlank() } ?: "External display",
                widthPx = mode.physicalWidth,
                heightPx = mode.physicalHeight
            )
        }

        if (next != _current.value) {
            Log.d(TAG, "external display -> ${next?.name ?: "none"} ${next?.resolutionLabel ?: ""}")
            _current.value = next
        }
    }

    private fun isUsable(display: Display): Boolean {
        if (display.displayId == Display.DEFAULT_DISPLAY) return false
        if (!display.isValid) return false
        if (display.state == Display.STATE_OFF) return false
        // A private display belongs to whoever created it - our own
        // VirtualDisplays, screen recorders, overlay tools. Not a television.
        if (display.flags and Display.FLAG_PRIVATE != 0) return false
        return true
    }

    private companion object {
        const val TAG = "MirrorDisplays"
    }
}
