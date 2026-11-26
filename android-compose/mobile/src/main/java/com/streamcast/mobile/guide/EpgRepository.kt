package com.streamcast.mobile.guide

import android.content.Context
import android.util.Log
import android.util.Xml
import com.streamcast.shared.model.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.zip.GZIPInputStream

/** Where the schedule data stands, for the UI to explain an empty list. */
sealed interface EpgStatus {
    data object Idle : EpgStatus
    data object Loading : EpgStatus
    data class Ready(val matchedChannels: Int) : EpgStatus
    data class Failed(val message: String) : EpgStatus
}

/**
 * Programme schedules from public XMLTV guides.
 *
 * M3U playlists carry no schedule, so it comes from a separate community
 * guide matched back to the playlist by channel name. The guide files are
 * large (about 60 MB of XML uncompressed), so they are:
 *  - downloaded gzipped and cached for [CACHE_TTL_MS], never fetched per channel;
 *  - stream-parsed, never loaded into memory whole;
 *  - filtered while parsing to channels in the current playlist and to a
 *    window around now, so only a few thousand programmes are kept.
 */
class EpgRepository(context: Context) {

    private val cacheDir = File(context.applicationContext.cacheDir, "epg").apply { mkdirs() }
    private val mutex = Mutex()

    private val _status = MutableStateFlow<EpgStatus>(EpgStatus.Idle)
    val status: StateFlow<EpgStatus> = _status.asStateFlow()

    /** Playlist channel id -> programmes, sorted by start time. */
    private val _schedules = MutableStateFlow<Map<String, List<Programme>>>(emptyMap())
    val schedules: StateFlow<Map<String, List<Programme>>> = _schedules.asStateFlow()

    /** Playlist the current schedules were matched against. */
    private var loadedFor: Set<String> = emptySet()
    private var loadedAtMillis = 0L

    /**
     * Loads schedules for [channels] if not already loaded for this playlist.
     * Safe to call from every player screen; concurrent calls share one load.
     */
    suspend fun ensureLoaded(channels: List<Channel>, force: Boolean = false) {
        if (channels.isEmpty()) return
        mutex.withLock {
            val ids = channels.mapTo(HashSet()) { it.id }
            val fresh = System.currentTimeMillis() - loadedAtMillis < RELOAD_INTERVAL_MS
            if (!force && ids == loadedFor && fresh && _status.value is EpgStatus.Ready) return

            _status.value = EpgStatus.Loading
            val result = runCatching { load(channels, force) }
            result.onSuccess { map ->
                _schedules.value = map
                loadedFor = ids
                loadedAtMillis = System.currentTimeMillis()
                _status.value = EpgStatus.Ready(map.size)
                Log.d(TAG, "schedules ready for ${map.size} of ${channels.size} channels")
            }.onFailure { error ->
                Log.w(TAG, "guide load failed", error)
                _status.value = EpgStatus.Failed(
                    "The programme guide could not be downloaded. Check your connection."
                )
            }
        }
    }

    private suspend fun load(
        channels: List<Channel>,
        force: Boolean
    ): Map<String, List<Programme>> = withContext(Dispatchers.IO) {
        // Guide key -> playlist channel ids (several streams can share a guide entry).
        val byKey = HashMap<String, MutableList<String>>()
        channels.forEach { channel ->
            ChannelKeys.forChannel(channel).forEach { key ->
                byKey.getOrPut(key) { mutableListOf() } += channel.id
            }
        }

        val now = System.currentTimeMillis()
        val windowStart = now - KEEP_PAST_MS
        val windowEnd = now + KEEP_FUTURE_MS
        val out = HashMap<String, MutableList<Programme>>()
        var anySource = false

        GUIDE_URLS.forEachIndexed { index, url ->
            val file = File(cacheDir, "guide_$index.xml.gz")
            val usable = runCatching { download(url, file, force) }
                .onFailure { Log.w(TAG, "could not fetch $url", it) }
                .isSuccess || file.exists()
            if (!usable) return@forEachIndexed
            anySource = true
            runCatching {
                file.inputStream().buffered().use { raw ->
                    GZIPInputStream(raw, 64 * 1024).use { gz ->
                        parse(gz, byKey, windowStart, windowEnd, out)
                    }
                }
            }.onFailure { Log.w(TAG, "could not parse $url", it) }
        }
        if (!anySource) error("no guide source reachable")

        out.mapValues { (_, list) ->
            list.distinctBy { it.startMillis }.sortedBy { it.startMillis }
        }
    }

    /** Re-downloads only when the cached copy is older than [CACHE_TTL_MS]. */
    private fun download(url: String, target: File, force: Boolean) {
        val age = System.currentTimeMillis() - target.lastModified()
        if (!force && target.exists() && age < CACHE_TTL_MS) return

        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", USER_AGENT)
        }
        try {
            if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode}")
            val partial = File(target.parentFile, target.name + ".part")
            connection.inputStream.use { input ->
                partial.outputStream().use { input.copyTo(it, 64 * 1024) }
            }
            // Swap in only a complete file, so a dropped connection never
            // replaces a good cache with a truncated one.
            if (!partial.renameTo(target)) {
                target.delete()
                partial.renameTo(target)
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun parse(
        input: InputStream,
        byKey: Map<String, List<String>>,
        windowStart: Long,
        windowEnd: Long,
        out: MutableMap<String, MutableList<Programme>>
    ) {
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(input, "UTF-8")
        }
        val timeFormat = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US)

        // XMLTV lists every <channel> before any <programme>, so guide ids are
        // resolved to playlist channels before the first programme arrives.
        val guideIdToChannels = HashMap<String, List<String>>()
        var channelId: String? = null
        val channelNames = mutableListOf<String>()

        var progChannels: List<String>? = null
        var progStart = 0L
        var progStop = 0L
        var title: String? = null
        var desc: String? = null
        var image: String? = null

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "channel" -> {
                        channelId = parser.getAttributeValue(null, "id")
                        channelNames.clear()
                    }

                    "display-name" -> if (channelId != null) {
                        channelNames += parser.nextText()
                    }

                    "programme" -> {
                        val targets = guideIdToChannels[parser.getAttributeValue(null, "channel")]
                        val start = parseTime(timeFormat, parser.getAttributeValue(null, "start"))
                        val stop = parseTime(timeFormat, parser.getAttributeValue(null, "stop"))
                        // Skip unmatched channels and anything outside the window.
                        progChannels = if (
                            targets != null && start != null && stop != null &&
                            stop > windowStart && start < windowEnd
                        ) targets else null
                        progStart = start ?: 0L
                        progStop = stop ?: 0L
                        title = null
                        desc = null
                        image = null
                    }

                    "title" -> if (progChannels != null && title == null) {
                        title = parser.nextText().trim()
                    }

                    "desc" -> if (progChannels != null && desc == null) {
                        desc = parser.nextText().trim().ifBlank { null }
                    }

                    "icon" -> if (progChannels != null) {
                        image = parser.getAttributeValue(null, "src")
                    }
                }

                XmlPullParser.END_TAG -> when (parser.name) {
                    "channel" -> {
                        val id = channelId
                        if (id != null) {
                            val keys = ChannelKeys.of(id) + channelNames.flatMap { ChannelKeys.of(it) }
                            val matched = keys.flatMap { byKey[it].orEmpty() }.distinct()
                            if (matched.isNotEmpty()) guideIdToChannels[id] = matched
                        }
                        channelId = null
                    }

                    "programme" -> {
                        val targets = progChannels
                        val name = title
                        if (targets != null && !name.isNullOrBlank()) {
                            val programme = Programme(progStart, progStop, name, desc, image)
                            targets.forEach { out.getOrPut(it) { mutableListOf() } += programme }
                        }
                        progChannels = null
                    }
                }
            }
            event = parser.next()
        }
    }

    private fun parseTime(format: SimpleDateFormat, raw: String?): Long? =
        raw?.let { runCatching { format.parse(it)?.time }.getOrNull() }

    companion object {
        private const val TAG = "StreamCastEpg"

        /**
         * Free community XMLTV guides for Indian channels (epgshare01), rebuilt
         * daily. Together they cover roughly 40% of the iptv-org India list -
         * mainly the larger networks. Smaller free-to-air channels generally
         * publish no schedule anywhere.
         */
        val GUIDE_URLS = listOf(
            "https://epgshare01.online/epgshare01/epg_ripper_IN1.xml.gz",
            "https://epgshare01.online/epgshare01/epg_ripper_IN4.xml.gz"
        )

        private const val USER_AGENT = "FourStreamsTV/1.0 (Android)"

        /** Guides are rebuilt daily; twice a day keeps "now" accurate. */
        private const val CACHE_TTL_MS = 12L * 60 * 60 * 1000

        /** Re-match in memory at most this often, so day rollover is picked up. */
        private const val RELOAD_INTERVAL_MS = 3L * 60 * 60 * 1000

        private const val KEEP_PAST_MS = 6L * 60 * 60 * 1000
        private const val KEEP_FUTURE_MS = 3L * 24 * 60 * 60 * 1000
    }
}
