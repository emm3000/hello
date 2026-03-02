package com.emm.hello.core.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * SpeechToTextManager - Proporciona una experiencia fluida de reconocimiento de voz.
 * Incluye gestión de estados, haptic-ready events y limpieza de resultados.
 */
class SpeechToTextManager(private val context: Context) : RecognitionListener {

    private val recognizer: SpeechRecognizer by lazy {
        SpeechRecognizer.createSpeechRecognizer(context).apply { setRecognitionListener(this@SpeechToTextManager) }
    }

    private val _isListening = mutableStateOf(false)
    val isListening: State<Boolean> = _isListening
    
    private val _textResult = mutableStateOf("")
    val textResult: State<String> = _textResult
    
    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error
    
    private val _soundLevel = mutableFloatStateOf(0f)
    val soundLevel: State<Float> = _soundLevel

    var onResultCallback: ((String) -> Unit)? = null
    var onStateChanged: ((STTState) -> Unit)? = null

    enum class STTState { IDLE, READY_TO_LISTEN, LISTENING, PROCESSING, ERROR }

    fun startListening(locale: Locale = Locale.US) {
        _error.value = null
        _textResult.value = ""
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            recognizer.startListening(intent)
            _isListening.value = true
            onStateChanged?.invoke(STTState.READY_TO_LISTEN)
        } catch (e: Exception) {
            _error.value = "No se pudo iniciar el reconocedor de voz"
            _isListening.value = false
            onStateChanged?.invoke(STTState.ERROR)
        }
    }

    fun stopListening() {
        recognizer.stopListening()
        _isListening.value = false
        onStateChanged?.invoke(STTState.IDLE)
    }

    fun cancel() {
        recognizer.cancel()
        _isListening.value = false
        onStateChanged?.invoke(STTState.IDLE)
    }

    fun destroy() {
        recognizer.destroy()
    }

    // -- RecognitionListener implementation -----------------------------------

    override fun onReadyForSpeech(params: Bundle?) {
        onStateChanged?.invoke(STTState.LISTENING)
    }

    override fun onBeginningOfSpeech() {
        onStateChanged?.invoke(STTState.LISTENING)
    }

    override fun onRmsChanged(rmsdB: Float) {
        _soundLevel.floatValue = (rmsdB + 2f) / 15f
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        _isListening.value = false
        onStateChanged?.invoke(STTState.PROCESSING)
    }

    override fun onError(error: Int) {
        _isListening.value = false
        _error.value = when (error) {
            SpeechRecognizer.ERROR_NO_MATCH -> "No se reconoció ninguna palabra"
            SpeechRecognizer.ERROR_NETWORK -> "Error de red"
            SpeechRecognizer.ERROR_AUDIO -> "Error de audio"
            SpeechRecognizer.ERROR_CLIENT -> "Error del cliente"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permiso denegado"
            else -> "Error en el reconocimiento: $error"
        }
        onStateChanged?.invoke(STTState.ERROR)
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val result = matches[0].trim().replaceFirstChar { it.uppercase() }
            _textResult.value = result
            onResultCallback?.invoke(result)
            onStateChanged?.invoke(STTState.IDLE)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            _textResult.value = matches[0]
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

}

@Composable
fun rememberSpeechToTextManager(
    onResult: (String) -> Unit = {}
): SpeechToTextManager {
    val context = LocalContext.current
    val manager = remember { SpeechToTextManager(context) }
    
    DisposableEffect(Unit) {
        manager.onResultCallback = onResult
        onDispose { manager.destroy() }
    }
    
    return manager
}
