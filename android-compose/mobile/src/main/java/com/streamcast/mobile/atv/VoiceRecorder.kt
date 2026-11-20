package com.streamcast.mobile.atv

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.streamcast.shared.atv.RemoteProto
import kotlin.math.abs
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Captures microphone audio in the one format the TV accepts.
 *
 * The Android TV Remote protocol carries raw PCM: 16-bit signed, mono, 8 kHz,
 * little-endian. Nothing in the protocol negotiates this - RemoteVoiceBegin
 * has no audio configuration field - so it is fixed, and sending anything else
 * produces a TV that hears nothing and reports no error.
 *
 * Capture runs on its own thread because [AudioRecord.read] blocks, and audio
 * that is read late is audio that is lost.
 */
class VoiceRecorder(private val scope: CoroutineScope) {

    private val _amplitude = MutableStateFlow(0f)

    /**
     * Loudness of the most recent buffer, 0..1.
     *
     * Only used to animate the button. Without it there is no way to tell a
     * working microphone from a muted one until the TV fails to respond.
     */
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _recording = MutableStateFlow(false)
    val recording: StateFlow<Boolean> = _recording.asStateFlow()

    private var record: AudioRecord? = null
    private var job: Job? = null

    /**
     * Starts capturing and hands each buffer to [onChunk].
     *
     * The caller must hold RECORD_AUDIO; without it AudioRecord is created in
     * an uninitialised state rather than throwing, which is checked for here.
     *
     * @return true if the microphone actually opened.
     */
    @SuppressLint("MissingPermission")
    fun start(onChunk: (ByteArray) -> Unit): Boolean {
        if (_recording.value) return true

        val minBuffer = AudioRecord.getMinBufferSize(
            RemoteProto.Voice.SAMPLE_RATE_HZ,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) {
            Log.w(TAG, "8 kHz mono capture is not available on this device")
            return false
        }

        // Read in pieces at least as large as the protocol's minimum, so a
        // chunk is normally sent whole rather than padded with silence.
        val bufferBytes = maxOf(minBuffer, RemoteProto.Voice.MIN_CHUNK_BYTES)

        val recorder = try {
            AudioRecord(
                // VOICE_RECOGNITION applies the noise suppression tuned for
                // speech and, unlike MIC, does not get AGC-pumped by the room.
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                RemoteProto.Voice.SAMPLE_RATE_HZ,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferBytes * 2
            )
        } catch (e: Exception) {
            Log.w(TAG, "Could not open the microphone", e)
            return false
        }

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            // Almost always a missing permission.
            Log.w(TAG, "Microphone did not initialise (state=${recorder.state})")
            runCatching { recorder.release() }
            return false
        }

        record = recorder
        _recording.value = true

        try {
            recorder.startRecording()
        } catch (e: Exception) {
            Log.w(TAG, "Could not start recording", e)
            stop()
            return false
        }

        job = scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(bufferBytes)
            while (isActive && _recording.value) {
                val read = try {
                    recorder.read(buffer, 0, buffer.size)
                } catch (e: Exception) {
                    Log.w(TAG, "Microphone read failed", e)
                    break
                }
                if (read <= 0) continue

                val chunk = buffer.copyOfRange(0, read)
                _amplitude.value = levelOf(chunk)
                onChunk(chunk)
            }
        }
        return true
    }

    fun stop() {
        _recording.value = false
        job?.cancel()
        job = null
        record?.let { recorder ->
            runCatching { if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) recorder.stop() }
            runCatching { recorder.release() }
        }
        record = null
        _amplitude.value = 0f
    }

    /**
     * Rough loudness of a 16-bit little-endian buffer, scaled to 0..1.
     *
     * Peak rather than RMS, and sampled rather than exhaustive: this drives an
     * animation, so being cheap matters more than being exact.
     */
    private fun levelOf(pcm: ByteArray): Float {
        if (pcm.size < 2) return 0f
        var peak = 0
        var i = 0
        val step = 2 * SAMPLE_STRIDE
        while (i + 1 < pcm.size) {
            val sample = ((pcm[i + 1].toInt() shl 8) or (pcm[i].toInt() and 0xFF)).toShort().toInt()
            val magnitude = abs(sample)
            if (magnitude > peak) peak = magnitude
            i += step
        }
        return min(1f, peak / 32768f * GAIN)
    }

    private companion object {
        const val TAG = "AtvVoice"

        /** Every nth sample is enough to drive a meter. */
        const val SAMPLE_STRIDE = 8

        /** Speech rarely approaches full scale; without this the ring barely moves. */
        const val GAIN = 2.6f
    }
}
