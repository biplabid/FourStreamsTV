package com.streamcast.shared.atv

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The wire format is the contract with the TV. If these are wrong, the device
 * closes the socket with no diagnostic at all, so the encoder is pinned here
 * against byte sequences worked out from the protobuf specification by hand.
 */
class ProtoWireTest {

    @Test
    fun `varint field encodes tag then value`() {
        // field 1, wire type 0 -> tag 0x08; value 200 -> 0xC8 0x01
        val bytes = protoMessage { varint(1, 200) }
        assertArrayEquals(byteArrayOf(0x08, 0xC8.toByte(), 0x01), bytes)
    }

    @Test
    fun `single byte varint has no continuation bit`() {
        val bytes = protoMessage { varint(2, 3) }
        assertArrayEquals(byteArrayOf(0x10, 0x03), bytes)
    }

    @Test
    fun `field numbers above 15 use a two byte tag`() {
        // field 20, wire type 2 -> (20 << 3) | 2 = 162 -> 0xA2 0x01
        val bytes = protoMessage { message(20) {} }
        assertArrayEquals(byteArrayOf(0xA2.toByte(), 0x01, 0x00), bytes)
    }

    @Test
    fun `proto3 defaults are omitted`() {
        val bytes = protoMessage {
            varint(1, 0)
            bool(2, false)
            string(3, "")
            bytes(4, ByteArray(0))
        }
        assertEquals(0, bytes.size)
    }

    @Test
    fun `an empty submessage is still written`() {
        // PairingConfigurationAck carries no fields, and the peer must see it.
        val bytes = protoMessage { message(31) {} }
        assertArrayEquals(byteArrayOf(0xFA.toByte(), 0x01, 0x00), bytes)
        assertTrue(parseProto(bytes).has(31))
    }

    @Test
    fun `string field is length delimited utf8`() {
        val bytes = protoMessage { string(1, "hi") }
        assertArrayEquals(byteArrayOf(0x0A, 0x02, 'h'.code.toByte(), 'i'.code.toByte()), bytes)
    }

    @Test
    fun `round trips nested messages`() {
        val encoded = protoMessage {
            varint(1, 2)
            varint(2, 200)
            message(10) {
                string(1, "atvremote")
                string(2, "FourStreamsTV")
            }
        }
        val msg = parseProto(encoded)
        assertEquals(2, msg.int(1))
        assertEquals(200, msg.int(2))
        val inner = msg.message(10)!!
        assertEquals("atvremote", inner.string(1))
        assertEquals("FourStreamsTV", inner.string(2))
    }

    @Test
    fun `repeated submessages are all kept`() {
        val encoded = protoMessage {
            message(1) { varint(1, 3) }
            message(1) { varint(1, 4) }
        }
        val values = parseProto(encoded).messages(1).map { it.int(1) }
        assertEquals(listOf(3, 4), values)
    }

    @Test
    fun `absent fields read as null or false`() {
        val msg = parseProto(ByteArray(0))
        assertNull(msg.int(1))
        assertNull(msg.string(2))
        assertFalse(msg.bool(3))
        assertFalse(msg.has(4))
    }

    @Test
    fun `unknown fields do not break parsing of known ones`() {
        val encoded = protoMessage {
            varint(1, 7)
            varint(999, 1234)       // a field this build does not model
            string(2, "kept")
        }
        val msg = parseProto(encoded)
        assertEquals(7, msg.int(1))
        assertEquals("kept", msg.string(2))
    }

    @Test(expected = ProtoParseException::class)
    fun `truncated varint is rejected`() {
        parseProto(byteArrayOf(0x08, 0x80.toByte()))
    }

    @Test(expected = ProtoParseException::class)
    fun `length running past the buffer is rejected`() {
        parseProto(byteArrayOf(0x0A, 0x7F))
    }

    // ------------------------------------------------------------ framing

    @Test
    fun `frame prefixes the payload with its length`() {
        val framed = frameMessage(byteArrayOf(1, 2, 3))
        assertArrayEquals(byteArrayOf(0x03, 1, 2, 3), framed)
    }

    @Test
    fun `frame length over 127 uses a two byte varint`() {
        val framed = frameMessage(ByteArray(200) { 0x41 })
        assertEquals(0xC8.toByte(), framed[0])
        assertEquals(0x01.toByte(), framed[1])
        assertEquals(202, framed.size)
    }

    @Test
    fun `buffer yields several frames from one read`() {
        val buffer = VarintFrameBuffer()
        val data = frameMessage(byteArrayOf(1)) + frameMessage(byteArrayOf(2, 2))
        val frames = buffer.append(data)
        assertEquals(2, frames.size)
        assertArrayEquals(byteArrayOf(1), frames[0])
        assertArrayEquals(byteArrayOf(2, 2), frames[1])
        assertEquals(0, buffer.pending)
    }

    @Test
    fun `buffer holds a partial frame until the rest arrives`() {
        val buffer = VarintFrameBuffer()
        val whole = frameMessage(byteArrayOf(9, 9, 9))
        assertTrue(buffer.append(whole.copyOfRange(0, 2)).isEmpty())
        val frames = buffer.append(whole.copyOfRange(2, whole.size))
        assertEquals(1, frames.size)
        assertArrayEquals(byteArrayOf(9, 9, 9), frames[0])
    }

    @Test
    fun `buffer survives a split inside the length prefix`() {
        // A 200-byte payload needs a two-byte prefix; split between them.
        val buffer = VarintFrameBuffer()
        val whole = frameMessage(ByteArray(200) { 0x7 })
        assertTrue(buffer.append(whole.copyOfRange(0, 1)).isEmpty())
        val frames = buffer.append(whole.copyOfRange(1, whole.size))
        assertEquals(1, frames.size)
        assertEquals(200, frames[0].size)
    }

    @Test
    fun `buffer accepts a frame delivered one byte at a time`() {
        val buffer = VarintFrameBuffer()
        val whole = frameMessage(byteArrayOf(4, 5, 6, 7))
        val collected = mutableListOf<ByteArray>()
        for (b in whole) collected += buffer.append(byteArrayOf(b))
        assertEquals(1, collected.size)
        assertArrayEquals(byteArrayOf(4, 5, 6, 7), collected[0])
    }
}
