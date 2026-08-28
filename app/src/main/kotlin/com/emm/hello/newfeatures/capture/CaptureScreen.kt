package com.emm.hello.newfeatures.capture

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emm.domain.flashcard.EnrichmentStatus
import com.emm.domain.ids.toFlashcardId
import com.emm.hello.R
import com.emm.hello.core.audio.rememberSpeechToTextManager
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.cardMint
import com.emm.hello.core.theme.ink
import com.emm.hello.core.theme.inkSoft
import com.emm.hello.core.theme.schibsted
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HButtonVariant
import com.emm.hello.core.ui.HFieldVariant
import com.emm.hello.core.ui.HIconButton
import com.emm.hello.core.ui.HInput
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

    Surface(modifier = Modifier.fillMaxSize(), color = cardMint) {
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp, bottom = 24.dp),
    ) {
        CaptureHeader(onNavigateBack = onNavigateBack)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HInput(
                    modifier = Modifier.weight(1f),
                    value = state.word,
                    onValueChange = { onIntent(CaptureUiIntent.WordChanged(it)) },
                    placeholder = stringResource(R.string.capture_placeholder),
                    variant = HFieldVariant.Underline,
                    enabled = !state.isSaving,
                )

                HIconButton(
                    icon = if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
                    contentDescription = stringResource(R.string.capture_mic_content_description),
                    onClick = onMicToggle,
                    tint = ink,
                    buttonSize = 44.dp,
                )
            }

            if (state.recentCaptures.isNotEmpty()) {
                CaptureRecentList(state = state)
            }

            if (state.failed > 0) {
                HButton(
                    text = stringResource(R.string.capture_retry),
                    onClick = { onIntent(CaptureUiIntent.RetryFailed) },
                    variant = HButtonVariant.Text,
                )
            }
        }

        HButton(
            text = stringResource(R.string.capture_save),
            onClick = { onIntent(CaptureUiIntent.Submit) },
            enabled = state.canSubmit,
            isLoading = state.isSaving,
            variant = HButtonVariant.Primary,
            full = true,
        )
    }
}

@Composable
private fun CaptureHeader(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.capture_title).uppercase(),
            fontFamily = schibsted,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            letterSpacing = 0.12.em,
            color = inkSoft,
        )

        Spacer(modifier = Modifier.weight(1f))

        HButton(
            text = stringResource(R.string.capture_done),
            onClick = onNavigateBack,
            variant = HButtonVariant.Text,
        )
    }
}

@Composable
private fun CaptureRecentList(state: CaptureUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.capture_recent_label),
            fontFamily = schibsted,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            color = inkSoft,
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.recentCaptures.forEach { capture ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 28.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = capture.word,
                        fontFamily = schibsted,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        color = ink,
                    )
                    Text(
                        text = stringResource(capture.status.labelRes()),
                        fontFamily = schibsted,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        color = inkSoft,
                    )
                }
            }
        }
    }
}

private fun EnrichmentStatus.labelRes(): Int = when (this) {
    EnrichmentStatus.PENDING -> R.string.capture_status_preparing
    EnrichmentStatus.ENRICHED -> R.string.capture_status_ready
    EnrichmentStatus.FAILED -> R.string.capture_status_failed
}

@PreviewLightDark
@Composable
private fun CaptureScreenPreview() {
    HelloTheme {
        CaptureScreen(
            state = CaptureUiState(
                word = "compelling",
                recentCaptures = listOf(
                    RecentCapture(
                        flashcardId = "1".toFlashcardId(),
                        word = "borrow",
                        status = EnrichmentStatus.ENRICHED,
                    ),
                    RecentCapture(
                        flashcardId = "2".toFlashcardId(),
                        word = "compelling",
                        status = EnrichmentStatus.PENDING,
                    ),
                ),
            ),
            onNavigateBack = {},
            onIntent = {},
        )
    }
}
