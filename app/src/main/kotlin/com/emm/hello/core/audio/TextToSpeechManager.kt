package com.emm.hello.core.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

private const val DEFAULT_SPEECH_RATE = 0.85f
private const val DEFAULT_PITCH = 1.0f

class TextToSpeechManager(context: Context) {

    private val applicationContext: Context = context.applicationContext

    private var engine: TextToSpeech? = null

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    fun init(
        locale: Locale = Locale.US,
        speechRate: Float = DEFAULT_SPEECH_RATE,
        pitch: Float = DEFAULT_PITCH,
    ) {
        if (engine != null) return

        engine = TextToSpeech(applicationContext) { status ->
            _isReady.value = status == TextToSpeech.SUCCESS && configure(locale, speechRate, pitch)
        }
    }

    fun speak(text: String) {
        val utterance: String = text.trim()
        if (utterance.isBlank() || !_isReady.value) return

        engine?.speak(utterance, TextToSpeech.QUEUE_FLUSH, null, utterance)
    }

    fun stop() {
        engine?.stop()
        _isSpeaking.value = false
    }

    private fun configure(locale: Locale, speechRate: Float, pitch: Float): Boolean {
        val tts: TextToSpeech = engine ?: return false
        if (!TtsLanguagePolicy.isUsable(tts.setLanguage(locale))) return false

        tts.setSpeechRate(speechRate)
        tts.setPitch(pitch)
        tts.setOnUtteranceProgressListener(SpeakingStateListener())
        return true
    }

    private inner class SpeakingStateListener : UtteranceProgressListener() {

        override fun onStart(utteranceId: String?) {
            _isSpeaking.value = true
        }

        override fun onDone(utteranceId: String?) {
            _isSpeaking.value = false
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            _isSpeaking.value = false
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            _isSpeaking.value = false
        }

        override fun onStop(utteranceId: String?, interrupted: Boolean) {
            _isSpeaking.value = false
        }
    }
}
