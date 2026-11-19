package com.streamcast.shared.parser

import com.streamcast.shared.model.ChannelCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uParserTest {

    @Test
    fun `parses attributes and display name`() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1 tvg-id="DDNational.in" tvg-name="DD National" tvg-logo="https://example.com/dd.png" tvg-language="Hindi" group-title="Entertainment",DD National (1080p)
            https://cdn.example.com/dd/master.m3u8
        """.trimIndent()

        val channels = M3uParser.parse(playlist)

        assertEquals(1, channels.size)
        val channel = channels.first()
        assertEquals("DD National", channel.name)
        assertEquals("https://cdn.example.com/dd/master.m3u8", channel.streamUrl)
        assertEquals("https://example.com/dd.png", channel.logoUrl)
        assertEquals("Hindi", channel.language)
        assertEquals("1080P", channel.quality)
        assertEquals("Entertainment", channel.groupRaw)
        assertFalse(channel.isGeoRestricted)
    }

    @Test
    fun `title split is not broken by a comma inside an attribute value`() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1 group-title="News, Hindi" tvg-name="Aaj Tak",Aaj Tak
            https://cdn.example.com/aajtak.m3u8
        """.trimIndent()

        val channel = M3uParser.parse(playlist).single()

        assertEquals("Aaj Tak", channel.name)
        assertEquals("News, Hindi", channel.groupRaw)
    }

    @Test
    fun `EXTGRP overrides the group-title attribute`() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1 group-title="Misc",Sony Six
            #EXTGRP:Sports
            https://cdn.example.com/six.m3u8
        """.trimIndent()

        val channel = M3uParser.parse(playlist).single()

        assertEquals("Sports", channel.groupRaw)
        assertEquals(ChannelCategory.SPORTS, channel.category)
    }

    @Test
    fun `geo-blocked and not 24 7 markers are stripped into flags`() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1,Star Plus [Geo-blocked]
            https://cdn.example.com/star.m3u8
            #EXTINF:-1,Local Channel [Not 24/7]
            https://cdn.example.com/local.m3u8
        """.trimIndent()

        val channels = M3uParser.parse(playlist)

        assertEquals("Star Plus", channels[0].name)
        assertTrue(channels[0].isGeoRestricted)
        assertEquals("Local Channel", channels[1].name)
        assertFalse(channels[1].isGeoRestricted)
    }

    @Test
    fun `duplicate stream urls are dropped`() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1,Channel One
            https://cdn.example.com/same.m3u8
            #EXTINF:-1,Channel One Mirror
            https://cdn.example.com/same.m3u8
        """.trimIndent()

        assertEquals(1, M3uParser.parse(playlist).size)
    }

    @Test
    fun `non-http lines and comments are ignored`() {
        val playlist = """
            #EXTM3U
            #PLAYLIST:India
            #EXTINF:-1,Valid
            https://cdn.example.com/valid.m3u8
            #EXTINF:-1,Relative Path
            /not/a/url.m3u8
            #EXTVLCOPT:network-caching=1000
        """.trimIndent()

        val channels = M3uParser.parse(playlist)

        assertEquals(1, channels.size)
        assertEquals("Valid", channels.single().name)
    }

    @Test
    fun `falls back to tvg-name then a placeholder when the title is empty`() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1 tvg-name="Fallback Name",
            https://cdn.example.com/a.m3u8
            #EXTINF:-1,
            https://cdn.example.com/b.m3u8
        """.trimIndent()

        val channels = M3uParser.parse(playlist)

        assertEquals("Fallback Name", channels[0].name)
        assertEquals("Indian Live Channel", channels[1].name)
    }

    @Test
    fun `ids are stable across reparsing and unique per stream`() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1,Channel A
            https://cdn.example.com/a.m3u8
            #EXTINF:-1,Channel B
            https://cdn.example.com/b.m3u8
        """.trimIndent()

        val first = M3uParser.parse(playlist)
        val second = M3uParser.parse(playlist)

        assertEquals(first.map { it.id }, second.map { it.id })
        assertNotEquals(first[0].id, first[1].id)
    }

    @Test
    fun `empty and malformed input yields no channels`() {
        assertTrue(M3uParser.parse("").isEmpty())
        assertTrue(M3uParser.parse("   ").isEmpty())
        assertTrue(M3uParser.parse("#EXTM3U").isEmpty())
        assertTrue(M3uParser.parse("just some text").isEmpty())
    }

    @Test
    fun `logo is ignored when it is not an absolute url`() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1 tvg-logo="logos/x.png",Channel X
            https://cdn.example.com/x.m3u8
        """.trimIndent()

        assertNull(M3uParser.parse(playlist).single().logoUrl)
    }

    @Test
    fun `category cascade prefers the more specific genre`() {
        // "Sony Six" contains both an entertainment token (sony) and a sports
        // token; sports must win because it is checked first.
        assertEquals(
            ChannelCategory.SPORTS,
            M3uParser.normalizeCategory(null, "Sony Six")
        )
        assertEquals(
            ChannelCategory.NEWS,
            M3uParser.normalizeCategory("Entertainment", "DD News")
        )
        assertEquals(
            ChannelCategory.REGIONAL,
            M3uParser.normalizeCategory("Regional", "Asianet")
        )
        assertEquals(
            ChannelCategory.KIDS,
            M3uParser.normalizeCategory(null, "Pogo")
        )
        assertEquals(
            ChannelCategory.DEVOTIONAL,
            M3uParser.normalizeCategory(null, "Aastha TV")
        )
        assertEquals(
            ChannelCategory.OTHERS,
            M3uParser.normalizeCategory(null, "Unlabelled Feed 42")
        )
    }

    @Test
    fun `windows line endings are handled`() {
        val playlist = "#EXTM3U\r\n#EXTINF:-1,CRLF Channel\r\nhttps://cdn.example.com/crlf.m3u8\r\n"

        assertEquals("CRLF Channel", M3uParser.parse(playlist).single().name)
    }
}
