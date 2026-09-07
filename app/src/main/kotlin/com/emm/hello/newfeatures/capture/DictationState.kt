package com.emm.hello.newfeatures.capture

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emm.hello.core.audio.SpeechRecognitionError
import com.emm.hello.core.audio.SpeechToTextManager
import com.emm.hello.core.audio.rememberSpeechToTextManager
import java.util.Locale

class DictationState(
    val isListening: Boolean,
    val error: SpeechRecognitionError?,
    val level: () -> Float,
    val onToggle: () -> Unit,
    val onClearError: () -> Unit,
)

@Composable
fun rememberDictationState(
    onText: (String) -> Unit,
    onPermissionDenied: () -> Unit,
): DictationState {
    val manager: SpeechToTextManager = rememberSpeechToTextManager(onText)
    val context: Context = LocalContext.current
    val isListening: Boolean by manager.isListening.collectAsStateWithLifecycle()
    val error: SpeechRecognitionError? by manager.error.collectAsStateWithLifecycle()
    val level: State<Float> = manager.level.collectAsStateWithLifecycle()
    val currentOnPermissionDenied: () -> Unit by rememberUpdatedState(onPermissionDenied)

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) manager.startListening(Locale.US) else currentOnPermissionDenied()
        },
    )

    return DictationState(
        isListening = isListening,
        error = error,
        level = { level.value },
        onToggle = {
            when {
                isListening -> manager.stopListening()
                hasRecordPermission(context) -> manager.startListening(Locale.US)
                else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
        onClearError = manager::clearError,
    )
}

private fun hasRecordPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED
}
