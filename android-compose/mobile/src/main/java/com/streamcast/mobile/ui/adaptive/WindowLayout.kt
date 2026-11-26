package com.streamcast.mobile.ui.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material window-size breakpoints, measured on the app's *window* rather than
 * the physical screen, so split-screen and pop-up windows on a tablet get the
 * phone layout they actually have room for.
 *
 * Rough reference points:
 *  - Galaxy S24 portrait       ~384 x 780 dp   -> Compact
 *  - Galaxy S24 landscape      ~780 x 360 dp   -> Medium, short landscape
 *  - Tab S11 Ultra portrait    ~1056 x 1690 dp -> Expanded
 *  - Tab S11 Ultra landscape   ~1690 x 1000 dp -> Expanded
 */
enum class WidthClass { Compact, Medium, Expanded }

@Immutable
data class WindowLayout(val widthDp: Int, val heightDp: Int) {

    val widthClass: WidthClass = when {
        widthDp < 600 -> WidthClass.Compact
        widthDp < 840 -> WidthClass.Medium
        else -> WidthClass.Expanded
    }

    val isCompact: Boolean get() = widthClass == WidthClass.Compact

    val isLandscape: Boolean get() = widthDp > heightDp

    /** A phone on its side: wide enough, but far too short for stacked content. */
    val isShortLandscape: Boolean get() = isLandscape && heightDp < 480

    /** A side rail instead of the bottom bar once there is horizontal room for it. */
    val useNavigationRail: Boolean get() = !isCompact

    /**
     * Content placed side by side rather than stacked. Landscape only: a tall
     * tablet in portrait (the Tab S11 Ultra is ~1056dp wide even upright) has
     * the height to stack, and side-by-side panes there leave half of each
     * column empty.
     */
    val useTwoPane: Boolean
        get() = isLandscape && (widthClass == WidthClass.Expanded || isShortLandscape)

    /** Horizontal page gutter. */
    val gutter: Dp get() = if (isCompact) 16.dp else 24.dp
}

val LocalWindowLayout = staticCompositionLocalOf { WindowLayout(widthDp = 384, heightDp = 780) }

/**
 * Reads the current window size. The activity handles size and orientation
 * changes itself, so this recomposes on rotation and window resizing instead of
 * recreating the activity (which would drop playback and the TV session).
 */
@Composable
fun rememberWindowLayout(): WindowLayout {
    val configuration = LocalConfiguration.current
    return remember(configuration.screenWidthDp, configuration.screenHeightDp) {
        WindowLayout(configuration.screenWidthDp, configuration.screenHeightDp)
    }
}
