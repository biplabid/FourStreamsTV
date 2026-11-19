package com.streamcast.mobile.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.streamcast.shared.data.AppSettings
import com.streamcast.shared.data.ChannelFilters
import com.streamcast.shared.data.PlaylistRepository
import com.streamcast.shared.data.UserPreferences
import com.streamcast.shared.model.Channel
import com.streamcast.shared.model.ChannelCategory
import com.streamcast.shared.model.PlaylistState
import com.streamcast.mobile.AppGraph
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Everything the channel browsing UI needs: the parsed playlist, the active
 * search and category filter, favourites and recents.
 *
 * All values are derived from the playlist on disk or on the network. Where the
 * previous build showed programme titles, viewer counts and uptime figures,
 * those simply do not exist in an M3U playlist and are gone.
 */
class ChannelsViewModel(
    private val repository: PlaylistRepository,
    private val preferences: UserPreferences
) : ViewModel() {

    val playlist: StateFlow<PlaylistState> = repository.state

    val settings: StateFlow<AppSettings> = preferences.settings

    private val _selectedCategory = MutableStateFlow(ChannelCategory.ALL)
    val selectedCategory: StateFlow<ChannelCategory> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    val favouriteIds: StateFlow<List<String>> = preferences.favouriteIds

    /** Channels matching the active category and search text. */
    val filteredChannels: StateFlow<List<Channel>> =
        combine(playlist, _selectedCategory, _searchQuery) { state, category, query ->
            ChannelFilters.filter(state.channels, category, query)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val availableCategories: StateFlow<List<ChannelCategory>> =
        playlist.map { ChannelFilters.availableCategories(it.channels) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categoryCounts: StateFlow<Map<ChannelCategory, Int>> =
        playlist.map { ChannelFilters.countsByCategory(it.channels) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val popularChannels: StateFlow<List<Channel>> =
        playlist.map { ChannelFilters.popular(it.channels, limit = 12) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favouriteChannels: StateFlow<List<Channel>> =
        combine(playlist, preferences.favouriteIds) { state, ids ->
            ChannelFilters.resolveByIds(state.channels, ids)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentChannels: StateFlow<List<Channel>> =
        combine(playlist, preferences.recentIds) { state, ids ->
            ChannelFilters.resolveByIds(state.channels, ids)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            repository.initialise(preferences.settings.value.playlistUrl)
        }
    }

    fun selectCategory(category: ChannelCategory) {
        _selectedCategory.value = category
    }

    fun selectCategoryByName(displayName: String?) {
        ChannelCategory.fromDisplayName(displayName)?.let { _selectedCategory.value = it }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun refresh() {
        viewModelScope.launch {
            val url = preferences.settings.value.playlistUrl
            repository.refresh(url).fold(
                onSuccess = { count -> _toast.value = "Playlist refreshed - $count channels" },
                onFailure = { error ->
                    _toast.value = error.message ?: "Could not refresh the playlist"
                }
            )
        }
    }

    fun updatePlaylistUrl(url: String) {
        val trimmed = url.trim()
        if (!trimmed.startsWith("http://", true) && !trimmed.startsWith("https://", true)) {
            _toast.value = "Playlist URL must start with http:// or https://"
            return
        }
        preferences.updateSettings { it.copy(playlistUrl = trimmed) }
        viewModelScope.launch {
            repository.refresh(trimmed).fold(
                onSuccess = { count -> _toast.value = "Loaded $count channels" },
                onFailure = { error -> _toast.value = error.message ?: "Could not load playlist" }
            )
        }
    }

    fun resetPlaylistUrl() {
        updatePlaylistUrl(PlaylistRepository.DEFAULT_PLAYLIST_URL)
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        preferences.updateSettings(transform)
    }

    fun toggleFavourite(channelId: String) {
        preferences.toggleFavourite(channelId)
    }

    fun isFavourite(channelId: String): Boolean = preferences.isFavourite(channelId)

    fun recordWatched(channelId: String) {
        preferences.recordWatched(channelId)
    }

    fun clearRecents() {
        preferences.clearRecents()
        _toast.value = "Recently watched cleared"
    }

    fun channelById(id: String): Channel? =
        repository.state.value.channels.firstOrNull { it.id == id }

    /** Next/previous within the currently filtered list, so it matches what the user sees. */
    fun adjacentChannel(current: Channel?, forward: Boolean): Channel? {
        val pool = filteredChannels.value.ifEmpty { playlist.value.channels }
        return ChannelFilters.adjacent(pool, current, forward)
    }

    fun relatedChannels(current: Channel?, limit: Int = 12): List<Channel> =
        ChannelFilters.related(playlist.value.channels, current, limit)

    fun showToast(message: String) {
        _toast.value = message
    }

    fun consumeToast() {
        _toast.value = null
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ChannelsViewModel(
                repository = AppGraph.playlistRepository(context),
                preferences = AppGraph.userPreferences(context)
            ) as T
    }
}
