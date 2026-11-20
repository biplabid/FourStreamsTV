package com.streamcast.shared.atv

import java.math.BigInteger
import java.security.MessageDigest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PairingCryptoTest {

    private val clientKey = PairingCrypto.RsaPublicNumbers(
        modulus = BigInteger("00C7A1F3B5", 16),   // high bit set: the tricky case
        exponent = BigInteger("010001", 16)
    )
    private val serverKey = PairingCrypto.RsaPublicNumbers(
        modulus = BigInteger("00913BEE2D", 16),
        exponent = BigInteger("010001", 16)
    )

    /**
     * Builds the digest independently of the production call path, so this
     * checks the order of the four key components and the suffix, not just
     * that the code agrees with itself.
     */
    private fun expectedHash(suffix: String): ByteArray =
        MessageDigest.getInstance("SHA-256").apply {
            update(byteArrayOf(0xC7.toByte(), 0xA1.toByte(), 0xF3.toByte(), 0xB5.toByte()))
            update(byteArrayOf(0x01, 0x00, 0x01))
            update(byteArrayOf(0x91.toByte(), 0x3B, 0xEE.toByte(), 0x2D))
            update(byteArrayOf(0x01, 0x00, 0x01))
            update(suffix.hexToBytes())
        }.digest()

    private fun validCodeFor(suffix: String): String {
        val check = expectedHash(suffix)[0].toInt() and 0xFF
        return "%02X".format(check) + suffix
    }

    @Test
    fun `secret is the sha256 over both public keys and the code suffix`() {
        val suffix = "1A2B"
        val code = validCodeFor(suffix)

        val secret = PairingCrypto.secretFor(clientKey, serverKey, code)

        assertEquals(32, secret.size)
        assertArrayEquals(expectedHash(suffix), secret)
    }

    @Test
    fun `first hash byte matches the leading two characters of the code`() {
        val code = validCodeFor("ABCD")
        val secret = PairingCrypto.secretFor(clientKey, serverKey, code)
        assertEquals(code.substring(0, 2).toInt(16), secret[0].toInt() and 0xFF)
    }

    @Test
    fun `code is case insensitive`() {
        val code = validCodeFor("ABCD")
        val lower = PairingCrypto.secretFor(clientKey, serverKey, code.lowercase())
        val upper = PairingCrypto.secretFor(clientKey, serverKey, code.uppercase())
        assertArrayEquals(upper, lower)
    }

    @Test
    fun `surrounding whitespace is tolerated`() {
        val code = validCodeFor("7F31")
        val padded = PairingCrypto.secretFor(clientKey, serverKey, "  $code ")
        assertArrayEquals(PairingCrypto.secretFor(clientKey, serverKey, code), padded)
    }

    @Test
    fun `a mistyped code fails the checksum rather than reaching the TV`() {
        val good = validCodeFor("1A2B")
        // Flip the last character; the checksum nibble no longer agrees.
        val bad = good.dropLast(1) + if (good.last() == 'C') 'D' else 'C'

        val error = assertThrows(PairingCrypto.InvalidPairingCode::class.java) {
            PairingCrypto.secretFor(clientKey, serverKey, bad)
        }
        assertEquals(PairingCrypto.CodeError.ChecksumMismatch, error.reason)
    }

    @Test
    fun `short codes are rejected`() {
        val error = assertThrows(PairingCrypto.InvalidPairingCode::class.java) {
            PairingCrypto.secretFor(clientKey, serverKey, "12345")
        }
        assertEquals(PairingCrypto.CodeError.WrongLength, error.reason)
    }

    @Test
    fun `non hexadecimal codes are rejected`() {
        val error = assertThrows(PairingCrypto.InvalidPairingCode::class.java) {
            PairingCrypto.secretFor(clientKey, serverKey, "12345Z")
        }
        assertEquals(PairingCrypto.CodeError.NotHexadecimal, error.reason)
    }

    // ---------------------------------------------------------- encoding

    @Test
    fun `unsigned encoding strips the twos complement sign byte`() {
        // The case that silently breaks pairing: an RSA modulus always has its
        // top bit set, so toByteArray() prepends 0x00 and the digest changes.
        val n = BigInteger(1, byteArrayOf(0xFF.toByte(), 0x01))
        assertArrayEquals(byteArrayOf(0x00, 0xFF.toByte(), 0x01), n.toByteArray())
        assertArrayEquals(byteArrayOf(0xFF.toByte(), 0x01), n.toUnsignedBytes())
    }

    @Test
    fun `unsigned encoding leaves values without the high bit alone`() {
        val n = BigInteger(1, byteArrayOf(0x7F, 0x01))
        assertArrayEquals(byteArrayOf(0x7F, 0x01), n.toUnsignedBytes())
    }

    @Test
    fun `unsigned encoding of a real 2048 bit modulus is exactly 256 bytes`() {
        val modulus = BigInteger(1, ByteArray(256) { 0xAB.toByte() })
        assertEquals(257, modulus.toByteArray().size)
        assertEquals(256, modulus.toUnsignedBytes().size)
    }

    @Test
    fun `unsigned encoding of zero is a single byte`() {
        assertArrayEquals(byteArrayOf(0), BigInteger.ZERO.toUnsignedBytes())
    }

    @Test
    fun `hex parsing round trips`() {
        assertArrayEquals(byteArrayOf(0x0A, 0xFF.toByte()), "0aff".hexToBytes())
        assertTrue("0aff" == byteArrayOf(0x0A, 0xFF.toByte()).toHex())
    }
}
