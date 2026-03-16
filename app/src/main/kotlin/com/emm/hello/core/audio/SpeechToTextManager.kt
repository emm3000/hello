package com.emm.hello.core.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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

/**
 * States for the speech-to-text process.
 */
enum class STTState { IDLE, READY_TO_LISTEN, LISTENING, PROCESSING, ERROR }

/**
 * Interface to control speech-to-text functionality.
 * This allows us to provide a stub implementation during Compose Previews,
 * avoiding NoClassDefFoundError for Android Speech classes not available in layoutlib.
 */
interface SpeechToTextManager {
    val isListening: StateFlow<Boolean>
    val textResult: StateFlow<String>
    val error: StateFlow<String?>
    val state: StateFlow<STTState>
    var onResultCallback: ((String) -> Unit)?
    fun startListening(locale: Locale = Locale.US)
    fun stopListening()
}

/**
 * Android-specific implementation of SpeechToTextManager using SpeechRecognizer.
 */
class AndroidSpeechToTextManager(private val context: Context) : RecognitionListener, SpeechToTextManager {

    private var recognizer: SpeechRecognizer? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isListening = MutableStateFlow(false)
    override val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _textResult = MutableStateFlow("")
    override val textResult: StateFlow<String> = _textResult.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error.asStateFlow()

    private val _state = MutableStateFlow(STTState.IDLE)
    override val state: StateFlow<STTState> = _state.asStateFlow()

    override var onResultCallback: ((String) -> Unit)? = null

    fun init() {
        if (recognizer != null) return
        // SpeechRecognizer must be created on the Main Thread
        mainHandler.post {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@AndroidSpeechToTextManager)
            }
        }
    }

    override fun startListening(locale: Locale) {
        _error.value = null
        _textResult.value = ""

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            recognizer?.startListening(intent)
            _isListening.value = true
            _state.value = STTState.READY_TO_LISTEN
        } catch (_: SecurityException) {
            _error.value = "Could not start speech recognizer"
            _isListening.value = false
            _state.value = STTState.ERROR
        } catch (_: IllegalStateException) {
            _error.value = "Could not start speech recognizer"
            _isListening.value = false
            _state.value = STTState.ERROR
        }
    }

    override fun stopListening() {
        recognizer?.stopListening()
        _isListening.value = false
        _state.value = STTState.IDLE
    }

    fun cancel() {
        recognizer?.cancel()
        _isListening.value = false
        _state.value = STTState.IDLE
    }

    fun destroy() {
        recognizer?.destroy()
        recognizer = null
        _isListening.value = false
        _state.value = STTState.IDLE
    }

    // -- RecognitionListener --------------------------------------------------

    override fun onReadyForSpeech(params: Bundle?) {
        mainHandler.post { _state.value = STTState.LISTENING }
    }

    override fun onBeginningOfSpeech() {
        mainHandler.post { _state.value = STTState.LISTENING }
    }

    override fun onRmsChanged(rmsdB: Float) {}

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        mainHandler.post {
            _isListening.value = false
            _state.value = STTState.PROCESSING
        }
    }

    override fun onError(error: Int) {
        mainHandler.post {
            _isListening.value = false
            _error.value = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_AUDIO -> "Audio error"
                SpeechRecognizer.ERROR_CLIENT -> "Client error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission denied"
                else -> "Recognition error: $error"
            }
            _state.value = STTState.ERROR
        }
    }

    override fun onResults(results: Bundle?) {
        mainHandler.post {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val result = matches[0].trim().replaceFirstChar { it.uppercase() }
                _textResult.value = result
                onResultCallback?.invoke(result)
                _state.value = STTState.IDLE
            }
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        mainHandler.post {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                _textResult.value = matches[0]
            }
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}
}

@Composable
fun rememberSpeechToTextManager(
    onResult: (String) -> Unit = {},
): SpeechToTextManager {
    val isPreview = LocalInspectionMode.current
    val context = LocalContext.current

    if (isPreview) {
        return remember {
            object : SpeechToTextManager {
                override val isListening: StateFlow<Boolean> = MutableStateFlow(false)
                override val textResult: StateFlow<String> = MutableStateFlow("")
                override val error: StateFlow<String?> = MutableStateFlow(null)
                override val state: StateFlow<STTState> = MutableStateFlow(STTState.IDLE)
                override var onResultCallback: ((String) -> Unit)? = null
                override fun startListening(locale: Locale) {}
                override fun stopListening() {}
            }
        }
    }

    val manager = remember { AndroidSpeechToTextManager(context) }
    // rememberUpdatedState ensures the callback always holds the latest lambda,
    // even if the Composable recomposes with a new capture after init.
    val currentOnResult = rememberUpdatedState(onResult)

    DisposableEffect(Unit) {
        manager.init()
        manager.onResultCallback = { currentOnResult.value(it) }
        onDispose { manager.destroy() }
    }

    return manager
}
