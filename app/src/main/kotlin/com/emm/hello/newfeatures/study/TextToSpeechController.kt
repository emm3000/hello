package com.emm.hello.newfeatures.study

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TextToSpeechController(private val context: Context) {

    private var tts: TextToSpeech? = null

    var isReady: Boolean = false

    fun init(onReady: (() -> Unit)? = null) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.apply {
                    language = Locale.US
                    setSpeechRate(0.5f)
                    setPitch(1.05f)
                }
                onReady?.invoke()
                isReady = true
            }
        }
    }

    fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts-${System.currentTimeMillis()}")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.apply {
            stop()
            shutdown()
        }
        tts = null
    }
}