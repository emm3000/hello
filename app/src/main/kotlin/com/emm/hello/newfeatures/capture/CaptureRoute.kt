package com.emm.hello.newfeatures.capture

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.emm.domain.ids.toFlashcardId
import com.emm.hello.enrichment.FlashcardEnrichmentScheduler
import com.emm.hello.navigation.Navigator
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel

@Serializable
data object CaptureRoute : NavKey

@Composable
fun CaptureDestination(navigator: Navigator) {
    val vm: CaptureViewModel = koinViewModel()
    val uiState by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        vm.effect.collect { effect ->
            when (effect) {
                is CaptureUiEffect.ShowMessage -> {
                    Toast.makeText(context, context.getString(effect.messageRes), Toast.LENGTH_SHORT).show()
                }
                is CaptureUiEffect.EnqueueEnrichment -> {
                    effect.flashcardIds.forEach { rawId ->
                        FlashcardEnrichmentScheduler.enqueue(context, rawId.toFlashcardId())
                    }
                }
            }
        }
    }

    CaptureScreen(
        state = uiState,
        onNavigateBack = navigator::goBack,
        onIntent = vm::onIntent,
    )
}
