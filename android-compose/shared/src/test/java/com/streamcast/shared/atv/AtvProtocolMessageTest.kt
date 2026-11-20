package com.streamcast.shared.atv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Round-trips every message either side of the protocol sends, through the
 * same encoder and parser the socket uses.
 */
class AtvProtocolMessageTest {

    // ------------------------------------------------------------ pairing

    @Test
    fun `pairing request carries the service and client name`() {
        val msg = parseProto(PairingProto.pairingRequest("FourStreamsTV"))

        assertEquals(2, msg.int(PairingProto.PROTOCOL_VERSION))
        assertEquals(PairingProto.STATUS_OK, msg.int(PairingProto.STATUS))

        val request = msg.message(PairingProto.PAIRING_REQUEST)!!
        assertEquals("atvremote", request.string(1))
        assertEquals("FourStreamsTV", request.string(2))
    }

    @Test
    fun `pairing option asks for a six character hex code`() {
        val option = parseProto(PairingProto.pairingOption())
            .message(PairingProto.PAIRING_OPTION)!!

        val encoding = option.messages(1).single()
        assertEquals(PairingProto.ENCODING_TYPE_HEXADECIMAL, encoding.int(1))
        assertEquals(6, encoding.int(2))
        assertEquals(PairingProto.ROLE_TYPE_INPUT, option.int(3))
    }

    @Test
    fun `pairing configuration mirrors the chosen encoding`() {
        val config = parseProto(PairingProto.pairingConfiguration())
            .message(PairingProto.PAIRING_CONFIGURATION)!!

        val encoding = config.message(1)!!
        assertEquals(PairingProto.ENCODING_TYPE_HEXADECIMAL, encoding.int(1))
        assertEquals(6, encoding.int(2))
        assertEquals(PairingProto.ROLE_TYPE_INPUT, config.int(2))
    }

    @Test
    fun `pairing secret carries all thirty two hash bytes`() {
        val secret = ByteArray(32) { it.toByte() }
        val body = parseProto(PairingProto.pairingSecret(secret))
            .message(PairingProto.PAIRING_SECRET)!!
        assertEquals(32, body.bytes(1)!!.size)
    }

    @Test
    fun `parses each pairing reply the TV can send`() {
        val ack = protoMessage {
            varint(PairingProto.STATUS, PairingProto.STATUS_OK)
            message(PairingProto.PAIRING_REQUEST_ACK) { string(1, "Living Room TV") }
        }
        assertEquals(
            PairingProto.Incoming.RequestAck("Living Room TV"),
            PairingProto.parse(ack)
        )

        val configAck = protoMessage {
            varint(PairingProto.STATUS, PairingProto.STATUS_OK)
            message(PairingProto.PAIRING_CONFIGURATION_ACK) {}
        }
        assertEquals(PairingProto.Incoming.ConfigurationAck, PairingProto.parse(configAck))

        val secretAck = protoMessage {
            varint(PairingProto.STATUS, PairingProto.STATUS_OK)
            message(PairingProto.PAIRING_SECRET_ACK) { bytes(1, ByteArray(32)) }
        }
        assertEquals(PairingProto.Incoming.SecretAck, PairingProto.parse(secretAck))
    }

    @Test
    fun `a bad secret status outranks whatever body came with it`() {
        val rejected = protoMessage {
            varint(PairingProto.STATUS, PairingProto.STATUS_BAD_SECRET)
            message(PairingProto.PAIRING_SECRET_ACK) {}
        }
        assertEquals(
            PairingProto.Incoming.Failed(PairingProto.STATUS_BAD_SECRET),
            PairingProto.parse(rejected)
        )
    }

    // ------------------------------------------------------------- remote

    @Test
    fun `configure reports the negotiated feature set and our identity`() {
        val features = RemoteProto.Feature.PING or RemoteProto.Feature.KEY
        val cfg = parseProto(
            RemoteProto.configure(features, "SM-S921B", "samsung", "com.fourstreams.tv", "1.0")
        ).message(RemoteProto.REMOTE_CONFIGURE)!!

        assertEquals(features, cfg.int(1))
        val device = cfg.message(2)!!
        assertEquals("SM-S921B", device.string(1))
        assertEquals("samsung", device.string(2))
        assertEquals(1, device.int(3))
        assertEquals("1", device.string(4))
        assertEquals("com.fourstreams.tv", device.string(5))
    }

    @Test
    fun `key inject carries code and direction`() {
        val inject = parseProto(
            RemoteProto.keyInject(AtvKey.DPAD_CENTER.code, RemoteProto.Direction.SHORT)
        ).message(RemoteProto.REMOTE_KEY_INJECT)!!

        assertEquals(23, inject.int(1))
        assertEquals(3, inject.int(2))
    }

    @Test
    fun `a long press is a start and an end`() {
        val start = parseProto(RemoteProto.keyInject(20, RemoteProto.Direction.START_LONG))
            .message(RemoteProto.REMOTE_KEY_INJECT)!!
        val end = parseProto(RemoteProto.keyInject(20, RemoteProto.Direction.END_LONG))
            .message(RemoteProto.REMOTE_KEY_INJECT)!!

        assertEquals(1, start.int(2))
        assertEquals(2, end.int(2))
    }

    @Test
    fun `ime insert leaves the caret after the inserted text`() {
        val batch = parseProto(RemoteProto.imeInsertText("hello", imeCounter = 4, fieldCounter = 9))
            .message(RemoteProto.REMOTE_IME_BATCH_EDIT)!!

        assertEquals(4, batch.int(1))
        assertEquals(9, batch.int(2))

        val edit = batch.messages(3).single()
        assertEquals(1, edit.int(1))
        val obj = edit.message(2)!!
        assertEquals(5, obj.int(1))
        assertEquals(5, obj.int(2))
        assertEquals("hello", obj.string(3))
    }

    @Test
    fun `app link launch carries the uri`() {
        val body = parseProto(RemoteProto.appLinkLaunch("https://www.youtube.com/tv"))
            .message(RemoteProto.REMOTE_APP_LINK_LAUNCH_REQUEST)!!
        assertEquals("https://www.youtube.com/tv", body.string(1))
    }

    @Test
    fun `parses the TV's opening configure`() {
        val frame = protoMessage {
            message(RemoteProto.REMOTE_CONFIGURE) {
                varint(1, 623)
                message(2) {
                    string(1, "BRAVIA 4K")
                    string(2, "Sony")
                    string(6, "6.1.0")
                }
            }
        }
        val parsed = RemoteProto.parse(frame) as RemoteProto.Incoming.Configure
        assertEquals(623, parsed.supportedFeatures)
        assertEquals("BRAVIA 4K", parsed.deviceInfo.model)
        assertEquals("Sony", parsed.deviceInfo.vendor)
        assertEquals("6.1.0", parsed.deviceInfo.appVersion)
    }

    @Test
    fun `parses ping requests so they can be answered`() {
        val frame = protoMessage {
            message(RemoteProto.REMOTE_PING_REQUEST) {
                varint(1, 42)
                varint(2, 1)
            }
        }
        assertEquals(RemoteProto.Incoming.PingRequest(42), RemoteProto.parse(frame))

        val reply = parseProto(RemoteProto.pingResponse(42))
            .message(RemoteProto.REMOTE_PING_RESPONSE)!!
        assertEquals(42, reply.int(1))
    }

    @Test
    fun `parses volume updates`() {
        val frame = protoMessage {
            message(RemoteProto.REMOTE_SET_VOLUME_LEVEL) {
                string(3, "BRAVIA")
                varint(6, 100)
                varint(7, 23)
                bool(8, true)
            }
        }
        val volume = (RemoteProto.parse(frame) as RemoteProto.Incoming.Volume).info
        assertEquals(23, volume.level)
        assertEquals(100, volume.max)
        assertTrue(volume.muted)
        assertEquals("BRAVIA", volume.playerModel)
    }

    @Test
    fun `remote start with no body means the screen is off`() {
        // proto3 omits a false bool, so an empty RemoteStart is started = false.
        val off = protoMessage { message(RemoteProto.REMOTE_START) {} }
        assertFalse((RemoteProto.parse(off) as RemoteProto.Incoming.Start).started)

        val on = protoMessage { message(RemoteProto.REMOTE_START) { bool(1, true) } }
        assertTrue((RemoteProto.parse(on) as RemoteProto.Incoming.Start).started)
    }

    @Test
    fun `ime key inject reveals the foreground app and the field counters`() {
        val frame = protoMessage {
            message(RemoteProto.REMOTE_IME_KEY_INJECT) {
                message(1) {
                    varint(1, 11)
                    string(12, "com.google.android.youtube.tv")
                }
                message(2) {
                    varint(1, 5)
                    string(6, "Search")
                }
            }
        }
        val parsed = RemoteProto.parse(frame) as RemoteProto.Incoming.ImeKeyInject
        assertEquals("com.google.android.youtube.tv", parsed.appPackage)
        assertEquals(11, parsed.imeCounter)
        assertEquals(5, parsed.fieldCounter)
        assertEquals("Search", parsed.fieldLabel)
    }

    @Test
    fun `an unmodelled message is reported rather than throwing`() {
        val frame = protoMessage { message(61) {} }
        assertTrue(RemoteProto.parse(frame) is RemoteProto.Incoming.Unknown)
    }

    @Test
    fun `requested feature set covers what the UI drives`() {
        val requested = RemoteProto.Feature.REQUESTED_BASE
        assertTrue(requested and RemoteProto.Feature.KEY != 0)
        assertTrue(requested and RemoteProto.Feature.PING != 0)
        assertTrue(requested and RemoteProto.Feature.VOLUME != 0)
        assertTrue(requested and RemoteProto.Feature.APP_LINK != 0)
        assertTrue(requested and RemoteProto.Feature.IME != 0)
        // Voice is opt-in: it needs the microphone permission.
        assertEquals(0, requested and RemoteProto.Feature.VOICE)
    }
}
