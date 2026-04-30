package com.mednavigator.app.services

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class AudioRecorderService {

    companion object {
        private const val TAG = "AudioRecorderService"
        const val SAMPLE_RATE = 16000
        const val CHANNELS = 1
        const val BITS_PER_SAMPLE = 16
    }

    private val sampleRate = SAMPLE_RATE
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    private var audioRecord: AudioRecord? = null
    private var bufferStream: ByteArrayOutputStream? = null
    private var isRecording = false

    private val _recordingState = MutableStateFlow(false)
    val recordingState: StateFlow<Boolean> = _recordingState

    suspend fun startRecording(): Result<Unit> = withContext(Dispatchers.IO) {
        if (isRecording) return@withContext Result.success(Unit)

        try {
            bufferStream = ByteArrayOutputStream()
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord not initialized")
                return@withContext Result.failure(Exception("AudioRecord not initialized"))
            }

            audioRecord?.startRecording()
            isRecording = true
            _recordingState.value = true

            val buffer = ShortArray(bufferSize)
            while (isRecording) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: break
                if (read > 0) {
                    for (i in 0 until read) {
                        val s = buffer[i].toInt()
                        bufferStream?.write(s and 0xFF)
                        bufferStream?.write((s shr 8) and 0xFF)
                    }
                } else if (read < 0) {
                    // read() returns negative on error (e.g. after stop())
                    Log.w(TAG, "AudioRecord.read returned $read, stopping")
                    break
                }
            }

            Log.d(TAG, "Recording loop ended, captured ${bufferStream?.size() ?: 0} bytes")
            Result.success(Unit)
        } catch (e: SecurityException) {
            Log.e(TAG, "Microphone permission denied", e)
            isRecording = false
            _recordingState.value = false
            Result.failure(Exception("Microphone permission not granted"))
        } catch (e: Exception) {
            Log.e(TAG, "Recording error", e)
            isRecording = false
            _recordingState.value = false
            Result.failure(e)
        }
    }

    fun stopRecording(): ByteArray? {
        if (!isRecording) return null

        isRecording = false
        _recordingState.value = false

        try {
            audioRecord?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping AudioRecord", e)
        }
        try {
            audioRecord?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing AudioRecord", e)
        }
        audioRecord = null

        val bytes = bufferStream?.toByteArray()
        bufferStream = null
        Log.d(TAG, "stopRecording: returning ${bytes?.size ?: 0} bytes")
        return bytes
    }

    fun release() {
        stopRecording()
    }
}
