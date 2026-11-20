package com.streamcast.shared.atv

/**
 * pairingmessage.proto, encoded by hand.
 *
 * Field numbers are the contract with the TV and must match the schema exactly;
 * they are named constants here rather than magic numbers at each call site so
 * that a mistake is visible in one place.
 *
 * Schema: https://github.com/louis49/androidtv-remote/blob/main/src/pairing/pairingmessage.proto
 */
object PairingProto {

    // ---- PairingMessage ----
    const val PROTOCOL_VERSION = 1
    const val STATUS = 2
    const val REQUEST_CASE = 3
    const val PAIRING_REQUEST = 10
    const val PAIRING_REQUEST_ACK = 11
    const val PAIRING_OPTION = 20
    const val PAIRING_CONFIGURATION = 30
    const val PAIRING_CONFIGURATION_ACK = 31
    const val PAIRING_SECRET = 40
    const val PAIRING_SECRET_ACK = 41

    // ---- Status ----
    const val STATUS_OK = 200
    const val STATUS_ERROR = 400
    const val STATUS_BAD_CONFIGURATION = 401
    const val STATUS_BAD_SECRET = 402

    // ---- RoleType ----
    const val ROLE_TYPE_INPUT = 1

    // ---- PairingEncoding.EncodingType ----
    const val ENCODING_TYPE_HEXADECIMAL = 3

    /** The TV shows six hexadecimal characters. */
    const val SYMBOL_LENGTH = 6

    /**
     * The service name the official client uses. The TV logs it and some
     * firmware refuses an unknown value, so it is not ours to choose.
     */
    const val SERVICE_NAME = "atvremote"

    // ---- PairingRequest ----
    private const val REQ_SERVICE_NAME = 1
    private const val REQ_CLIENT_NAME = 2

    // ---- PairingRequestAck ----
    private const val ACK_SERVER_NAME = 1

    // ---- PairingEncoding ----
    private const val ENC_TYPE = 1
    private const val ENC_SYMBOL_LENGTH = 2

    // ---- PairingOption ----
    private const val OPT_INPUT_ENCODINGS = 1
    private const val OPT_PREFERRED_ROLE = 3

    // ---- PairingConfiguration ----
    private const val CFG_ENCODING = 1
    private const val CFG_CLIENT_ROLE = 2

    // ---- PairingSecret ----
    private const val SECRET_VALUE = 1

    /** Every message we send carries the version and an OK status. */
    private fun ProtoWriter.envelope() {
        varint(PROTOCOL_VERSION, 2)
        varint(STATUS, STATUS_OK)
    }

    /** Step 1: announce ourselves. [clientName] is what the TV shows the user. */
    fun pairingRequest(clientName: String): ByteArray = protoMessage {
        envelope()
        message(PAIRING_REQUEST) {
            string(REQ_SERVICE_NAME, SERVICE_NAME)
            string(REQ_CLIENT_NAME, clientName)
        }
    }

    /** Step 2: say we can accept a six-character hex code typed by a human. */
    fun pairingOption(): ByteArray = protoMessage {
        envelope()
        message(PAIRING_OPTION) {
            message(OPT_INPUT_ENCODINGS) {
                varint(ENC_TYPE, ENCODING_TYPE_HEXADECIMAL)
                varint(ENC_SYMBOL_LENGTH, SYMBOL_LENGTH)
            }
            varint(OPT_PREFERRED_ROLE, ROLE_TYPE_INPUT)
        }
    }

    /** Step 3: commit to that encoding. The TV displays the code after this. */
    fun pairingConfiguration(): ByteArray = protoMessage {
        envelope()
        message(PAIRING_CONFIGURATION) {
            message(CFG_ENCODING) {
                varint(ENC_TYPE, ENCODING_TYPE_HEXADECIMAL)
                varint(ENC_SYMBOL_LENGTH, SYMBOL_LENGTH)
            }
            varint(CFG_CLIENT_ROLE, ROLE_TYPE_INPUT)
        }
    }

    /** Step 4: prove we saw the code. See [PairingCrypto.secretFor]. */
    fun pairingSecret(secret: ByteArray): ByteArray = protoMessage {
        envelope()
        message(PAIRING_SECRET) {
            bytes(SECRET_VALUE, secret)
        }
    }

    /** What the TV just said. */
    sealed interface Incoming {
        data class RequestAck(val serverName: String?) : Incoming
        data object Option : Incoming
        data object ConfigurationAck : Incoming
        data object SecretAck : Incoming
        data class Failed(val status: Int) : Incoming
        data class Unknown(val fields: Set<Int>) : Incoming
    }

    fun parse(frame: ByteArray): Incoming {
        val msg = parseProto(frame)

        // A non-OK status outranks whatever payload came with it.
        val status = msg.int(STATUS) ?: STATUS_OK
        if (status != STATUS_OK) return Incoming.Failed(status)

        return when {
            msg.has(PAIRING_REQUEST_ACK) ->
                Incoming.RequestAck(msg.message(PAIRING_REQUEST_ACK)?.string(ACK_SERVER_NAME))

            msg.has(PAIRING_OPTION) -> Incoming.Option
            msg.has(PAIRING_CONFIGURATION_ACK) -> Incoming.ConfigurationAck
            msg.has(PAIRING_SECRET_ACK) -> Incoming.SecretAck
            else -> Incoming.Unknown(msg.presentFields)
        }
    }

    /** A human-readable reason for a non-OK status. */
    fun describeStatus(status: Int): String = when (status) {
        STATUS_BAD_SECRET -> "The TV rejected that pairing code"
        STATUS_BAD_CONFIGURATION -> "The TV rejected the pairing configuration"
        STATUS_ERROR -> "The TV reported a pairing error"
        else -> "The TV reported pairing status $status"
    }
}
