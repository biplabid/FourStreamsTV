package com.streamcast.shared.atv

import java.io.ByteArrayOutputStream

/**
 * A minimal protobuf wire-format codec, just enough for the Android TV Remote
 * v2 protocol.
 *
 * ## Why not the protobuf library
 * The protocol uses two small schemas (pairingmessage.proto and
 * remotemessage.proto) with perhaps a dozen message types between them. Pulling
 * in protobuf-javalite would mean the protoc Gradle plugin, a code-generation
 * step and a toolchain download at build time - all to encode a handful of
 * varints and length-delimited fields. The wire format itself is tiny and
 * completely specified, so it is encoded here directly and covered by tests.
 *
 * Only the two wire types the protocol actually uses are supported: VARINT (0)
 * and LEN (2). Fixed32/fixed64 and the deprecated group types are skipped on
 * read and never written, because neither schema contains a field of those
 * types.
 */
internal const val WIRE_VARINT = 0
internal const val WIRE_FIXED64 = 1
internal const val WIRE_LEN = 2
internal const val WIRE_START_GROUP = 3
internal const val WIRE_END_GROUP = 4
internal const val WIRE_FIXED32 = 5

/** Thrown when a byte sequence is not valid protobuf, or ends mid-field. */
class ProtoParseException(message: String) : Exception(message)

// ---------------------------------------------------------------- writing

/**
 * Builds one protobuf message.
 *
 * Follows proto3 semantics: scalar fields holding their default value (0,
 * false, empty string, empty bytes) are omitted entirely, because on the wire
 * an absent scalar and a zero scalar are the same thing. Sub-messages are the
 * exception - [message] always writes the tag, even for an empty body, since
 * for a message field "present but empty" and "absent" are genuinely different
 * and the protocol depends on that difference (PairingConfigurationAck carries
 * no fields at all, and the peer still has to see it).
 */
class ProtoWriter {

    private val out = ByteArrayOutputStream()

    fun varint(field: Int, value: Long): ProtoWriter = apply {
        if (value == 0L) return@apply
        tag(field, WIRE_VARINT)
        writeVarint(value)
    }

    fun varint(field: Int, value: Int): ProtoWriter = varint(field, value.toLong())

    fun bool(field: Int, value: Boolean): ProtoWriter = apply {
        if (!value) return@apply
        tag(field, WIRE_VARINT)
        writeVarint(1L)
    }

    fun string(field: Int, value: String): ProtoWriter = apply {
        if (value.isEmpty()) return@apply
        bytesAlways(field, value.toByteArray(Charsets.UTF_8))
    }

    fun bytes(field: Int, value: ByteArray): ProtoWriter = apply {
        if (value.isEmpty()) return@apply
        bytesAlways(field, value)
    }

    /** Writes a nested message, including when it is empty. */
    fun message(field: Int, build: ProtoWriter.() -> Unit): ProtoWriter = apply {
        val nested = ProtoWriter().apply(build).toByteArray()
        bytesAlways(field, nested)
    }

    /** Writes an already-encoded nested message, including when it is empty. */
    fun messageBytes(field: Int, body: ByteArray): ProtoWriter = apply {
        bytesAlways(field, body)
    }

    private fun bytesAlways(field: Int, value: ByteArray) {
        tag(field, WIRE_LEN)
        writeVarint(value.size.toLong())
        out.write(value)
    }

    private fun tag(field: Int, wireType: Int) {
        require(field > 0) { "field number must be positive, was $field" }
        writeVarint((field.toLong() shl 3) or wireType.toLong())
    }

    private fun writeVarint(value: Long) {
        var v = value
        while (true) {
            // Only the low seven bits travel in each byte; the high bit is the
            // "more bytes follow" flag.
            val b = (v and 0x7F).toInt()
            v = v ushr 7
            if (v == 0L) {
                out.write(b)
                return
            }
            out.write(b or 0x80)
        }
    }

    fun toByteArray(): ByteArray = out.toByteArray()
}

/** Encodes one message and returns it. */
fun protoMessage(build: ProtoWriter.() -> Unit): ByteArray =
    ProtoWriter().apply(build).toByteArray()

// ---------------------------------------------------------------- reading

/** One decoded field value: either a varint or a length-delimited run of bytes. */
sealed interface ProtoValue {
    @JvmInline
    value class Num(val value: Long) : ProtoValue

    @JvmInline
    value class Bytes(val value: ByteArray) : ProtoValue
}

/**
 * A decoded protobuf message, held as field number to the values seen for it.
 *
 * Values are kept in a list because protobuf allows a field to repeat, and the
 * last one wins for non-repeated fields - which is what the scalar accessors
 * below do.
 */
class ProtoMessage(val fields: Map<Int, List<ProtoValue>>) {

    fun has(field: Int): Boolean = fields.containsKey(field)

    fun varint(field: Int): Long? =
        (fields[field]?.lastOrNull() as? ProtoValue.Num)?.value

    fun int(field: Int): Int? = varint(field)?.toInt()

    fun bool(field: Int): Boolean = varint(field) == 1L

    fun bytes(field: Int): ByteArray? =
        (fields[field]?.lastOrNull() as? ProtoValue.Bytes)?.value

    fun string(field: Int): String? = bytes(field)?.toString(Charsets.UTF_8)

    fun message(field: Int): ProtoMessage? = bytes(field)?.let { parseProto(it) }

    fun messages(field: Int): List<ProtoMessage> =
        fields[field].orEmpty()
            .filterIsInstance<ProtoValue.Bytes>()
            .map { parseProto(it.value) }

    /** The field numbers present, which is how a oneof-style union is dispatched. */
    val presentFields: Set<Int> get() = fields.keys

    override fun toString(): String = "ProtoMessage(fields=${fields.keys.sorted()})"
}

/**
 * Decodes one protobuf message.
 *
 * Unknown fields are kept rather than dropped, so a device sending a field this
 * build does not model cannot derail parsing.
 *
 * @throws ProtoParseException if the bytes are malformed or truncated.
 */
fun parseProto(bytes: ByteArray): ProtoMessage {
    val fields = LinkedHashMap<Int, MutableList<ProtoValue>>()
    var pos = 0

    fun readVarint(): Long {
        var result = 0L
        var shift = 0
        while (true) {
            if (pos >= bytes.size) throw ProtoParseException("truncated varint at $pos")
            if (shift > 63) throw ProtoParseException("varint longer than 10 bytes at $pos")
            val b = bytes[pos++].toInt() and 0xFF
            result = result or ((b and 0x7F).toLong() shl shift)
            if (b and 0x80 == 0) return result
            shift += 7
        }
    }

    while (pos < bytes.size) {
        val tag = readVarint()
        val field = (tag ushr 3).toInt()
        val wireType = (tag and 0x7L).toInt()
        if (field == 0) throw ProtoParseException("field number 0 is not valid")

        val value: ProtoValue = when (wireType) {
            WIRE_VARINT -> ProtoValue.Num(readVarint())

            WIRE_LEN -> {
                val len = readVarint()
                if (len < 0 || len > Int.MAX_VALUE) {
                    throw ProtoParseException("absurd length $len at $pos")
                }
                val size = len.toInt()
                if (pos + size > bytes.size) {
                    throw ProtoParseException("truncated length-delimited field at $pos")
                }
                val slice = bytes.copyOfRange(pos, pos + size)
                pos += size
                ProtoValue.Bytes(slice)
            }

            // Not used by either schema, but skipped cleanly so an unexpected
            // field from a future device does not break the whole message.
            WIRE_FIXED64 -> {
                if (pos + 8 > bytes.size) throw ProtoParseException("truncated fixed64")
                pos += 8
                continue
            }

            WIRE_FIXED32 -> {
                if (pos + 4 > bytes.size) throw ProtoParseException("truncated fixed32")
                pos += 4
                continue
            }

            WIRE_START_GROUP, WIRE_END_GROUP ->
                throw ProtoParseException("group wire types are not supported (field $field)")

            else -> throw ProtoParseException("unknown wire type $wireType for field $field")
        }

        fields.getOrPut(field) { mutableListOf() }.add(value)
    }

    return ProtoMessage(fields)
}

// ------------------------------------------------------------ stream framing

/**
 * Both sockets carry a stream of messages, each prefixed by its length as a
 * varint. A single TCP read can deliver part of a message, several messages, or
 * a split in the middle of the length prefix itself, so frames are accumulated
 * here and only handed on once complete.
 */
class VarintFrameBuffer {

    private var buffer = ByteArray(0)

    /** Appends freshly read bytes and returns every complete frame now available. */
    fun append(data: ByteArray, length: Int = data.size): List<ByteArray> {
        buffer = buffer + data.copyOfRange(0, length)
        val frames = mutableListOf<ByteArray>()

        while (true) {
            val header = readLengthPrefix(buffer) ?: break
            val (payloadLength, headerSize) = header
            val total = headerSize + payloadLength
            if (buffer.size < total) break
            frames += buffer.copyOfRange(headerSize, total)
            buffer = buffer.copyOfRange(total, buffer.size)
        }
        return frames
    }

    /** Bytes held back because they do not yet form a whole frame. */
    val pending: Int get() = buffer.size

    fun clear() {
        buffer = ByteArray(0)
    }

    private fun readLengthPrefix(bytes: ByteArray): Pair<Int, Int>? {
        var result = 0L
        var shift = 0
        var i = 0
        while (true) {
            if (i >= bytes.size) return null          // prefix not fully arrived
            if (shift > 35) throw ProtoParseException("message length prefix too long")
            val b = bytes[i].toInt() and 0xFF
            result = result or ((b and 0x7F).toLong() shl shift)
            i++
            if (b and 0x80 == 0) break
            shift += 7
        }
        if (result < 0 || result > MAX_FRAME_BYTES) {
            throw ProtoParseException("implausible frame length $result")
        }
        return result.toInt() to i
    }

    private companion object {
        /**
         * A sane ceiling. The largest message the protocol sends is a voice
         * payload chunk at 20 KB; anything near a megabyte means the stream has
         * desynchronised and should fail loudly rather than allocate.
         */
        const val MAX_FRAME_BYTES = 1L * 1024 * 1024
    }
}

/** Prefixes an encoded message with its length, ready to write to the socket. */
fun frameMessage(payload: ByteArray): ByteArray {
    val header = ByteArrayOutputStream(5)
    var v = payload.size.toLong()
    while (true) {
        val b = (v and 0x7F).toInt()
        v = v ushr 7
        if (v == 0L) {
            header.write(b)
            break
        }
        header.write(b or 0x80)
    }
    return header.toByteArray() + payload
}
