package com.streamcast.shared.data

import android.content.Context
import com.streamcast.shared.model.Channel
import com.streamcast.shared.model.PlaylistSource
import com.streamcast.shared.model.PlaylistState
import com.streamcast.shared.parser.M3uParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Single source of truth for the channel list.
 *
 * Load order on startup:
 *   1. on-disk cache (instant, so the UI is never empty)
 *   2. network refresh if the cache is missing or older than [CACHE_TTL_MS]
 *
 * A failed network fetch never clears a usable cache - the previous channels
 * stay on screen and the error is surfaced alongside them.
 */
class PlaylistRepository(
    context: Context,
    private val remoteSource: PlaylistRemoteSource = PlaylistRemoteSource()
) {
    private val appContext = context.applicationContext
    private val cacheFile = File(appContext.filesDir, CACHE_FILENAME)
    private val metaFile = File(appContext.filesDir, META_FILENAME)
    private val refreshMutex = Mutex()

    private val _state = MutableStateFlow(PlaylistState())
    val state: StateFlow<PlaylistState> = _state.asStateFlow()

    /**
     * Called once on app start. Serves the cache immediately, then refreshes
     * from the network when the cache is stale, missing, or for a different URL.
     */
    suspend fun initialise(playlistUrl: String) {
        val cached = readCache()
        if (cached != null && cached.url == playlistUrl) {
            val channels = parse(cached.raw)
            if (channels.isNotEmpty()) {
                _state.value = PlaylistState(
                    channels = channels,
                    isLoading = false,
                    source = PlaylistSource.CACHE,
                    url = cached.url,
                    fetchedAtMillis = cached.fetchedAtMillis
                )
            }
            val age = System.currentTimeMillis() - cached.fetchedAtMillis
            if (age < CACHE_TTL_MS && channels.isNotEmpty()) {
                return
            }
        }
        refresh(playlistUrl, force = cached == null || cached.url != playlistUrl)
    }

    /**
     * Fetches the playlist from the network. Safe to call concurrently - the
     * mutex collapses overlapping refreshes.
     */
    suspend fun refresh(playlistUrl: String, force: Boolean = true): Result<Int> =
        refreshMutex.withLock {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)

            val result = remoteSource.fetch(playlistUrl)

            result.fold(
                onSuccess = { raw ->
                    val channels = parse(raw)
                    if (channels.isEmpty()) {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            errorMessage = "Playlist contained no playable channels."
                        )
                        return@withLock Result.failure(
                            PlaylistException("Playlist contained no playable channels.")
                        )
                    }

                    val now = System.currentTimeMillis()
                    writeCache(playlistUrl, raw, now)

                    _state.value = PlaylistState(
                        channels = channels,
                        isLoading = false,
                        source = PlaylistSource.NETWORK,
                        url = playlistUrl,
                        fetchedAtMillis = now,
                        errorMessage = null
                    )
                    Result.success(channels.size)
                },
                onFailure = { error ->
                    // Keep whatever is already on screen; only report the failure.
                    val current = _state.value
                    _state.value = current.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Could not refresh the playlist.",
                        source = if (current.channels.isEmpty()) {
                            PlaylistSource.NONE
                        } else {
                            current.source
                        }
                    )
                    Result.failure(error)
                }
            )
        }

    private suspend fun parse(raw: String): List<Channel> = withContext(Dispatchers.Default) {
        M3uParser.parse(raw)
    }

    private data class CachedPlaylist(
        val url: String,
        val raw: String,
        val fetchedAtMillis: Long
    )

    private suspend fun readCache(): CachedPlaylist? = withContext(Dispatchers.IO) {
        try {
            if (!cacheFile.exists() || !metaFile.exists()) return@withContext null
            val meta = metaFile.readText().split('\n')
            if (meta.size < 2) return@withContext null
            CachedPlaylist(
                url = meta[0],
                raw = cacheFile.readText(),
                fetchedAtMillis = meta[1].toLongOrNull() ?: 0L
            )
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun writeCache(url: String, raw: String, atMillis: Long) {
        withContext(Dispatchers.IO) {
            try {
                cacheFile.writeText(raw)
                metaFile.writeText("$url\n$atMillis")
            } catch (_: Exception) {
                // A cache write failure is not worth interrupting playback for.
            }
        }
    }

    companion object {
        const val DEFAULT_PLAYLIST_URL = "https://iptv-org.github.io/iptv/countries/in.m3u"
        private const val CACHE_FILENAME = "playlist_cache.m3u"
        private const val META_FILENAME = "playlist_cache_meta.txt"
        private const val CACHE_TTL_MS = 6L * 60 * 60 * 1000 // 6 hours
    }
}
