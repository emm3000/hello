package com.emm.hello.core.audio

import android.speech.tts.TextToSpeech

object TtsLanguagePolicy {

    fun isUsable(languageResult: Int): Boolean {
        return languageResult != TextToSpeech.LANG_MISSING_DATA &&
            languageResult != TextToSpeech.LANG_NOT_SUPPORTED
    }
}
