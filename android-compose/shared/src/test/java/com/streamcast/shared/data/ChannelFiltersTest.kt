package com.streamcast.shared.data

import com.streamcast.shared.model.Channel
import com.streamcast.shared.model.ChannelCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelFiltersTest {

    private fun channel(
        id: String,
        name: String,
        category: ChannelCategory = ChannelCategory.OTHERS,
        language: String? = null,
        logoUrl: String? = null
    ) = Channel(
        id = id,
        name = name,
        streamUrl = "https://cdn.example.com/$id.m3u8",
        category = category,
        language = language,
        logoUrl = logoUrl
    )

    private val channels = listOf(
        channel("1", "DD News", ChannelCategory.NEWS, "Hindi"),
        channel("2", "Aaj Tak", ChannelCategory.NEWS, "Hindi"),
        channel("3", "Star Sports", ChannelCategory.SPORTS, "English"),
        channel("4", "Sun TV", ChannelCategory.REGIONAL, "Tamil"),
        channel("5", "DD National", ChannelCategory.ENTERTAINMENT, "Hindi")
    )

    @Test
    fun `category filter narrows the list`() {
        val news = ChannelFilters.filter(channels, ChannelCategory.NEWS, "")
        assertEquals(2, news.size)
        assertTrue(news.all { it.category == ChannelCategory.NEWS })
    }

    @Test
    fun `all category returns everything`() {
        assertEquals(channels.size, ChannelFilters.filter(channels, ChannelCategory.ALL, "").size)
    }

    @Test
    fun `search matches name, language and category`() {
        assertEquals(1, ChannelFilters.filter(channels, ChannelCategory.ALL, "aaj").size)
        assertEquals(1, ChannelFilters.filter(channels, ChannelCategory.ALL, "tamil").size)
        assertEquals(1, ChannelFilters.filter(channels, ChannelCategory.ALL, "sports").size)
        assertEquals(2, ChannelFilters.filter(channels, ChannelCategory.ALL, "dd ").size)
    }

    @Test
    fun `search is case insensitive and trimmed`() {
        assertEquals(1, ChannelFilters.filter(channels, ChannelCategory.ALL, "  AAJ TAK  ").size)
    }

    @Test
    fun `search and category combine`() {
        assertTrue(ChannelFilters.filter(channels, ChannelCategory.SPORTS, "aaj").isEmpty())
    }

    @Test
    fun `popular puts known national channels first`() {
        val popular = ChannelFilters.popular(channels, limit = 3)
        assertEquals("DD National", popular[0].name)
        assertEquals("DD News", popular[1].name)
    }

    @Test
    fun `available categories follow display order and skip empty ones`() {
        val available = ChannelFilters.availableCategories(channels)
        assertEquals(
            listOf(
                ChannelCategory.NEWS,
                ChannelCategory.ENTERTAINMENT,
                ChannelCategory.SPORTS,
                ChannelCategory.REGIONAL
            ),
            available
        )
    }

    @Test
    fun `counts by category are correct`() {
        val counts = ChannelFilters.countsByCategory(channels)
        assertEquals(2, counts[ChannelCategory.NEWS])
        assertEquals(1, counts[ChannelCategory.SPORTS])
        assertNull(counts[ChannelCategory.KIDS])
    }

    @Test
    fun `related prefers the same category and excludes the current channel`() {
        val related = ChannelFilters.related(channels, channels[0], limit = 10)
        assertTrue(related.none { it.id == channels[0].id })
        assertEquals("Aaj Tak", related.first().name)
    }

    @Test
    fun `adjacent wraps around at both ends`() {
        assertEquals("Aaj Tak", ChannelFilters.adjacent(channels, channels[0], true)?.name)
        assertEquals("DD National", ChannelFilters.adjacent(channels, channels[0], false)?.name)
        assertEquals("DD News", ChannelFilters.adjacent(channels, channels[4], true)?.name)
    }

    @Test
    fun `adjacent handles empty lists and unknown channels`() {
        assertNull(ChannelFilters.adjacent(emptyList(), channels[0], true))
        val unknown = channel("99", "Ghost")
        assertEquals("DD News", ChannelFilters.adjacent(channels, unknown, true)?.name)
    }

    @Test
    fun `resolveByIds preserves the given order and drops missing ids`() {
        val resolved = ChannelFilters.resolveByIds(channels, listOf("3", "missing", "1"))
        assertEquals(listOf("Star Sports", "DD News"), resolved.map { it.name })
    }
}
