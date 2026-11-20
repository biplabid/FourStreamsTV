package com.streamcast.mobile.atv

import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * The slice of DER needed to write one self-signed X.509 certificate.
 *
 * ## Why this exists
 * The Android TV Remote protocol identifies a client by a certificate it
 * generates for itself, which means the app has to mint an X.509 certificate at
 * runtime. The platform has no public API for that - `java.security` reads
 * certificates but will not build one - and the usual answer, BouncyCastle's
 * `bcpkix`, brings roughly 8 MB of provider with it for a single signature over
 * a structure that is about sixty bytes of scaffolding.
 *
 * DER is just tag-length-value, and every field in a certificate is known here
 * in advance, so it is written out directly. The result is checked by parsing
 * it back with the platform's own CertificateFactory and verifying the
 * signature - if this encoder were wrong, that would fail.
 */
internal object Der {

    // Universal tags
    private const val TAG_INTEGER = 0x02
    private const val TAG_BIT_STRING = 0x03
    private const val TAG_NULL = 0x05
    private const val TAG_OID = 0x06
    private const val TAG_UTF8_STRING = 0x0C
    private const val TAG_UTC_TIME = 0x17
    private const val TAG_SEQUENCE = 0x30
    private const val TAG_SET = 0x31

    /** Wraps [content] in a tag and a DER length. */
    fun tlv(tag: Int, content: ByteArray): ByteArray {
        val out = ByteArrayOutputStream(content.size + 6)
        out.write(tag)
        writeLength(out, content.size)
        out.write(content)
        return out.toByteArray()
    }

    private fun writeLength(out: ByteArrayOutputStream, length: Int) {
        if (length < 0x80) {
            // Short form: the length is the byte.
            out.write(length)
            return
        }
        // Long form: one byte saying how many length bytes follow, then the
        // length itself, big-endian with no leading zeros.
        val bytes = BigInteger.valueOf(length.toLong()).toByteArray()
            .dropWhile { it == 0.toByte() }
            .toByteArray()
        out.write(0x80 or bytes.size)
        out.write(bytes)
    }

    fun sequence(vararg parts: ByteArray): ByteArray = tlv(TAG_SEQUENCE, concat(*parts))

    fun set(vararg parts: ByteArray): ByteArray = tlv(TAG_SET, concat(*parts))

    fun integer(value: BigInteger): ByteArray = tlv(TAG_INTEGER, value.toByteArray())

    fun integer(value: Int): ByteArray = integer(BigInteger.valueOf(value.toLong()))

    fun nullValue(): ByteArray = byteArrayOf(TAG_NULL.toByte(), 0x00)

    fun utf8String(value: String): ByteArray =
        tlv(TAG_UTF8_STRING, value.toByteArray(Charsets.UTF_8))

    /**
     * A BIT STRING whose content is a whole number of bytes, so the leading
     * "unused bits" octet is always zero.
     */
    fun bitString(value: ByteArray): ByteArray =
        tlv(TAG_BIT_STRING, byteArrayOf(0x00) + value)

    /**
     * UTCTime, as `YYMMDDHHMMSSZ`.
     *
     * Only valid for years 1950-2049; X.509 requires GeneralizedTime outside
     * that window. Certificates here are minted now and expire well inside it,
     * which [requireUtcTimeRange] enforces rather than silently emitting a
     * malformed date.
     */
    fun utcTime(date: Date): ByteArray {
        requireUtcTimeRange(date)
        val format = SimpleDateFormat("yyMMddHHmmss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return tlv(TAG_UTC_TIME, format.format(date).toByteArray(Charsets.US_ASCII))
    }

    private fun requireUtcTimeRange(date: Date) {
        val year = SimpleDateFormat("yyyy", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(date).toInt()
        require(year in 1950..2049) {
            "UTCTime cannot represent $year; GeneralizedTime would be required"
        }
    }

    /** An OBJECT IDENTIFIER from its dotted form, e.g. `2.5.4.3`. */
    fun oid(dotted: String): ByteArray {
        val parts = dotted.split('.').map { it.toLong() }
        require(parts.size >= 2) { "an OID needs at least two arcs: $dotted" }

        val out = ByteArrayOutputStream()
        // The first two arcs share one byte.
        out.write((parts[0] * 40 + parts[1]).toInt())
        for (i in 2 until parts.size) {
            writeBase128(out, parts[i])
        }
        return tlv(TAG_OID, out.toByteArray())
    }

    private fun writeBase128(out: ByteArrayOutputStream, value: Long) {
        if (value < 0x80) {
            out.write(value.toInt())
            return
        }
        // Seven bits per byte, most significant first, continuation bit set on
        // every byte but the last.
        val chunks = ArrayDeque<Int>()
        var v = value
        while (v > 0) {
            chunks.addFirst((v and 0x7F).toInt())
            v = v ushr 7
        }
        for (i in chunks.indices) {
            val last = i == chunks.size - 1
            out.write(if (last) chunks[i] else chunks[i] or 0x80)
        }
    }

    /** A context-specific constructed tag, as used by `[0] EXPLICIT`. */
    fun explicit(tagNumber: Int, content: ByteArray): ByteArray =
        tlv(0xA0 or tagNumber, content)

    fun concat(vararg parts: ByteArray): ByteArray {
        val out = ByteArrayOutputStream(parts.sumOf { it.size })
        parts.forEach(out::write)
        return out.toByteArray()
    }

    // Object identifiers used in the certificate
    val OID_SHA256_WITH_RSA: ByteArray get() = oid("1.2.840.113549.1.1.11")
    val OID_COMMON_NAME: ByteArray get() = oid("2.5.4.3")
    val OID_ORGANISATION: ByteArray get() = oid("2.5.4.10")

    /** `AlgorithmIdentifier` for sha256WithRSAEncryption, with its NULL params. */
    fun sha256WithRsaAlgorithm(): ByteArray = sequence(OID_SHA256_WITH_RSA, nullValue())

    /** A Name holding a single CN, plus an O so the TV has something to show. */
    fun distinguishedName(commonName: String, organisation: String): ByteArray = sequence(
        set(sequence(OID_COMMON_NAME, utf8String(commonName))),
        set(sequence(OID_ORGANISATION, utf8String(organisation)))
    )
}
