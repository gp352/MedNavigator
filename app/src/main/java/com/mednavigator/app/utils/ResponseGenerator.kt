package com.mednavigator.app.utils

object ResponseGenerator {

    fun generate(hasAudio: Boolean): String {
        if (!hasAudio) {
            return "I didn't catch that. Please try again."
        }
        return "Thank you for your voice input. I received your query and will analyze it soon."
    }
}
