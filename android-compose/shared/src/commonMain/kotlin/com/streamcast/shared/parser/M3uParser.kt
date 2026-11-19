package com.streamcast.shared.parser

import com.streamcast.shared.model.Channel
import com.streamcast.shared.model.ChannelCategory

/**
 * Parser for the #EXTM3U playlist format as emitted by iptv-org and most
 * IPTV providers.
 *
 * Handles the cases that actually show up in Indian playlists:
 *  - attribute values containing commas (quote-aware title split)
 *  - #EXTGRP lines overriding the group-title attribute
 *  - [Geo-blocked] / [Not 24/7] markers appended to display names
 *  - resolution suffixes such as "(1080p)" folded into Channel.quality
 *  - duplicate stream URLs
 */
object M3uParser {

    private val attrRegex = Regex("""([a-zA-Z0-9_-]+)="([^"]*)"""")
    private val markerRegex = Regex("""\[(?:geo-blocked|not 24/7)]""", RegexOption.IGNORE_CASE)
    private val geoRegex = Regex("""\[geo-blocked]""", RegexOption.IGNORE_CASE)
    private val qualityRegex = Regex("""\((\d{3,4}p|4K|HD|SD)\)""", RegexOption.IGNORE_CASE)
    private val slugRegex = Regex("[^a-z0-9]+")
    private val multiSpaceRegex = Regex("""\s{2,}""")

    fun parse(rawM3u: String): List<Channel> {
        if (rawM3u.isBlank()) return emptyList()

        val channels = ArrayList<Channel>()
        val seenUrls = HashSet<String>()
        var pendingExtinf: String? = null
        var pendingGroup: String? = null

        rawM3u.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.isEmpty() -> Unit
                line.startsWith("#EXTM3U") -> Unit
                line.startsWith("#EXTINF:") -> {
                    pendingExtinf = line
                    pendingGroup = null
                }
                line.startsWith("#EXTGRP:") -> {
                    pendingGroup = line.removePrefix("#EXTGRP:").trim().ifBlank { null }
                }
                line.startsWith("#") -> Unit
                line.startsWith("http://", true) || line.startsWith("https://", true) -> {
                    if (seenUrls.add(line)) {
                        val channel = buildChannel(
                            extinf = pendingExtinf ?: "#EXTINF:-1,Unknown Channel",
                            streamUrl = line,
                            groupOverride = pendingGroup,
                            index = channels.size
                        )
                        channels.add(channel)
                    }
                    pendingExtinf = null
                    pendingGroup = null
                }
            }
        }

        return channels
    }

    private fun buildChannel(
        extinf: String,
        streamUrl: String,
        groupOverride: String?,
        index: Int
    ): Channel {
        val attrs = attrRegex.findAll(extinf)
            .associate { it.groupValues[1].lowercase() to it.groupValues[2].trim() }

        val commaIndex = titleCommaIndex(extinf)
        val rawTitle = if (commaIndex >= 0) extinf.substring(commaIndex + 1).trim() else ""

        val displaySource = rawTitle.ifBlank { attrs["tvg-name"].orEmpty() }
        val isGeoRestricted = geoRegex.containsMatchIn(displaySource)
        val quality = qualityRegex.find(displaySource)?.groupValues?.get(1)?.uppercase()

        val cleanName = displaySource
            .replace(markerRegex, "")
            .replace(qualityRegex, "")
            .replace(multiSpaceRegex, " ")
            .trim()
            .ifBlank { attrs["tvg-name"].orEmpty().trim() }
            .ifBlank { "Indian Live Channel" }

        val groupTitle = groupOverride ?: attrs["group-title"]?.ifBlank { null }

        return Channel(
            id = stableId(cleanName, streamUrl, index),
            name = cleanName,
            streamUrl = streamUrl,
            logoUrl = attrs["tvg-logo"]?.takeIf { it.startsWith("http", ignoreCase = true) },
            groupRaw = groupTitle,
            category = normalizeCategory(groupTitle, cleanName),
            language = attrs["tvg-language"]?.ifBlank { null },
            quality = quality,
            isLive = true,
            isGeoRestricted = isGeoRestricted,
            epgAvailable = false
        )
    }

    /**
     * The #EXTINF title is everything after the first comma that sits outside
     * a quoted attribute value. A naive substringAfter(",") breaks on
     * group-title="News, Hindi".
     */
    private fun titleCommaIndex(extinf: String): Int {
        var inQuotes = false
        for (i in extinf.indices) {
            val c = extinf[i]
            if (c == '"') {
                inQuotes = !inQuotes
            } else if (c == ',' && !inQuotes) {
                return i
            }
        }
        return extinf.lastIndexOf(',')
    }

    /**
     * Stable across reloads: the same channel keeps its id so favourites and
     * recents survive a playlist refresh even when ordering changes.
     */
    private fun stableId(name: String, streamUrl: String, index: Int): String {
        val slug = name.lowercase().replace(slugRegex, "-").trim('-').take(32)
        var hash = 0
        for (ch in streamUrl) {
            hash = hash * 31 + ch.code
        }
        val suffix = (hash.toLong() and 0xFFFFFFFFL).toString(36)
        val safeSlug = if (slug.isBlank()) "ch" else slug
        return "$safeSlug-$suffix-$index"
    }

    private val newsRegex =
        Regex("""\b(news|samachar|aaj ?tak|ndtv|dd news|sansad|republic|abp|india today|cnbc|wion|times now|cnn|mirror now|tv9|zee news|news18)\b""")
    private val moviesRegex =
        Regex("""\b(movie|movies|cinema|cine|film|filmy|gold|pictures|bollywood|talkies|utsav)\b""")
    private val sportsRegex =
        Regex("""\b(sport|sports|cricket|football|kabaddi|espn|star sports|sony ten|sony six|eurosport)\b""")
    private val musicRegex =
        Regex("""\b(music|mtv|9xm|9x ?jalwa|mastiii|zoom|songs|hits|vh1)\b""")
    private val kidsRegex =
        Regex("""\b(kid|kids|cartoon|pogo|nick|disney|hungama|sonic|chutti|bal|chintu)\b""")
    private val devotionalRegex =
        Regex("""\b(devotional|religious|aastha|sanskar|bhakti|spiritual|peace|sharnam|shraddha|sadhna|paras)\b""")
    private val regionalRegex =
        Regex("""\b(regional|tamil|telugu|malayalam|kannada|marathi|bengali|bangla|punjabi|gujarati|odia|oriya|assamese|bhojpuri|sun tv|etv|kairali|asianet|raj tv|jaya tv|kalaignar|manorama|mazhavil|surya|udaya|gemini|maa tv|star vijay|sahyadri|podhigai|chandana|saptagiri)\b""")
    private val entertainmentRegex =
        Regex("""\b(entertainment|general|sab|sony|star plus|colors|zee tv|dd national|dd bharati|dd india|comedy|lifestyle|serial|drama|e24|rishtey|big magic)\b""")

    /**
     * Ordered cascade - the first match wins, so specific genres are tested
     * before the catch-all Entertainment bucket. Matched against the group
     * title and channel name together, because Indian playlists are
     * inconsistent about which one carries the genre.
     */
    fun normalizeCategory(groupRaw: String?, channelName: String): ChannelCategory {
        val text = "${groupRaw.orEmpty()} $channelName".lowercase()
        return when {
            newsRegex.containsMatchIn(text) -> ChannelCategory.NEWS
            moviesRegex.containsMatchIn(text) -> ChannelCategory.MOVIES
            sportsRegex.containsMatchIn(text) -> ChannelCategory.SPORTS
            musicRegex.containsMatchIn(text) -> ChannelCategory.MUSIC
            kidsRegex.containsMatchIn(text) -> ChannelCategory.KIDS
            devotionalRegex.containsMatchIn(text) -> ChannelCategory.DEVOTIONAL
            regionalRegex.containsMatchIn(text) -> ChannelCategory.REGIONAL
            entertainmentRegex.containsMatchIn(text) -> ChannelCategory.ENTERTAINMENT
            else -> ChannelCategory.OTHERS
        }
    }
}
