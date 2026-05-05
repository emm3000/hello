package com.emm.hello.core.audio

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TextToSpeechManager(private val context: Context) {

    private var tts: TextToSpeech? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

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
                    mainHandler.post { _isSpeaking.value = true }
                }

                override fun onDone(utteranceId: String?) {
                    mainHandler.post {
                        _isSpeaking.value = false
                        onDoneSpeaking?.invoke()
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    mainHandler.post { _isSpeaking.value = false }
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    mainHandler.post { _isSpeaking.value = false }
                }

                override fun onStop(utteranceId: String?, interrupted: Boolean) {
                    mainHandler.post {
                        _isSpeaking.value = false
                        onDoneSpeaking?.invoke()
                    }
                }
            })
        }
    }

    fun speak(text: String) {
        if (!_isReady.value || tts == null || text.isBlank()) return

        val utteranceId = "tts-${System.currentTimeMillis()}"
        tts?.speak(text.trim(), TextToSpeech.QUEUE_FLUSH, null, utteranceId)
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
    val isInspectionMode = LocalInspectionMode.current

    DisposableEffect(Unit) {
        if (!isInspectionMode) {
            manager.init(locale, speed, pitch)
        }
        onDispose {
            if (!isInspectionMode) {
                manager.shutdown()
            }
        }
    }

    return manager
}
