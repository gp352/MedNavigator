package com.mednavigator.app.utils

import java.nio.ByteBuffer
import java.nio.ByteOrder

object AudioUtils {

    fun pcmToWav(
        pcmData: ByteArray,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ): ByteArray {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val dataSize = pcmData.size
        val totalDataLen = 36 + dataSize

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(totalDataLen)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16) // PCM header size
            putShort(1) // PCM format
            putShort(channels.toShort())
            putInt(sampleRate)
            putInt(byteRate)
            putShort((channels * bitsPerSample / 8).toShort())
            putShort(bitsPerSample.toShort())
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(dataSize)
        }.array()

        return header + pcmData
    }

    fun trimToMaxSeconds(
        pcmData: ByteArray,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int,
        maxSeconds: Int
    ): ByteArray {
        val bytesPerSecond = sampleRate * channels * bitsPerSample / 8
        val maxBytes = bytesPerSecond * maxSeconds
        return if (pcmData.size > maxBytes) {
            pcmData.copyOfRange(0, maxBytes)
        } else {
            pcmData
        }
    }
}
