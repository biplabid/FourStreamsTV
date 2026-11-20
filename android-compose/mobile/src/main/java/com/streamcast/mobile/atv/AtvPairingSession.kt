package com.streamcast.mobile.atv

import android.util.Log
import com.streamcast.shared.atv.PairingCrypto
import com.streamcast.shared.atv.PairingProto
import com.streamcast.shared.atv.VarintFrameBuffer
import com.streamcast.shared.atv.frameMessage
import java.io.Closeable
import java.net.InetSocketAddress
import java.net.Socket
import java.security.cert.X509Certificate
import javax.net.ssl.SSLSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The pairing handshake, on port 6467.
 *
 * ## Shape of the exchange
 * ```
 * connect TLS (we present our certificate, the TV presents its own)
 *   -> PairingRequest        "I am FourStreamsTV"
 *   <- PairingRequestAck
 *   -> PairingOption         "I can take six hex characters typed by a person"
 *   <- PairingOption
 *   -> PairingConfiguration  "do that then"
 *   <- PairingConfigurationAck       ... the TV now shows a code
 *      [ the user reads it and types it in ]
 *   -> PairingSecret         hash binding both certificates to that code
 *   <- PairingSecretAck      the TV has stored our certificate
 * ```
 *
 * Every step up to the code is answered by the device itself, so the only part
 * that waits on a human is between [start] and [submitCode]. The socket has to
 * stay open across that wait: the secret is bound to this TLS session's
 * certificates, and reconnecting would invalidate the code on screen.
 */
class AtvPairingSession(
    private val credentials: AtvCredentials,
    private val host: String,
    private val clientName: String
) : Closeable {

    private var socket: SSLSocket? = null
    private var trustManager: CapturingTrustManager? = null
    private val frames = VarintFrameBuffer()
    private val readChunk = ByteArray(8 * 1024)

    /** The TV's certificate, captured during the handshake, for pinning later. */
    var serverCertificate: X509Certificate? = null
        private set

    /** What the TV called itself in its acknowledgement, if anything. */
    var serverName: String? = null
        private set

    /**
     * Connects and runs the handshake up to the point where the TV displays a
     * code.
     *
     * @throws AtvPairingException if the device refuses or is unreachable.
     */
    suspend fun start() = withContext(Dispatchers.IO) {
        val (context, trust) = credentials.sslContext(pinnedServerCertificate = null)
        trustManager = trust

        val raw = Socket()
        try {
            raw.connect(InetSocketAddress(host, PAIRING_PORT), CONNECT_TIMEOUT_MS)
        } catch (e: Exception) {
            throw AtvPairingException(
                "Could not reach the TV on $host. It needs to be on and on the same Wi-Fi.",
                e
            )
        }

        val tls = try {
            (context.socketFactory.createSocket(raw, host, PAIRING_PORT, true) as SSLSocket).apply {
                soTimeout = READ_TIMEOUT_MS
                startHandshake()
            }
        } catch (e: Exception) {
            runCatching { raw.close() }
            throw AtvPairingException("The TV refused the secure connection", e)
        }

        socket = tls
        serverCertificate = trust.seenCertificate
        Log.d(TAG, "TLS up to $host:$PAIRING_PORT")

        send(PairingProto.pairingRequest(clientName))
        when (val reply = expect()) {
            is PairingProto.Incoming.RequestAck -> serverName = reply.serverName
            else -> throw unexpected("pairing request", reply)
        }

        send(PairingProto.pairingOption())
        expectOrThrow<PairingProto.Incoming.Option>("pairing options")

        send(PairingProto.pairingConfiguration())
        expectOrThrow<PairingProto.Incoming.ConfigurationAck>("pairing configuration")

        Log.d(TAG, "TV is displaying a pairing code")
    }

    /**
     * Sends the proof for the code the user typed.
     *
     * @return the TV's certificate, to be pinned against this device.
     * @throws PairingCrypto.InvalidPairingCode if the code is malformed or
     *         mistyped - caught locally, so the TV never sees it.
     * @throws AtvPairingException if the TV rejects the secret.
     */
    suspend fun submitCode(code: String): X509Certificate = withContext(Dispatchers.IO) {
        val tls = socket ?: throw AtvPairingException("Pairing is not in progress")

        val secret = PairingCrypto.secretFor(
            clientPublicKey = credentials.publicNumbers,
            serverPublicKey = tls.serverPublicNumbers(),
            code = code
        )

        send(PairingProto.pairingSecret(secret))
        expectOrThrow<PairingProto.Incoming.SecretAck>("pairing secret")

        Log.d(TAG, "Paired with $host")
        serverCertificate ?: throw AtvPairingException("The TV presented no certificate to remember")
    }

    override fun close() {
        runCatching { socket?.close() }
        socket = null
        frames.clear()
    }

    // ------------------------------------------------------------ plumbing

    private fun send(message: ByteArray) {
        val tls = socket ?: throw AtvPairingException("Pairing connection is closed")
        try {
            tls.outputStream.apply {
                write(frameMessage(message))
                flush()
            }
        } catch (e: Exception) {
            throw AtvPairingException("Lost the connection to the TV while pairing", e)
        }
    }

    /** Blocks until one complete message arrives. */
    private fun expect(): PairingProto.Incoming {
        val tls = socket ?: throw AtvPairingException("Pairing connection is closed")

        while (true) {
            pending.removeFirstOrNull()?.let { return it }

            val read = try {
                tls.inputStream.read(readChunk)
            } catch (e: Exception) {
                throw AtvPairingException("The TV stopped responding while pairing", e)
            }
            if (read < 0) {
                throw AtvPairingException(
                    "The TV closed the connection. If it has paired with this app before, " +
                        "forget the device on the TV and try again."
                )
            }

            frames.append(readChunk, read).forEach { pending.add(PairingProto.parse(it)) }
        }
    }

    private val pending = ArrayDeque<PairingProto.Incoming>()

    private inline fun <reified T : PairingProto.Incoming> expectOrThrow(step: String): T {
        val reply = expect()
        return reply as? T ?: throw unexpected(step, reply)
    }

    private fun unexpected(step: String, reply: PairingProto.Incoming): AtvPairingException =
        when (reply) {
            is PairingProto.Incoming.Failed ->
                AtvPairingException(PairingProto.describeStatus(reply.status))

            else ->
                AtvPairingException("The TV sent an unexpected reply during $step: $reply")
        }

    companion object {
        private const val TAG = "AtvPairing"

        /** The Android TV Remote Service listens for pairing here. */
        const val PAIRING_PORT = 6467

        private const val CONNECT_TIMEOUT_MS = 8_000
        private const val READ_TIMEOUT_MS = 15_000
    }
}

class AtvPairingException(message: String, cause: Throwable? = null) : Exception(message, cause)
