package com.streamcast.mobile.atv

import android.content.Context
import android.os.Build
import android.util.Log
import com.streamcast.shared.atv.PairingCrypto
import java.io.File
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPublicKey
import java.util.Calendar
import java.util.Date
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * The client identity this app presents to an Android TV, and the TLS plumbing
 * built on it.
 *
 * ## How the TV recognises us
 * There is no password and no token. The app generates one RSA key pair and a
 * self-signed certificate the first time it is needed, and that certificate
 * *is* the credential: pairing tells the TV to remember it, and every later
 * connection is authenticated by presenting the same certificate again. Lose
 * the key and the TV has to be paired again; copy it elsewhere and that copy
 * can drive the TV. It therefore lives in the app's private storage and is
 * never sent anywhere except as part of a TLS handshake.
 *
 * The key is generated in software rather than in the Android keystore because
 * `KeyManagerFactory` needs a `PrivateKey` it can hand to the TLS stack, and
 * hardware-backed keys are exposed through a different path that several
 * Android TV firmwares reject mid-handshake. The file sits inside the app
 * sandbox, which is the same protection the rest of the app's data gets.
 */
class AtvCredentials private constructor(
    val keyPair: KeyPair,
    val certificate: X509Certificate
) {

    /** Our own public key, as the pairing hash needs it. */
    val publicNumbers: PairingCrypto.RsaPublicNumbers
        get() {
            val rsa = certificate.publicKey as RSAPublicKey
            return PairingCrypto.RsaPublicNumbers(rsa.modulus, rsa.publicExponent)
        }

    /**
     * An SSL context presenting this certificate to the TV.
     *
     * @param pinnedServerCertificate the TV's certificate as recorded at
     *        pairing time. When present the handshake only succeeds against
     *        that exact certificate; when null the first certificate seen is
     *        accepted and should be stored by the caller.
     */
    fun sslContext(pinnedServerCertificate: ByteArray?): Pair<SSLContext, CapturingTrustManager> {
        val keyStore = KeyStore.getInstance(KEYSTORE_TYPE).apply {
            load(null, null)
            setKeyEntry(ALIAS, keyPair.private, PASSWORD, arrayOf(certificate))
        }
        val keyManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            .apply { init(keyStore, PASSWORD) }
            .keyManagers

        val trustManager = CapturingTrustManager(pinnedServerCertificate)
        val context = SSLContext.getInstance("TLS").apply {
            init(keyManagers, arrayOf<TrustManager>(trustManager), SecureRandom())
        }
        return context to trustManager
    }

    companion object {
        private const val TAG = "AtvCredentials"
        private const val KEYSTORE_TYPE = "PKCS12"
        private const val KEYSTORE_FILE = "atv_client_identity.p12"
        private const val ALIAS = "atv-client"
        private val PASSWORD = "atv-client".toCharArray()

        private const val KEY_SIZE = 2048
        private const val VALID_YEARS = 20

        @Volatile
        private var cached: AtvCredentials? = null

        /**
         * Loads the stored identity, generating one on first use.
         *
         * Generating a 2048-bit RSA key takes a noticeable fraction of a second,
         * so this must not run on the main thread.
         */
        @Synchronized
        fun load(context: Context): AtvCredentials {
            cached?.let { return it }

            val file = File(context.filesDir, KEYSTORE_FILE)
            val credentials = if (file.exists()) {
                runCatching { read(file) }.getOrElse { error ->
                    // A corrupt identity is not recoverable; starting over at
                    // least lets the user pair again instead of being stuck.
                    Log.w(TAG, "Stored identity unreadable, regenerating", error)
                    file.delete()
                    generate().also { write(it, file) }
                }
            } else {
                Log.d(TAG, "No client identity yet; generating one")
                generate().also { write(it, file) }
            }

            cached = credentials
            return credentials
        }

        /** Forgets the identity, so the next connection has to pair again. */
        @Synchronized
        fun reset(context: Context) {
            File(context.filesDir, KEYSTORE_FILE).delete()
            cached = null
        }

        private fun read(file: File): AtvCredentials {
            val keyStore = KeyStore.getInstance(KEYSTORE_TYPE)
            file.inputStream().use { keyStore.load(it, PASSWORD) }

            val key = keyStore.getKey(ALIAS, PASSWORD) as PrivateKey
            val certificate = keyStore.getCertificate(ALIAS) as X509Certificate
            return AtvCredentials(KeyPair(certificate.publicKey, key), certificate)
        }

        private fun write(credentials: AtvCredentials, file: File) {
            val keyStore = KeyStore.getInstance(KEYSTORE_TYPE).apply {
                load(null, null)
                setKeyEntry(
                    ALIAS,
                    credentials.keyPair.private,
                    PASSWORD,
                    arrayOf(credentials.certificate)
                )
            }
            file.outputStream().use { keyStore.store(it, PASSWORD) }
        }

        /**
         * Mints a fresh key pair and self-signed certificate.
         *
         * The common name is what some TV firmwares show in their list of
         * paired devices, so it is the app name rather than something opaque.
         */
        fun generate(
            commonName: String = "FourStreamsTV",
            organisation: String = "FourStreamsTV"
        ): AtvCredentials {
            val keyPair = KeyPairGenerator.getInstance("RSA")
                .apply { initialize(KEY_SIZE, SecureRandom()) }
                .generateKeyPair()

            val certificate = selfSign(keyPair, commonName, organisation)
            return AtvCredentials(keyPair, certificate)
        }

        /**
         * Builds and signs an X.509 v3 certificate over [keyPair].
         *
         * `publicKey.encoded` is already a DER SubjectPublicKeyInfo, which is
         * exactly the field the certificate needs, so the only structure built
         * by hand is the surrounding TBSCertificate.
         */
        private fun selfSign(
            keyPair: KeyPair,
            commonName: String,
            organisation: String
        ): X509Certificate {
            val now = System.currentTimeMillis()
            // Backdate slightly: a TV whose clock is a little behind ours would
            // otherwise see a certificate that is not valid yet.
            val notBefore = Date(now - 24L * 60 * 60 * 1000)
            val notAfter = Calendar.getInstance().apply {
                time = Date(now)
                add(Calendar.YEAR, VALID_YEARS)
            }.time

            val serial = BigInteger(64, SecureRandom()).let {
                if (it.signum() == 0) BigInteger.ONE else it
            }
            val name = Der.distinguishedName(commonName, organisation)
            val algorithm = Der.sha256WithRsaAlgorithm()

            val tbsCertificate = Der.sequence(
                Der.explicit(0, Der.integer(2)),      // version v3
                Der.integer(serial),
                algorithm,
                name,                                  // issuer == subject
                Der.sequence(Der.utcTime(notBefore), Der.utcTime(notAfter)),
                name,
                keyPair.public.encoded                 // SubjectPublicKeyInfo
            )

            val signature = Signature.getInstance("SHA256withRSA").run {
                initSign(keyPair.private)
                update(tbsCertificate)
                sign()
            }

            val encoded = Der.sequence(
                tbsCertificate,
                algorithm,
                Der.bitString(signature)
            )

            val certificate = CertificateFactory.getInstance("X.509")
                .generateCertificate(encoded.inputStream()) as X509Certificate

            // If the hand-written DER were wrong this would throw, so failing
            // here is far better than failing opaquely inside a TLS handshake.
            certificate.verify(keyPair.public)
            certificate.checkValidity()

            return certificate
        }

        /** A short, human-comparable form of a certificate fingerprint. */
        fun fingerprintOf(certificate: X509Certificate): ByteArray =
            java.security.MessageDigest.getInstance("SHA-256").digest(certificate.encoded)
    }
}

/**
 * Trusts the TV's self-signed certificate, on the terms set at pairing time.
 *
 * An Android TV presents a certificate signed by nobody, so ordinary PKI
 * validation cannot apply - there is no authority to check it against. The
 * usual shortcut is to accept any certificate at all, which leaves every later
 * connection open to anyone able to answer on that address.
 *
 * Instead this trusts on first use: during pairing, whatever certificate the TV
 * presents is captured and stored alongside the device, and from then on only
 * that exact certificate is accepted. An impostor would have to have been
 * present at pairing time, in front of the user reading a code off the real
 * TV's screen.
 */
class CapturingTrustManager(
    private val pinnedCertificate: ByteArray?
) : X509TrustManager {

    /** The certificate the TV presented, available after the handshake. */
    @Volatile
    var seenCertificate: X509Certificate? = null
        private set

    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        // This client never acts as a server.
        throw java.security.cert.CertificateException("Client authentication is not supported")
    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        val presented = chain?.firstOrNull()
            ?: throw java.security.cert.CertificateException("The TV presented no certificate")

        seenCertificate = presented

        val pin = pinnedCertificate ?: return  // first use: record and accept
        if (!pin.contentEquals(presented.encoded)) {
            throw java.security.cert.CertificateException(
                "This device is not the TV that was paired. Its certificate has changed, " +
                    "which happens after a factory reset - or if something else is answering " +
                    "on its address."
            )
        }
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}

/** The TV's public key, read off a completed handshake for the pairing hash. */
internal fun SSLSocket.serverPublicNumbers(): PairingCrypto.RsaPublicNumbers {
    val peer = session.peerCertificates.firstOrNull() as? X509Certificate
        ?: error("The TV did not present an X.509 certificate")
    val rsa = peer.publicKey as? RSAPublicKey
        ?: error("The TV's certificate is not RSA, which this protocol requires")
    return PairingCrypto.RsaPublicNumbers(rsa.modulus, rsa.publicExponent)
}

/** Identifies this handset to the TV during the remote handshake. */
internal object ThisDevice {
    val model: String get() = Build.MODEL ?: "Android"
    val vendor: String get() = Build.MANUFACTURER ?: "Android"
}
