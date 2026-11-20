package com.streamcast.shared.atv

/**
 * remotemessage.proto, encoded by hand.
 *
 * Schema: https://github.com/louis49/androidtv-remote/blob/main/src/remote/remotemessage.proto
 */
object RemoteProto {

    // ---- RemoteMessage: the union of everything either side can send ----
    const val REMOTE_CONFIGURE = 1
    const val REMOTE_SET_ACTIVE = 2
    const val REMOTE_ERROR = 3
    const val REMOTE_PING_REQUEST = 8
    const val REMOTE_PING_RESPONSE = 9
    const val REMOTE_KEY_INJECT = 10
    const val REMOTE_IME_KEY_INJECT = 20
    const val REMOTE_IME_BATCH_EDIT = 21
    const val REMOTE_IME_SHOW_REQUEST = 22
    const val REMOTE_VOICE_BEGIN = 30
    const val REMOTE_VOICE_PAYLOAD = 31
    const val REMOTE_VOICE_END = 32
    const val REMOTE_START = 40
    const val REMOTE_SET_VOLUME_LEVEL = 50
    const val REMOTE_APP_LINK_LAUNCH_REQUEST = 90

    // ---- RemoteConfigure ----
    private const val CFG_CODE1 = 1
    private const val CFG_DEVICE_INFO = 2

    // ---- RemoteDeviceInfo ----
    private const val DEV_MODEL = 1
    private const val DEV_VENDOR = 2
    private const val DEV_UNKNOWN1 = 3
    private const val DEV_UNKNOWN2 = 4
    private const val DEV_PACKAGE_NAME = 5
    private const val DEV_APP_VERSION = 6

    // ---- RemoteSetActive ----
    private const val ACTIVE_VALUE = 1

    // ---- RemotePingRequest / Response ----
    private const val PING_VAL1 = 1

    // ---- RemoteKeyInject ----
    private const val KEY_CODE = 1
    private const val KEY_DIRECTION = 2

    // ---- RemoteStart ----
    private const val START_STARTED = 1

    // ---- RemoteSetVolumeLevel ----
    private const val VOL_PLAYER_MODEL = 3
    private const val VOL_MAX = 6
    private const val VOL_LEVEL = 7
    private const val VOL_MUTED = 8

    // ---- RemoteAppLinkLaunchRequest ----
    private const val APP_LINK = 1

    // ---- RemoteImeKeyInject ----
    private const val IME_APP_INFO = 1
    private const val IME_TEXT_FIELD_STATUS = 2

    // ---- RemoteAppInfo ----
    private const val APP_COUNTER = 1
    private const val APP_PACKAGE = 12

    // ---- RemoteTextFieldStatus ----
    private const val TFS_COUNTER_FIELD = 1
    private const val TFS_VALUE = 2
    private const val TFS_LABEL = 6

    // ---- RemoteImeShowRequest ----
    private const val SHOW_TEXT_FIELD_STATUS = 2

    // ---- RemoteImeBatchEdit ----
    private const val BATCH_IME_COUNTER = 1
    private const val BATCH_FIELD_COUNTER = 2
    private const val BATCH_EDIT_INFO = 3

    // ---- RemoteEditInfo ----
    private const val EDIT_INSERT = 1
    private const val EDIT_TEXT_FIELD_STATUS = 2

    // ---- RemoteImeObject ----
    private const val OBJ_START = 1
    private const val OBJ_END = 2
    private const val OBJ_VALUE = 3

    // ---- RemoteVoice* ----
    private const val VOICE_SESSION_ID = 1
    private const val VOICE_SAMPLES = 2

    /**
     * Capability bits exchanged in RemoteConfigure.code1.
     *
     * The TV sends what it supports; we send back the intersection with what we
     * want. Asking for a bit the device does not have gets the connection
     * closed, which is why the handshake has to be a negotiation rather than a
     * fixed constant.
     */
    object Feature {
        const val PING = 1 shl 0
        const val KEY = 1 shl 1
        const val IME = 1 shl 2
        const val VOICE = 1 shl 3
        const val UNKNOWN_1 = 1 shl 4
        const val POWER = 1 shl 5
        const val VOLUME = 1 shl 6
        const val APP_LINK = 1 shl 9

        /** Everything this client knows how to drive, bar voice. */
        const val REQUESTED_BASE = PING or KEY or POWER or VOLUME or APP_LINK or IME

        /**
         * With voice added.
         *
         * Negotiating VOICE changes what KEYCODE_SEARCH does: instead of
         * opening the TV's own search screen, the TV answers with
         * RemoteVoiceBegin and waits for audio from us. That is the behaviour
         * the hold-to-talk button wants and the wrong behaviour for a plain
         * search button, so voice is only requested once the microphone
         * permission has actually been granted.
         */
        const val REQUESTED_WITH_VOICE = REQUESTED_BASE or VOICE
    }

    /**
     * Audio format the TV expects for [voicePayload].
     *
     * Not negotiated anywhere in the protocol - RemoteVoiceBegin carries no
     * audio configuration - so these are the values the Google TV app uses and
     * the only ones known to work.
     */
    object Voice {
        const val SAMPLE_RATE_HZ = 8_000
        const val CHANNELS = 1
        const val BITS_PER_SAMPLE = 16

        /** Larger payloads get the connection closed. */
        const val MAX_CHUNK_BYTES = 20 * 1024

        /** Shield TV rejects anything smaller, so short chunks are padded. */
        const val MIN_CHUNK_BYTES = 8 * 1024
    }

    /** RemoteDirection. A long press is START_LONG, then END_LONG on release. */
    enum class Direction(val wire: Int) {
        SHORT(3),
        START_LONG(1),
        END_LONG(2)
    }

    // ------------------------------------------------------------ outgoing

    /**
     * Step 2 of the handshake, in reply to the TV's own RemoteConfigure.
     *
     * [activeFeatures] must already be the intersection of what we want and
     * what the TV reported.
     */
    fun configure(
        activeFeatures: Int,
        model: String,
        vendor: String,
        packageName: String,
        appVersion: String
    ): ByteArray = protoMessage {
        message(REMOTE_CONFIGURE) {
            varint(CFG_CODE1, activeFeatures)
            message(CFG_DEVICE_INFO) {
                string(DEV_MODEL, model)
                string(DEV_VENDOR, vendor)
                varint(DEV_UNKNOWN1, 1)
                string(DEV_UNKNOWN2, "1")
                string(DEV_PACKAGE_NAME, packageName)
                string(DEV_APP_VERSION, appVersion)
            }
        }
    }

    /** Step 4: confirm the negotiated feature set and go live. */
    fun setActive(activeFeatures: Int): ByteArray = protoMessage {
        message(REMOTE_SET_ACTIVE) {
            varint(ACTIVE_VALUE, activeFeatures)
        }
    }

    /**
     * The keep-alive reply. The TV pings roughly every five seconds and drops
     * the connection after three unanswered pings, so this is not optional.
     */
    fun pingResponse(val1: Int): ByteArray = protoMessage {
        message(REMOTE_PING_RESPONSE) {
            varint(PING_VAL1, val1)
        }
    }

    fun keyInject(keyCode: Int, direction: Direction): ByteArray = protoMessage {
        message(REMOTE_KEY_INJECT) {
            varint(KEY_CODE, keyCode)
            varint(KEY_DIRECTION, direction.wire)
        }
    }

    /** Opens an app by deep link, e.g. `https://www.youtube.com/tv`. */
    fun appLinkLaunch(appLink: String): ByteArray = protoMessage {
        message(REMOTE_APP_LINK_LAUNCH_REQUEST) {
            string(APP_LINK, appLink)
        }
    }

    /**
     * Inserts [text] at the cursor of the focused field on the TV.
     *
     * The counters identify which field is being edited; they arrive from the
     * TV in RemoteImeKeyInject or RemoteImeShowRequest. Sending stale counters
     * makes the edit land nowhere, which looks exactly like nothing happening.
     */
    fun imeInsertText(text: String, imeCounter: Int, fieldCounter: Int): ByteArray = protoMessage {
        message(REMOTE_IME_BATCH_EDIT) {
            varint(BATCH_IME_COUNTER, imeCounter)
            varint(BATCH_FIELD_COUNTER, fieldCounter)
            message(BATCH_EDIT_INFO) {
                varint(EDIT_INSERT, 1)
                message(EDIT_TEXT_FIELD_STATUS) {
                    // start == end == length leaves the caret after the insert.
                    varint(OBJ_START, text.length)
                    varint(OBJ_END, text.length)
                    string(OBJ_VALUE, text)
                }
            }
        }
    }

    fun voiceBegin(sessionId: Int): ByteArray = protoMessage {
        message(REMOTE_VOICE_BEGIN) {
            varint(VOICE_SESSION_ID, sessionId)
        }
    }

    fun voicePayload(sessionId: Int, samples: ByteArray): ByteArray = protoMessage {
        message(REMOTE_VOICE_PAYLOAD) {
            varint(VOICE_SESSION_ID, sessionId)
            bytes(VOICE_SAMPLES, samples)
        }
    }

    fun voiceEnd(sessionId: Int): ByteArray = protoMessage {
        message(REMOTE_VOICE_END) {
            varint(VOICE_SESSION_ID, sessionId)
        }
    }

    // ------------------------------------------------------------ incoming

    data class TvDeviceInfo(
        val model: String?,
        val vendor: String?,
        val appVersion: String?
    )

    data class VolumeInfo(
        val level: Int,
        val max: Int,
        val muted: Boolean,
        val playerModel: String?
    )

    sealed interface Incoming {
        /** The TV opened the handshake and told us what it supports. */
        data class Configure(val supportedFeatures: Int, val deviceInfo: TvDeviceInfo) : Incoming

        /** The TV asks us to confirm the feature set. */
        data object SetActive : Incoming

        /** Handshake complete. [started] false means the screen is off. */
        data class Start(val started: Boolean) : Incoming

        data class PingRequest(val val1: Int) : Incoming

        data class Volume(val info: VolumeInfo) : Incoming

        /**
         * Sent when a text field gains focus. Carries both the package name of
         * the foreground app and the counters needed to send text to it.
         */
        data class ImeKeyInject(
            val appPackage: String?,
            val imeCounter: Int,
            val fieldCounter: Int,
            val fieldLabel: String?,
            val fieldValue: String?
        ) : Incoming

        data class ImeShowRequest(val fieldCounter: Int, val fieldLabel: String?) : Incoming

        data class ImeBatchEdit(val imeCounter: Int, val fieldCounter: Int) : Incoming

        data class VoiceBegin(val sessionId: Int) : Incoming

        data class Error(val detail: String) : Incoming

        data class Unknown(val fields: Set<Int>) : Incoming
    }

    fun parse(frame: ByteArray): Incoming {
        val msg = parseProto(frame)

        msg.message(REMOTE_CONFIGURE)?.let { cfg ->
            val info = cfg.message(CFG_DEVICE_INFO)
            return Incoming.Configure(
                supportedFeatures = cfg.int(CFG_CODE1) ?: 0,
                deviceInfo = TvDeviceInfo(
                    model = info?.string(DEV_MODEL),
                    vendor = info?.string(DEV_VENDOR),
                    appVersion = info?.string(DEV_APP_VERSION)
                )
            )
        }

        if (msg.has(REMOTE_SET_ACTIVE)) return Incoming.SetActive

        msg.message(REMOTE_START)?.let {
            return Incoming.Start(it.bool(START_STARTED))
        }

        msg.message(REMOTE_PING_REQUEST)?.let {
            return Incoming.PingRequest(it.int(PING_VAL1) ?: 0)
        }

        msg.message(REMOTE_SET_VOLUME_LEVEL)?.let { v ->
            return Incoming.Volume(
                VolumeInfo(
                    level = v.int(VOL_LEVEL) ?: 0,
                    max = v.int(VOL_MAX) ?: 0,
                    muted = v.bool(VOL_MUTED),
                    playerModel = v.string(VOL_PLAYER_MODEL)
                )
            )
        }

        msg.message(REMOTE_IME_KEY_INJECT)?.let { inject ->
            val app = inject.message(IME_APP_INFO)
            val field = inject.message(IME_TEXT_FIELD_STATUS)
            return Incoming.ImeKeyInject(
                appPackage = app?.string(APP_PACKAGE),
                imeCounter = app?.int(APP_COUNTER) ?: 0,
                fieldCounter = field?.int(TFS_COUNTER_FIELD) ?: 0,
                fieldLabel = field?.string(TFS_LABEL),
                fieldValue = field?.string(TFS_VALUE)
            )
        }

        msg.message(REMOTE_IME_SHOW_REQUEST)?.let { show ->
            val field = show.message(SHOW_TEXT_FIELD_STATUS)
            return Incoming.ImeShowRequest(
                fieldCounter = field?.int(TFS_COUNTER_FIELD) ?: 0,
                fieldLabel = field?.string(TFS_LABEL)
            )
        }

        msg.message(REMOTE_IME_BATCH_EDIT)?.let { batch ->
            return Incoming.ImeBatchEdit(
                imeCounter = batch.int(BATCH_IME_COUNTER) ?: 0,
                fieldCounter = batch.int(BATCH_FIELD_COUNTER) ?: 0
            )
        }

        msg.message(REMOTE_VOICE_BEGIN)?.let {
            return Incoming.VoiceBegin(it.int(VOICE_SESSION_ID) ?: 0)
        }

        if (msg.has(REMOTE_ERROR)) {
            return Incoming.Error("The TV reported a protocol error")
        }

        return Incoming.Unknown(msg.presentFields)
    }
}
