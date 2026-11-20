package com.streamcast.shared.atv

import java.math.BigInteger
import java.security.MessageDigest

/**
 * The proof-of-possession step of Android TV Remote v2 pairing.
 *
 * ## What the code on the TV actually proves
 * Both sides have already exchanged self-signed TLS certificates, but neither
 * has any reason to trust the other's. The six hex characters the TV displays
 * bind the two certificates together through a channel an attacker on the
 * network cannot see: the user's eyes.
 *
 * The client hashes both public keys together with the last four characters of
 * the code. The first two characters are a checksum over that hash, so a
 * mistyped code is caught locally - before anything is sent - rather than
 * burning one of the TV's pairing attempts.
 *
 * Getting the integer encoding wrong here produces a hash that is wrong in a
 * way nothing explains: pairing simply fails with STATUS_BAD_SECRET. See
 * [toUnsignedBytes].
 */
object PairingCrypto {

    const val CODE_LENGTH = 6

    /** Why a code was rejected before it ever reached the TV. */
    sealed interface CodeError {
        data object WrongLength : CodeError
        data object NotHexadecimal : CodeError
        /** The checksum nibble disagrees: almost always a typo. */
        data object ChecksumMismatch : CodeError
    }

    class InvalidPairingCode(val reason: CodeError, message: String) : Exception(message)

    /**
     * Computes the secret to send back for [code].
     *
     * @param clientPublicKey the modulus and exponent of our own certificate.
     * @param serverPublicKey the modulus and exponent of the certificate the TV
     *        presented during the TLS handshake on port 6467.
     * @param code the six hex characters shown on the TV.
     * @throws InvalidPairingCode if the code is malformed or fails its checksum.
     */
    fun secretFor(
        clientPublicKey: RsaPublicNumbers,
        serverPublicKey: RsaPublicNumbers,
        code: String
    ): ByteArray {
        val cleaned = code.trim().replace(" ", "")

        if (cleaned.length != CODE_LENGTH) {
            throw InvalidPairingCode(
                CodeError.WrongLength,
                "A pairing code is exactly $CODE_LENGTH characters; got ${cleaned.length}"
            )
        }
        if (!cleaned.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }) {
            throw InvalidPairingCode(
                CodeError.NotHexadecimal,
                "A pairing code uses only 0-9 and A-F"
            )
        }

        val digest = MessageDigest.getInstance("SHA-256").apply {
            update(clientPublicKey.modulus.toUnsignedBytes())
            update(clientPublicKey.exponent.toUnsignedBytes())
            update(serverPublicKey.modulus.toUnsignedBytes())
            update(serverPublicKey.exponent.toUnsignedBytes())
            // Only the last four characters take part; the first two are the check byte.
            update(cleaned.substring(2).hexToBytes())
        }
        val hash = digest.digest()

        val expectedCheck = cleaned.substring(0, 2).toInt(16)
        if ((hash[0].toInt() and 0xFF) != expectedCheck) {
            throw InvalidPairingCode(
                CodeError.ChecksumMismatch,
                "That code does not match the one on the TV"
            )
        }

        return hash
    }

    /** The two numbers that make up an RSA public key. */
    data class RsaPublicNumbers(val modulus: BigInteger, val exponent: BigInteger)
}

/**
 * The minimal big-endian *unsigned* encoding of a non-negative integer.
 *
 * [BigInteger.toByteArray] produces two's-complement, so any value whose top
 * bit is set gains a leading zero byte to keep it positive - which an RSA
 * modulus, being a 2048-bit number with the high bit set, essentially always
 * does. Feeding that extra byte into the hash changes the digest completely and
 * the TV rejects the secret with no useful diagnostic, so it is stripped here.
 */
internal fun BigInteger.toUnsignedBytes(): ByteArray {
    require(signum() >= 0) { "negative values have no unsigned encoding" }
    val raw = toByteArray()
    return if (raw.size > 1 && raw[0] == 0.toByte()) raw.copyOfRange(1, raw.size) else raw
}

/** Parses an even-length hex string. */
internal fun String.hexToBytes(): ByteArray {
    require(length % 2 == 0) { "hex string must have an even length" }
    return ByteArray(length / 2) { i ->
        ((Character.digit(this[i * 2], 16) shl 4) or Character.digit(this[i * 2 + 1], 16)).toByte()
    }
}

/** Lower-case hex, used for certificate fingerprints in logs and storage. */
internal fun ByteArray.toHex(): String =
    joinToString("") { "%02x".format(it) }
