package com.emm.hello.newfeatures.capture

import android.content.res.Resources
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.emm.domain.ids.toFlashcardId
import com.emm.hello.enrichment.FlashcardEnrichmentScheduler
import com.emm.hello.navigation.Navigator
import com.emm.hello.newfeatures.card.CardDetailRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel

@Serializable
data object CaptureRoute : NavKey

@Composable
fun CaptureDestination(navigator: Navigator) {
    val vm: CaptureViewModel = koinViewModel()
    val uiState by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources: Resources = LocalResources.current
    val snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
    val snackbarScope: CoroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        vm.effect.collect { effect ->
            when (effect) {
                is CaptureUiEffect.ShowMessage -> {
                    val message: String = resources.getString(effect.messageRes)
                    snackbarScope.launch { snackbarHostState.showSnackbar(message) }
                }
                is CaptureUiEffect.EnqueueEnrichment -> {
                    effect.flashcardIds.forEach { rawId ->
                        FlashcardEnrichmentScheduler.enqueue(context, rawId.toFlashcardId())
                    }
                }
                is CaptureUiEffect.OpenCard -> navigator.navigateTo(
                    CardDetailRoute(cardId = effect.cardId, deckId = effect.deckId),
                )
            }
        }
    }

    CaptureScreen(
        state = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateBack = navigator::goBack,
        onIntent = vm::onIntent,
    )
}
