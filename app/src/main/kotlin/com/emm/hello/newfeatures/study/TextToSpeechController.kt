package com.emm.hello.newfeatures.study

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

class TextToSpeechController(private val context: Context) {

    private var tts: TextToSpeech? = null

    var isReady: Boolean by mutableStateOf(false)
        private set

    var onDoneSpeaking: (() -> Unit)? = null

    fun init() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.apply {
                    language = Locale.US
                    setSpeechRate(0.5f)
                    setPitch(1.05f)
                    setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {}
                        override fun onStop(utteranceId: String?, interrupted: Boolean) {
                            onDoneSpeaking?.invoke()
                        }
                        override fun onDone(utteranceId: String?) {
                            onDoneSpeaking?.invoke()
                        }
                        override fun onError(utteranceId: String?) {
                            onDoneSpeaking?.invoke()
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            onDoneSpeaking?.invoke()
                        }
                    })
                }
                isReady = true
            } else {
                isReady = false
            }
        }
    }

    fun speak(text: String) {
        if (!isReady || tts == null) {
            return
        }

        tts!!.stop()
        tts!!.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts-${System.currentTimeMillis()}")
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
        isReady = false
    }
}