package com.streamcast.shared.model

enum class ChannelCategory(val displayName: String) {
    ALL("All"),
    NEWS("News"),
    ENTERTAINMENT("Entertainment"),
    MOVIES("Movies"),
    SPORTS("Sports"),
    MUSIC("Music"),
    REGIONAL("Regional"),
    KIDS("Kids"),
    DEVOTIONAL("Devotional"),
    OTHERS("Others");

    companion object {
        /** Display order for category chips and shortcuts. Excludes ALL. */
        val browseOrder: List<ChannelCategory> = listOf(
            NEWS, ENTERTAINMENT, MOVIES, SPORTS, MUSIC, REGIONAL, KIDS, DEVOTIONAL, OTHERS
        )

        fun fromDisplayName(name: String?): ChannelCategory? =
            entries.firstOrNull { it.displayName.equals(name, ignoreCase = true) }
    }
}

/**
 * A single playable channel parsed from an M3U playlist.
 *
 * Every field here is derived from the playlist itself - nothing is invented.
 * Playlists carry no programme data, so there is no "current show" or viewer
 * count; [epgAvailable] stays false until an XMLTV source is wired up.
 */
data class Channel(
    val id: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val groupRaw: String? = null,
    val category: ChannelCategory = ChannelCategory.OTHERS,
    val language: String? = null,
    val quality: String? = null,
    val isLive: Boolean = true,
    val isGeoRestricted: Boolean = false,
    val epgAvailable: Boolean = false,
    /**
     * The playlist's tvg-id, e.g. "DDNational.in@HD". The part before '@' is
     * the iptv-org channel id, which is how guide data and channel metadata
     * are matched back to a stream.
     */
    val tvgId: String? = null
)

enum class CastProtocol(val label: String) {
    GOOGLE_CAST("Google Cast"),
    ANDROID_TV_REMOTE_V2("Android TV Remote v2"),
    GOOGLE_TV_CAST("Google TV Cast")
}

data class TvCapabilities(
    val power: Boolean,
    val voiceSearch: Boolean,
    val volumeControl: Boolean,
    val numericKeypad: Boolean,
    val textInput: Boolean
)

data class TvDevice(
    val id: String,
    val name: String,
    val deviceType: String,
    val protocol: CastProtocol,
    val ipAddress: String,
    val capabilities: TvCapabilities
)

sealed interface DiscoveryState {
    data object Idle : DiscoveryState
    data object Searching : DiscoveryState
    data class Found(val devices: List<TvDevice>) : DiscoveryState
    data class Connecting(val device: TvDevice) : DiscoveryState
    data class Connected(val device: TvDevice) : DiscoveryState
    data class Failed(val reason: String) : DiscoveryState
    data object Empty : DiscoveryState
}

/** What the receiver is doing with the channel we asked it to play. */
enum class CastPlaybackState { IDLE, LOADING, PLAYING, PAUSED, FAILED }

enum class PlaylistSource { NETWORK, CACHE, NONE }

data class PlaylistState(
    val channels: List<Channel> = emptyList(),
    val isLoading: Boolean = false,
    val source: PlaylistSource = PlaylistSource.NONE,
    val url: String = "",
    val fetchedAtMillis: Long = 0L,
    val errorMessage: String? = null
)
