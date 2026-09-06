package com.emm.hello.newfeatures.suggest

import android.content.res.Resources
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.emm.domain.ids.toFlashcardId
import com.emm.hello.enrichment.FlashcardEnrichmentScheduler
import com.emm.hello.navigation.Navigator
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel

@Serializable
data object SuggestRoute : NavKey

@Composable
fun SuggestDestination(navigator: Navigator) {
    val vm: SuggestViewModel = koinViewModel()
    val uiState by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources: Resources = LocalResources.current

    LaunchedEffect(Unit) {
        vm.effect.collect { effect ->
            when (effect) {
                is SuggestUiEffect.EnqueueEnrichment -> {
                    effect.flashcardIds.forEach { rawId ->
                        FlashcardEnrichmentScheduler.enqueue(context, rawId.toFlashcardId())
                    }
                }
                is SuggestUiEffect.ShowMessage -> {
                    Toast.makeText(context, resources.getString(effect.messageRes), Toast.LENGTH_SHORT).show()
                }
                SuggestUiEffect.NavigateBack -> navigator.goBack()
            }
        }
    }

    SuggestScreen(
        state = uiState,
        onIntent = vm::onIntent,
    )
}
