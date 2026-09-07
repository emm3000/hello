package com.emm.hello.core.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/** Exists as an interface to stub in Compose previews (layoutlib does not include android.speech). */
interface SpeechToTextManager {
    val isListening: StateFlow<Boolean>
    val level: StateFlow<Float>
    val error: StateFlow<SpeechRecognitionError?>
    var onResultCallback: ((String) -> Unit)?
    fun startListening(locale: Locale = Locale.US)
    fun stopListening()
    fun clearError()
}

class AndroidSpeechToTextManager(context: Context) : RecognitionListener, SpeechToTextManager {

    private val applicationContext: Context = context.applicationContext

    private var recognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    override val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _level = MutableStateFlow(0f)
    override val level: StateFlow<Float> = _level.asStateFlow()

    private val _error = MutableStateFlow<SpeechRecognitionError?>(null)
    override val error: StateFlow<SpeechRecognitionError?> = _error.asStateFlow()

    override var onResultCallback: ((String) -> Unit)? = null

    fun init() {
        if (recognizer != null) return
        if (!SpeechRecognizer.isRecognitionAvailable(applicationContext)) return

        recognizer = SpeechRecognizer.createSpeechRecognizer(applicationContext).apply {
            setRecognitionListener(this@AndroidSpeechToTextManager)
        }
    }

    override fun startListening(locale: Locale) {
        val available: SpeechRecognizer = recognizer ?: run {
            fail(SpeechRecognitionError.Unavailable)
            return
        }

        _error.value = null
        try {
            available.startListening(recognitionIntent(locale))
            _isListening.value = true
        } catch (_: SecurityException) {
            fail(SpeechRecognitionError.PermissionMissing)
        } catch (_: IllegalStateException) {
            fail(SpeechRecognitionError.Unavailable)
        }
    }

    override fun stopListening() {
        recognizer?.stopListening()
        idle()
    }

    override fun clearError() {
        _error.value = null
    }

    fun destroy() {
        recognizer?.destroy()
        recognizer = null
        idle()
    }

    override fun onEndOfSpeech() {
        idle()
    }

    override fun onError(error: Int) {
        fail(SpeechRecognitionError.fromCode(error))
    }

    override fun onResults(results: Bundle?) {
        idle()
        val spoken: String = results
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.trim()
            ?.replaceFirstChar { it.uppercase() }
            .orEmpty()

        if (spoken.isNotEmpty()) onResultCallback?.invoke(spoken)
    }

    override fun onReadyForSpeech(params: Bundle?) = Unit

    override fun onBeginningOfSpeech() = Unit

    override fun onRmsChanged(rmsdB: Float) {
        _level.value = MicLevel.smooth(previous = _level.value, target = MicLevel.fromRms(rmsdB))
    }

    override fun onBufferReceived(buffer: ByteArray?) = Unit

    override fun onPartialResults(partialResults: Bundle?) = Unit

    override fun onEvent(eventType: Int, params: Bundle?) = Unit

    private fun fail(reason: SpeechRecognitionError) {
        idle()
        _error.value = reason
    }

    private fun idle() {
        _isListening.value = false
        _level.value = 0f
    }

    private fun recognitionIntent(locale: Locale): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
    }
}

@Composable
fun rememberSpeechToTextManager(onResult: (String) -> Unit = {}): SpeechToTextManager {
    if (LocalInspectionMode.current) {
        return remember { PreviewSpeechToTextManager() }
    }

    val context: Context = LocalContext.current
    val manager: AndroidSpeechToTextManager = remember { AndroidSpeechToTextManager(context) }
    val currentOnResult = rememberUpdatedState(onResult)

    DisposableEffect(manager) {
        manager.init()
        manager.onResultCallback = { currentOnResult.value(it) }
        onDispose { manager.destroy() }
    }

    return manager
}

private class PreviewSpeechToTextManager : SpeechToTextManager {
    override val isListening: StateFlow<Boolean> = MutableStateFlow(false)
    override val level: StateFlow<Float> = MutableStateFlow(0f)
    override val error: StateFlow<SpeechRecognitionError?> = MutableStateFlow(null)
    override var onResultCallback: ((String) -> Unit)? = null
    override fun startListening(locale: Locale) = Unit
    override fun stopListening() = Unit
    override fun clearError() = Unit
}
