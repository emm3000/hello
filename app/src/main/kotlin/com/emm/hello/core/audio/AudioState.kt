package com.emm.hello.core.audio

data class AudioState(
    val speakingUtteranceId: String? = null,
    val isTtsReady: Boolean = false,
) {

    fun isSpeaking(utteranceId: String): Boolean = speakingUtteranceId == utteranceId
}
