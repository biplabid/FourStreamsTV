package com.streamcast.shared.data

import android.content.Context
import com.streamcast.shared.model.ChannelCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persisted user state: favourites, recently watched, and settings.
 *
 * Backed by SharedPreferences and mirrored into StateFlows so Compose can
 * observe it. Favourites and recents are keyed by [com.streamcast.shared.model.Channel.id],
 * which M3uParser derives from the stream URL and therefore keeps stable across
 * playlist refreshes.
 */
class UserPreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("streamcast_prefs", Context.MODE_PRIVATE)

    private val _favouriteIds = MutableStateFlow(readStringList(KEY_FAVOURITES))
    val favouriteIds: StateFlow<List<String>> = _favouriteIds.asStateFlow()

    private val _recentIds = MutableStateFlow(readStringList(KEY_RECENTS))
    val recentIds: StateFlow<List<String>> = _recentIds.asStateFlow()

    private val _settings = MutableStateFlow(readSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun toggleFavourite(channelId: String) {
        val current = _favouriteIds.value
        val updated = if (current.contains(channelId)) {
            current - channelId
        } else {
            listOf(channelId) + current
        }
        _favouriteIds.value = updated
        writeStringList(KEY_FAVOURITES, updated)
    }

    fun isFavourite(channelId: String): Boolean = _favouriteIds.value.contains(channelId)

    fun recordWatched(channelId: String) {
        val updated = (listOf(channelId) + (_recentIds.value - channelId)).take(MAX_RECENTS)
        _recentIds.value = updated
        writeStringList(KEY_RECENTS, updated)
    }

    fun clearRecents() {
        _recentIds.value = emptyList()
        writeStringList(KEY_RECENTS, emptyList())
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val updated = transform(_settings.value)
        _settings.value = updated
        prefs.edit()
            .putString(KEY_PLAYLIST_URL, updated.playlistUrl)
            .putBoolean(KEY_AUTOPLAY, updated.autoPlayOnOpen)
            .putBoolean(KEY_KEEP_AWAKE, updated.keepScreenAwake)
            .putBoolean(KEY_PREFER_CAST, updated.preferCastWhenConnected)
            .putString(KEY_LANDING_CATEGORY, updated.defaultCategory.name)
            .apply()
    }

    fun resetOnboarding() {
        prefs.edit().putBoolean(KEY_ONBOARDED, false).apply()
    }

    var hasOnboarded: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDED, value).apply()

    private fun readSettings(): AppSettings = AppSettings(
        playlistUrl = prefs.getString(KEY_PLAYLIST_URL, PlaylistRepository.DEFAULT_PLAYLIST_URL)
            ?: PlaylistRepository.DEFAULT_PLAYLIST_URL,
        autoPlayOnOpen = prefs.getBoolean(KEY_AUTOPLAY, true),
        keepScreenAwake = prefs.getBoolean(KEY_KEEP_AWAKE, true),
        preferCastWhenConnected = prefs.getBoolean(KEY_PREFER_CAST, true),
        defaultCategory = runCatching {
            ChannelCategory.valueOf(
                prefs.getString(KEY_LANDING_CATEGORY, ChannelCategory.ALL.name)
                    ?: ChannelCategory.ALL.name
            )
        }.getOrDefault(ChannelCategory.ALL)
    )

    private fun readStringList(key: String): List<String> =
        prefs.getString(key, null)
            ?.split(SEPARATOR)
            ?.filter { it.isNotBlank() }
            ?: emptyList()

    private fun writeStringList(key: String, values: List<String>) {
        prefs.edit().putString(key, values.joinToString(SEPARATOR)).apply()
    }

    private companion object {
        const val KEY_FAVOURITES = "favourite_ids"
        const val KEY_RECENTS = "recent_ids"
        const val KEY_PLAYLIST_URL = "playlist_url"
        const val KEY_AUTOPLAY = "autoplay_on_open"
        const val KEY_KEEP_AWAKE = "keep_screen_awake"
        const val KEY_PREFER_CAST = "prefer_cast_when_connected"
        const val KEY_LANDING_CATEGORY = "landing_category"
        const val KEY_ONBOARDED = "has_onboarded"
        const val SEPARATOR = "\u001F"
        const val MAX_RECENTS = 20
    }
}

data class AppSettings(
    val playlistUrl: String = PlaylistRepository.DEFAULT_PLAYLIST_URL,
    val autoPlayOnOpen: Boolean = true,
    val keepScreenAwake: Boolean = true,
    val preferCastWhenConnected: Boolean = true,
    val defaultCategory: ChannelCategory = ChannelCategory.ALL
)
