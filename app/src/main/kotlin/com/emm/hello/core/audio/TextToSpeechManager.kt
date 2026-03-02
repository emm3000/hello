package com.emm.hello.core.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * TextToSpeechManager - Versión refinada y optimizada para la experiencia del usuario.
 */
class TextToSpeechManager(private val context: Context) {

    private var tts: TextToSpeech? = null
    
    private val _isReady = mutableStateOf(false)
    val isReady: State<Boolean> = _isReady
    
    private val _isSpeaking = mutableStateOf(false)
    val isSpeaking: State<Boolean> = _isSpeaking

    var onDoneSpeaking: (() -> Unit)? = null

    fun init(locale: Locale = Locale.US, speed: Float = 0.85f, pitch: Float = 1.0f) {
        if (tts != null) return

        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                setup(locale, speed, pitch)
                _isReady.value = true
            } else {
                _isReady.value = false
            }
        }
    }

    private fun setup(locale: Locale, speed: Float, pitch: Float) {
        tts?.apply {
            language = locale
            setSpeechRate(speed)
            setPitch(pitch)
            setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    onDoneSpeaking?.invoke()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    onDoneSpeaking?.invoke()
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                    onDoneSpeaking?.invoke()
                }

                override fun onStop(utteranceId: String?, interrupted: Boolean) {
                    _isSpeaking.value = false
                    onDoneSpeaking?.invoke()
                }
            })
        }
    }

    fun speak(text: String) {
        if (!_isReady.value || tts == null || text.isBlank()) return

        val cleanText = text.replace(Regex("[^a-zA-Z0-9 ]"), " ").trim()
        val utteranceId = "tts-${System.currentTimeMillis()}"
        
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun shutdown() {
        tts?.apply {
            stop()
            shutdown()
        }
        tts = null
        _isReady.value = false
        _isSpeaking.value = false
    }
}

@Composable
fun rememberTextToSpeechManager(
    locale: Locale = Locale.US,
    speed: Float = 0.85f,
    pitch: Float = 1.0f,
): TextToSpeechManager {
    val context = LocalContext.current
    val manager = remember { TextToSpeechManager(context) }
    
    DisposableEffect(Unit) {
        manager.init(locale, speed, pitch)
        onDispose { manager.shutdown() }
    }
    
    return manager
}
