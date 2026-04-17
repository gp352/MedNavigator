package com.mednavigator.app.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream

object BitmapUtils {

    // Resize image to max dimension before sending to E4B vision encoder
    // E4B at 560 vision tokens works best with images ~800px on longest side
    fun resizeForInference(bitmap: Bitmap, maxDim: Int = 800): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDim && height <= maxDim) return bitmap

        val ratio = maxDim.toFloat() / maxOf(width, height)
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    // Convert bitmap to base64 JPEG string for E4B image input
    fun toBase64Jpeg(bitmap: Bitmap, quality: Int = 85): String {
        val resized = resizeForInference(bitmap)
        val outputStream = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    // Decode base64 back to bitmap (for display)
    fun fromBase64(base64: String): Bitmap? {
        return try {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }
}
