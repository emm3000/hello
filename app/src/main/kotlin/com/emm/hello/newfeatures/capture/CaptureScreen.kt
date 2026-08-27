package com.emm.hello.newfeatures.capture

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emm.hello.R
import com.emm.hello.core.audio.rememberSpeechToTextManager
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.geist
import com.emm.hello.core.theme.instrumentAccent
import com.emm.hello.core.theme.instrumentBg
import com.emm.hello.core.theme.instrumentMuted
import com.emm.hello.core.theme.instrumentOnBg
import com.emm.hello.core.theme.spacing
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HButtonVariant
import com.emm.hello.core.ui.HCard
import com.emm.hello.core.ui.HIconButton
import com.emm.hello.core.ui.HInput
import com.emm.hello.core.ui.HTopBar
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun CaptureScreen(
    state: CaptureUiState,
    onNavigateBack: () -> Unit,
    onIntent: (CaptureUiIntent) -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val sttManager = rememberSpeechToTextManager { voiceText -> onIntent(CaptureUiIntent.WordChanged(voiceText)) }
    val isListening by sttManager.isListening.collectAsStateWithLifecycle()
    val sttError by sttManager.error.collectAsStateWithLifecycle()
    val micPermissionDeniedMessage = stringResource(R.string.mic_permission_denied)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                sttManager.startListening(Locale.US)
            } else {
                snackbarScope.launch { snackbarHostState.showSnackbar(micPermissionDeniedMessage) }
            }
        },
    )

    LaunchedEffect(sttError) {
        val message: String? = sttError
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            sttManager.clearError()
        }
    }

    val onMicToggle: () -> Unit = {
        val granted: Boolean = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        when {
            isListening -> sttManager.stopListening()
            granted -> sttManager.startListening(Locale.US)
            else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = instrumentBg) {
        Box(modifier = Modifier.fillMaxSize()) {
            CaptureContent(
                state = state,
                isListening = isListening,
                onNavigateBack = onNavigateBack,
                onMicToggle = onMicToggle,
                onIntent = onIntent,
            )

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun CaptureContent(
    state: CaptureUiState,
    isListening: Boolean,
    onNavigateBack: () -> Unit,
    onMicToggle: () -> Unit,
    onIntent: (CaptureUiIntent) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        HTopBar(title = stringResource(R.string.capture_title), onBack = onNavigateBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.screenGutter, vertical = MaterialTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg),
        ) {
            Text(
                text = stringResource(R.string.capture_headline),
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = geist,
                color = instrumentOnBg,
            )

            HInput(
                value = state.word,
                onValueChange = { onIntent(CaptureUiIntent.WordChanged(it)) },
                placeholder = stringResource(R.string.capture_placeholder),
                supportingText = state.targetDeck?.let { stringResource(R.string.capture_target_deck, it.name) },
                enabled = !state.isSaving,
                trailingIcon = {
                    HIconButton(
                        icon = if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
                        contentDescription = stringResource(R.string.capture_mic_content_description),
                        onClick = onMicToggle,
                        tint = instrumentAccent,
                    )
                },
            )

            HButton(
                text = stringResource(R.string.capture_save),
                onClick = { onIntent(CaptureUiIntent.Submit) },
                enabled = state.canSubmit,
                isLoading = state.isSaving,
                full = true,
            )

            if (state.hasBacklog) {
                CaptureBacklog(state = state, onRetry = { onIntent(CaptureUiIntent.RetryFailed) })
            }
        }
    }
}

@Composable
private fun CaptureBacklog(state: CaptureUiState, onRetry: () -> Unit) {
    HCard {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
            if (state.pending > 0) {
                Text(
                    text = pluralStringResource(R.plurals.capture_backlog_pending, state.pending, state.pending),
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = geist,
                    color = instrumentMuted,
                )
            }

            if (state.failed > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = pluralStringResource(R.plurals.capture_backlog_failed, state.failed, state.failed),
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = geist,
                        color = instrumentOnBg,
                    )
                    HButton(
                        text = stringResource(R.string.capture_retry),
                        onClick = onRetry,
                        variant = HButtonVariant.Ghost,
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun CaptureScreenPreview() {
    HelloTheme {
        CaptureScreen(
            state = CaptureUiState(word = "compelling", pending = 2, failed = 1),
            onNavigateBack = {},
            onIntent = {},
        )
    }
}
