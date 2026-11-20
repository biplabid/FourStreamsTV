package com.streamcast.shared.atv

/**
 * The subset of Android key codes this remote exposes.
 *
 * Values come from the RemoteKeyCode enum in remotemessage.proto, which is
 * itself a copy of the platform's `keycodes.h`, so these numbers are the same
 * ones a physical Android TV remote injects.
 *
 * The full enum has 300-odd entries; only keys with somewhere sensible to live
 * in the UI are listed. Adding one is just a matter of its number.
 */
enum class AtvKey(val code: Int) {

    // Navigation
    DPAD_UP(19),
    DPAD_DOWN(20),
    DPAD_LEFT(21),
    DPAD_RIGHT(22),
    DPAD_CENTER(23),
    BACK(4),
    HOME(3),
    MENU(82),
    APP_SWITCH(187),
    ALL_APPS(284),

    // Power
    POWER(26),
    TV_POWER(177),
    SLEEP(223),
    WAKEUP(224),

    // Volume
    VOLUME_UP(24),
    VOLUME_DOWN(25),
    VOLUME_MUTE(164),

    // Transport
    MEDIA_PLAY_PAUSE(85),
    MEDIA_PLAY(126),
    MEDIA_PAUSE(127),
    MEDIA_STOP(86),
    MEDIA_NEXT(87),
    MEDIA_PREVIOUS(88),
    MEDIA_REWIND(89),
    MEDIA_FAST_FORWARD(90),
    MEDIA_SKIP_FORWARD(272),
    MEDIA_SKIP_BACKWARD(273),

    // Television
    CHANNEL_UP(166),
    CHANNEL_DOWN(167),
    GUIDE(172),
    INFO(165),
    CAPTIONS(175),
    SETTINGS(176),
    TV_INPUT(178),
    LAST_CHANNEL(229),

    // Text entry and search
    SEARCH(84),
    ASSIST(219),
    VOICE_ASSIST(231),
    ENTER(66),
    DEL(67),

    // Digits, for typing a channel number straight on the TV
    DIGIT_0(7),
    DIGIT_1(8),
    DIGIT_2(9),
    DIGIT_3(10),
    DIGIT_4(11),
    DIGIT_5(12),
    DIGIT_6(13),
    DIGIT_7(14),
    DIGIT_8(15),
    DIGIT_9(16);

    companion object {
        /** The key for a single digit character, or null if [c] is not 0-9. */
        fun forDigit(c: Char): AtvKey? = when (c) {
            '0' -> DIGIT_0
            '1' -> DIGIT_1
            '2' -> DIGIT_2
            '3' -> DIGIT_3
            '4' -> DIGIT_4
            '5' -> DIGIT_5
            '6' -> DIGIT_6
            '7' -> DIGIT_7
            '8' -> DIGIT_8
            '9' -> DIGIT_9
            else -> null
        }
    }
}

/**
 * A deep link that opens an app on the TV.
 *
 * App launching goes through RemoteAppLinkLaunchRequest, which takes a URI
 * rather than a package name: the TV resolves it the same way it would resolve
 * the link arriving from anywhere else. An app that is not installed simply
 * does nothing, so these are offered as shortcuts rather than promises.
 */
data class AtvAppLink(
    val label: String,
    val uri: String,
    /** Package name, used only to check whether to show the shortcut. */
    val packageName: String
) {
    companion object {
        val YOUTUBE = AtvAppLink("YouTube", "https://www.youtube.com/tv", "com.google.android.youtube.tv")
        val NETFLIX = AtvAppLink("Netflix", "https://www.netflix.com/title", "com.netflix.ninja")
        val PRIME_VIDEO = AtvAppLink("Prime Video", "https://app.primevideo.com", "com.amazon.amazonvideo.livingroom")
        val DISNEY_HOTSTAR = AtvAppLink("JioHotstar", "https://www.hotstar.com", "in.startv.hotstar.dplus.tv")
        val JIOCINEMA = AtvAppLink("JioCinema", "https://www.jiocinema.com", "com.jio.media.stb.ondemand")
        val SONYLIV = AtvAppLink("SonyLIV", "https://www.sonyliv.com", "com.sonyliv.tv")
        val ZEE5 = AtvAppLink("ZEE5", "https://www.zee5.com", "com.graymatrix.did.tv")
        val SPOTIFY = AtvAppLink("Spotify", "https://open.spotify.com", "com.spotify.tv.android")

        /** Shown in the shortcuts row, in this order. */
        val DEFAULTS = listOf(
            YOUTUBE, NETFLIX, PRIME_VIDEO, DISNEY_HOTSTAR, JIOCINEMA, SONYLIV, ZEE5, SPOTIFY
        )
    }
}
