package com.streamcast.mobile.cast

import android.content.Context
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions
import com.google.android.gms.cast.framework.media.MediaIntentReceiver
import com.google.android.gms.cast.framework.media.NotificationOptions

/**
 * Cast configuration for StreamCast.
 *
 * ## Receiver application ID
 * This currently uses Google's Default Media Receiver, which plays HLS but
 * gives the sender no control over request headers, cookies, retry policy or
 * live-edge behaviour. Several Indian IPTV sources require a Referer or a
 * specific User-Agent and will fail on the default receiver.
 *
 * To fix those streams, register a Custom Web Receiver at
 * https://cast.google.com/publish, host a receiver page that configures
 * Shaka/hls.js, and replace [RECEIVER_APPLICATION_ID] with the issued ID.
 * Nothing else in the app has to change.
 */
class StreamCastOptionsProvider : OptionsProvider {

    override fun getCastOptions(context: Context): CastOptions {
        val notificationOptions = NotificationOptions.Builder()
            .setActions(
                listOf(
                    MediaIntentReceiver.ACTION_TOGGLE_PLAYBACK,
                    MediaIntentReceiver.ACTION_STOP_CASTING
                ),
                intArrayOf(0, 1)
            )
            .setTargetActivityClassName(ExpandedControlsActivity::class.java.name)
            .build()

        val mediaOptions = CastMediaOptions.Builder()
            .setNotificationOptions(notificationOptions)
            .setExpandedControllerActivityClassName(ExpandedControlsActivity::class.java.name)
            .build()

        return CastOptions.Builder()
            .setReceiverApplicationId(RECEIVER_APPLICATION_ID)
            .setCastMediaOptions(mediaOptions)
            // Rejoin an existing session when the app returns to the foreground.
            .setResumeSavedSession(true)
            .setEnableReconnectionService(true)
            .setStopReceiverApplicationWhenEndingSession(true)
            .build()
    }

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? = null

    companion object {
        /** Swap for a custom receiver ID to control headers and retry policy. */
        const val RECEIVER_APPLICATION_ID: String =
            CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID
    }
}
