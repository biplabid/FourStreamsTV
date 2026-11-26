package com.streamcast.mobile.guide

import com.streamcast.shared.model.Channel

/** One entry in a channel's broadcast schedule. Times are epoch millis. */
data class Programme(
    val startMillis: Long,
    val stopMillis: Long,
    val title: String,
    val description: String?,
    val imageUrl: String?
) {
    fun isOnAt(nowMillis: Long): Boolean = nowMillis in startMillis until stopMillis
}

/** Facts about a channel, gathered from public catalogues. Any field may be missing. */
data class ChannelInfo(
    val network: String? = null,
    val owners: List<String> = emptyList(),
    val launched: String? = null,
    val website: String? = null,
    val summary: String? = null,
    val summarySource: String? = null,
    val summaryUrl: String? = null
) {
    val isEmpty: Boolean
        get() = network == null && owners.isEmpty() && launched == null &&
            website == null && summary == null
}

/**
 * Loose channel-name matching between independent catalogues.
 *
 * The playlist says "DDNational.in@HD" / "DD National HD", a guide says
 * "DD.National.in" / "DD National", another says "ABP.News.India.in". Lower-
 * casing, dropping punctuation and the ".in" country suffix, and peeling
 * trailing "india"/"hd"/"sd"/"tv" variants lines most of these up without
 * fuzzy scoring that would pair the wrong regional feeds.
 */
internal object ChannelKeys {

    private val parenthetical = Regex("""\(.*?\)""")
    private val nonAlnum = Regex("[^a-z0-9]")
    private val peelable = listOf("india", "hd", "sd", "tv")

    fun of(raw: String?): Set<String> {
        if (raw.isNullOrBlank()) return emptySet()
        var key = raw.lowercase()
            .substringBefore('@')
            .removeSuffix(".in")
            .replace(parenthetical, "")
            .replace(nonAlnum, "")
        if (key.isEmpty()) return emptySet()

        val keys = linkedSetOf(key)
        var peeled = true
        while (peeled) {
            peeled = false
            for (suffix in peelable) {
                // Keep at least a few characters so "Zee TV" never becomes "zee".
                if (key.endsWith(suffix) && key.length > suffix.length + 3) {
                    key = key.removeSuffix(suffix)
                    keys += key
                    peeled = true
                }
            }
        }
        return keys
    }

    /** Every key a playlist channel could be known by. */
    fun forChannel(channel: Channel): Set<String> = of(channel.tvgId) + of(channel.name)
}
