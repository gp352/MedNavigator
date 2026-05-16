package com.mednavigator.app.services

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.mednavigator.app.utils.AudioUtils
import com.mednavigator.app.utils.Constants
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.io.Closeable
import java.io.File

class GemmaInferenceService(private val context: Context) : Closeable {

    companion object {
        private const val TAG = "GemmaInferenceService"
    }

    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var loadedModelPath: String? = null
    private val sessionLock = Any()

    fun isLoaded(): Boolean = engine != null

    suspend fun loadModel(modelFile: File): Result<Unit> {
        if (engine != null && loadedModelPath == modelFile.absolutePath) {
            return Result.success(Unit)
        }

        return try {
            close()
            Log.d(TAG, "Starting model load from ${modelFile.absolutePath}")
            
            withTimeout(60000L) { // 60 second timeout
                val config = EngineConfig(
                    modelPath = modelFile.absolutePath,
                    backend = Backend.CPU(),
                    audioBackend = Backend.CPU(),
                    cacheDir = context.cacheDir.path
                )
                val newEngine = Engine(config)
                Log.d(TAG, "Initializing engine...")
                newEngine.initialize()
                Log.d(TAG, "Engine initialized successfully")
                engine = newEngine
                loadedModelPath = modelFile.absolutePath
            }
            Result.success(Unit)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Model load timed out", e)
            Result.failure(Exception("Model loading timed out. Please try again."))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model", e)
            Result.failure(e)
        }
    }

    suspend fun generateResponse(prompt: String, pcm16Audio: ByteArray): Result<String> {
        val activeEngine = engine ?: return Result.failure(IllegalStateException("Model not loaded"))

        return try {
            Log.d(TAG, "Starting inference...")
            val trimmed = AudioUtils.trimToMaxSeconds(
                pcmData = pcm16Audio,
                sampleRate = AudioRecorderService.SAMPLE_RATE,
                channels = AudioRecorderService.CHANNELS,
                bitsPerSample = AudioRecorderService.BITS_PER_SAMPLE,
                maxSeconds = Constants.AUDIO_MAX_SECONDS
            )
            val wavBytes = AudioUtils.pcmToWav(
                pcmData = trimmed,
                sampleRate = AudioRecorderService.SAMPLE_RATE,
                channels = AudioRecorderService.CHANNELS,
                bitsPerSample = AudioRecorderService.BITS_PER_SAMPLE
            )
            Log.d(TAG, "Audio prepared, size: ${wavBytes.size} bytes")

            val conversationConfig = ConversationConfig(
                systemInstruction = Contents.of(Content.Text(prompt))
            )

            withTimeout(90000L) { // 90 second timeout for inference
                synchronized(sessionLock) {
                    conversation?.close()
                    Log.d(TAG, "Creating conversation...")
                    conversation = activeEngine.createConversation(conversationConfig)
                }

                Log.d(TAG, "Sending message to model...")
                val message = conversation?.sendMessage(
                    Contents.of(Content.AudioBytes(wavBytes))
                )

                if (message == null) {
                    Result.failure(IllegalStateException("Failed to create conversation"))
                } else {
                    Log.d(TAG, "Response received from model")
                    Result.success(message.toString())
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Inference timed out", e)
            synchronized(sessionLock) {
                try {
                    conversation?.close()
                } catch (_: Exception) {
                }
                conversation = null
            }
            Result.failure(Exception("Response generation timed out. Please try with a shorter audio."))
        } catch (e: Exception) {
            Log.e(TAG, "Inference failed", e)
            synchronized(sessionLock) {
                try {
                    conversation?.close()
                } catch (_: Exception) {
                }
                conversation = null
            }
            Result.failure(e)
        } finally {
            synchronized(sessionLock) {
                try {
                    conversation?.close()
                } catch (_: Exception) {
                }
                conversation = null
            }
        }
    }

    fun closeConversation() {
        synchronized(sessionLock) {
            try {
                conversation?.close()
            } catch (_: Exception) {
            }
            conversation = null
        }
    }

    override fun close() {
        try {
            conversation?.close()
            engine?.close()
        } catch (_: Exception) {
        }
        conversation = null
        engine = null
        loadedModelPath = null
    }
}
