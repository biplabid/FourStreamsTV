package com.streamcast.shared.data

import com.streamcast.shared.model.Channel
import com.streamcast.shared.model.ChannelCategory

/**
 * Pure list operations over the parsed channel set. Kept free of Android types
 * so they stay trivially testable.
 */
object ChannelFilters {

    /**
     * Channels that get surfaced first on the home screen. Matched as a
     * substring against the channel name, in priority order.
     */
    private val POPULAR_PRIORITY = listOf(
        "dd national",
        "dd news",
        "sansad",
        "aaj tak",
        "ndtv india",
        "india today",
        "abp news",
        "republic",
        "dd india",
        "zee news"
    )

    fun filter(
        channels: List<Channel>,
        category: ChannelCategory,
        query: String
    ): List<Channel> {
        val trimmed = query.trim().lowercase()
        return channels.filter { channel ->
            if (category != ChannelCategory.ALL && channel.category != category) return@filter false
            if (trimmed.isEmpty()) return@filter true
            channel.name.lowercase().contains(trimmed) ||
                channel.category.displayName.lowercase().contains(trimmed) ||
                channel.groupRaw?.lowercase()?.contains(trimmed) == true ||
                channel.language?.lowercase()?.contains(trimmed) == true
        }
    }

    /** Categories that actually have channels, in display order. */
    fun availableCategories(channels: List<Channel>): List<ChannelCategory> {
        val present = channels.mapTo(HashSet()) { it.category }
        return ChannelCategory.browseOrder.filter { present.contains(it) }
    }

    fun countsByCategory(channels: List<Channel>): Map<ChannelCategory, Int> =
        channels.groupingBy { it.category }.eachCount()

    /**
     * Well-known national channels first, then anything with a logo (those
     * render better in the UI), then alphabetical.
     */
    fun popular(channels: List<Channel>, limit: Int = 12): List<Channel> {
        if (channels.isEmpty()) return emptyList()
        return channels.sortedWith(
            compareBy(
                { channel ->
                    val name = channel.name.lowercase()
                    val idx = POPULAR_PRIORITY.indexOfFirst { name.contains(it) }
                    if (idx == -1) Int.MAX_VALUE else idx
                },
                { if (it.logoUrl != null) 0 else 1 },
                { it.name.lowercase() }
            )
        ).take(limit)
    }

    /** Same-category channels first, then the rest, excluding the current one. */
    fun related(channels: List<Channel>, current: Channel?, limit: Int = 12): List<Channel> {
        if (current == null) return channels.take(limit)
        val remaining = channels.filter { it.id != current.id }
        val (sameCategory, others) = remaining.partition { it.category == current.category }
        return (sameCategory + others).take(limit)
    }

    /** Wraps around at both ends so channel up/down never dead-ends. */
    fun adjacent(channels: List<Channel>, current: Channel?, forward: Boolean): Channel? {
        if (channels.isEmpty()) return null
        if (current == null) return channels.first()
        val index = channels.indexOfFirst { it.id == current.id }
        if (index == -1) return channels.first()
        val offset = if (forward) 1 else -1
        val target = ((index + offset) % channels.size + channels.size) % channels.size
        return channels[target]
    }

    fun resolveByIds(channels: List<Channel>, ids: List<String>): List<Channel> {
        if (ids.isEmpty() || channels.isEmpty()) return emptyList()
        val byId = channels.associateBy { it.id }
        return ids.mapNotNull { byId[it] }
    }
}
