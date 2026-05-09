package com.mednavigator.app.ui.navigation

object Routes {
    const val SPLASH = "splash"
    const val MODEL_DOWNLOAD = "model_download"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val HISTORY = "history"
    const val CONVERSATION_DETAIL = "conversation_detail/{conversationId}"

    fun conversationDetail(conversationId: Int): String = "conversation_detail/$conversationId"
}
