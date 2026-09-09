package com.emm.hello.newfeatures.card

import android.content.Context
import android.content.res.Resources
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.emm.hello.navigation.Navigator
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
data class CardDetailRoute(val cardId: String, val deckId: String) : NavKey

@Composable
fun CardDetailDestination(navigator: Navigator, cardId: String, deckId: String) {
    val vm: FlashcardDetailViewModel = koinViewModel(
        parameters = { parametersOf(cardId) }
    )

    val uiState by vm.state.collectAsStateWithLifecycle()
    val context: Context = LocalContext.current
    val resources: Resources = LocalResources.current

    LaunchedEffect(Unit) {
        vm.onIntent(FlashcardDetailUiIntent.Load)
    }

    LaunchedEffect(Unit) {
        vm.effect.collect { effect ->
            when (effect) {
                is FlashcardDetailUiEffect.LoadFailed -> {
                    Toast.makeText(context, resources.getString(effect.messageRes), Toast.LENGTH_LONG).show()
                    navigator.goBack()
                }
                FlashcardDetailUiEffect.NavigateBack -> navigator.goBack()
                is FlashcardDetailUiEffect.NavigateToEditFlashcard -> {
                    navigator.navigateTo(EditFlashcardRoute(cardId = effect.cardId, deckId = deckId))
                }
                FlashcardDetailUiEffect.FlashcardDeleted -> navigator.goBack()
                is FlashcardDetailUiEffect.ShowMessage -> {
                    Toast.makeText(context, resources.getString(effect.messageRes), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    FlashcardDetailScreen(
        state = uiState,
        onIntent = vm::onIntent,
    )
}
