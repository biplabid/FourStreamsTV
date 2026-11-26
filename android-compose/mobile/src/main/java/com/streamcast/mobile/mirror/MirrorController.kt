package com.streamcast.mobile.mirror

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.media3.common.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Sends video to a television that does not speak Google Cast.
 *
 * Google Cast needs a Cast receiver, which means a Chromecast, a Google TV or
 * an Android TV. A Panasonic Viera, an older LG or Samsung, a Philips - these
 * have no Cast receiver at all, and no amount of work on the Cast path will
 * ever reach them. What they almost all do have is Miracast, branded as
 * "Screen Mirroring", "Mirroring", "Smart View" or "Screen Share" depending
 * on the maker.
 *
 * Two things have to happen, and only the second is ours:
 *
 *  1. The user starts a mirroring session from the system panel
 *     ([SystemCastPanel]). No app can do this itself - the API is hidden
 *     behind a signature permission.
 *  2. Once a session is live the phone owns a second display, and we put the
 *     video on it as its own full-screen window ([ChannelPresentation])
 *     rather than leaving the system to duplicate the phone's screen.
 *
 * Step 2 is what makes this worth building. Plain mirroring sends the TV a
 * scaled copy of a portrait phone - video in a small box, status bar and
 * notifications included. Drawing onto the display directly gives the TV a
 * full-screen picture at its own resolution and frees the phone to stay a
 * remote.
 */
class MirrorController(context: Context) {

    private val displays = MirrorDisplays(context)

    /** The TV we are driving, or null. Drives the whole UI. */
    val externalDisplay: StateFlow<ExternalDisplay?> = displays.current

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private var presentation: ChannelPresentation? = null

    init {
        displays.start()
    }

    /** Re-reads the display list, for a manual "check again" tap. */
    fun refresh() = displays.refresh()

    fun openSystemPanel(context: Context): Boolean {
        val opened = SystemCastPanel.open(context)
        if (!opened) {
            _lastError.value = "This phone has no screen-mirroring setting. " +
                "Google removed Wi-Fi Display from Pixel phones, and some other " +
                "makers never shipped it."
        }
        return opened
    }

    /**
     * Puts [player]'s video on the television.
     *
     * Safe to call repeatedly with the same arguments and safe to call with
     * nulls, which makes it usable straight from a Compose effect keyed on the
     * display and the player.
     *
     * @return true when the picture is now on the TV.
     */
    fun present(activity: Activity?, player: Player?): Boolean {
        if (activity == null || player == null) {
            dismiss()
            return false
        }

        val display = displays.liveDisplay()
        if (display == null) {
            dismiss()
            return false
        }

        presentation?.let { existing ->
            if (existing.display?.displayId == display.displayId && existing.isShowing) {
                return true
            }
            dismiss()
        }

        return runCatching {
            ChannelPresentation(activity, display, player).also { created ->
                created.setOnDismissListener {
                    // Covers the system taking the window away under us when
                    // the mirroring session drops.
                    if (presentation === created) {
                        created.releasePlayer()
                        presentation = null
                    }
                }
                created.show()
                presentation = created
            }
            _lastError.value = null
            Log.d(TAG, "presenting on display ${display.displayId} (${display.name})")
            true
        }.getOrElse { error ->
            Log.w(TAG, "could not present on ${display.name}", error)
            _lastError.value = "Could not take over the TV screen. " +
                "Your phone is still mirroring, so the picture will show as a " +
                "copy of this screen instead."
            presentation = null
            false
        }
    }

    /** Takes the video back off the TV and returns the player to the caller. */
    fun dismiss() {
        val current = presentation ?: return
        presentation = null
        current.releasePlayer()
        runCatching { current.dismiss() }
    }

    private companion object {
        const val TAG = "MirrorController"
    }
}
