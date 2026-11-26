package com.streamcast.mobile.mirror

import android.app.Presentation
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Display
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

/**
 * The window shown on the television.
 *
 * A [Presentation] is a dialog that lives on a secondary display, so this is
 * genuinely a second window with its own surface - not a copy of the phone's.
 * The phone decodes the HLS stream once and the decoded frames go to the TV
 * at the TV's resolution, with no phone chrome in the picture and nothing on
 * screen when a notification arrives.
 *
 * It holds no player of its own. The caller's [Player] is borrowed and handed
 * back untouched, so the stream never restarts when the picture moves between
 * the phone and the TV.
 */
@UnstableApi
class ChannelPresentation(
    outerContext: Context,
    display: Display,
    private val player: Player
) : Presentation(outerContext, display, android.R.style.Theme_Black_NoTitleBar_Fullscreen) {

    private var playerView: PlayerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val video = PlayerView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            // There is no touchscreen on the far end of a Miracast link, so a
            // control overlay there would only ever be in the way.
            useController = false
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            setShutterBackgroundColor(Color.BLACK)
            setBackgroundColor(Color.BLACK)
            // Keep the last frame rather than flashing black while a channel
            // change re-buffers.
            setKeepContentOnPlayerReset(true)
        }

        setContentView(
            FrameLayout(context).apply {
                setBackgroundColor(Color.BLACK)
                addView(video)
            }
        )

        video.player = player
        playerView = video
    }

    /**
     * Gives the player back before this window goes away.
     *
     * Letting the dialog tear down while the player still points at its
     * surface leaves the player rendering into a dead surface, which on some
     * devices is a hard decoder error rather than a blank frame.
     */
    fun releasePlayer() {
        playerView?.player = null
        playerView = null
    }
}
