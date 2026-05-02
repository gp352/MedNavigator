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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.delay
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

    fun loadModel(modelFile: File): Result<Unit> {
        if (engine != null && loadedModelPath == modelFile.absolutePath) {
            return Result.success(Unit)
        }

        return try {
            close()
            val config = EngineConfig(
                modelPath = modelFile.absolutePath,
                backend = Backend.CPU(),
                audioBackend = Backend.CPU(),
                cacheDir = context.cacheDir.path
            )
            val newEngine = Engine(config)
            newEngine.initialize()
            engine = newEngine
            loadedModelPath = modelFile.absolutePath
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model", e)
            Result.failure(e)
        }
    }

    fun generateResponse(prompt: String, pcm16Audio: ByteArray): Result<String> {
        val activeEngine = engine ?: return Result.failure(IllegalStateException("Model not loaded"))

        return try {
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

            val conversationConfig = ConversationConfig(
                systemInstruction = Contents.of(Content.Text(prompt))
            )

            synchronized(sessionLock) {
                conversation?.close()
                conversation = activeEngine.createConversation(conversationConfig)
            }

            val message = conversation?.sendMessage(
                Contents.of(Content.AudioBytes(wavBytes))
            )

            if (message == null) {
                Result.failure(IllegalStateException("Failed to create conversation"))
            } else {
                Result.success(message.toString())
            }
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

    /**
     * Generate response with streaming support — emits text chunks as Flow<String>
     * This allows real-time UI updates as the model produces output.
     */
    fun generateResponseStream(prompt: String, pcm16Audio: ByteArray): Flow<String> = flow {
        val activeEngine = engine ?: throw IllegalStateException("Model not loaded")

        try {
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

            val conversationConfig = ConversationConfig(
                systemInstruction = Contents.of(Content.Text(prompt))
            )

            synchronized(sessionLock) {
                conversation?.close()
                conversation = activeEngine.createConversation(conversationConfig)
            }

            val message = conversation?.sendMessage(
                Contents.of(Content.AudioBytes(wavBytes))
            )

            if (message != null) {
                // Emit the complete message (Gemma4 may not support true token streaming,
                // so we emit the full response or chunk it manually if needed)
                val responseText = message.toString()

                // If response is lengthy, chunk it for better UX
                val chunkSize = 50 // characters per chunk
                var offset = 0
                while (offset < responseText.length) {
                    val chunk = responseText.substring(
                        offset,
                        minOf(offset + chunkSize, responseText.length)
                    )
                    emit(chunk)
                    offset += chunkSize
                    // Small delay to simulate streaming effect
                    delay(10)
                }
            } else {
                throw IllegalStateException("Failed to create conversation or get response")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Streaming inference failed", e)
            synchronized(sessionLock) {
                try {
                    conversation?.close()
                } catch (_: Exception) {
                }
                conversation = null
            }
            throw e
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
