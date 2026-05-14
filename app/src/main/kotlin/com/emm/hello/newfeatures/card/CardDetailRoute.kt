package com.emm.hello.newfeatures.card

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        vm.effect.collect { effect ->
            when (effect) {
                is FlashcardDetailUiEffect.LoadFailed -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_LONG).show()
                    navigator.goBack()
                }
                is FlashcardDetailUiEffect.NavigateToEditFlashcard -> {
                    navigator.navigateTo(EditFlashcardRoute(cardId = effect.cardId, deckId = deckId))
                }
                FlashcardDetailUiEffect.FlashcardDeleted -> {
                    navigator.goBack()
                }
                is FlashcardDetailUiEffect.ShowMessage -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    FlashcardDetailScreen(
        flashcard = uiState.flashcard,
        onNavigateBack = { navigator.goBack() },
        onEditClick = { vm.onIntent(FlashcardDetailUiIntent.EditFlashcard) },
        onDeleteClick = { vm.onIntent(FlashcardDetailUiIntent.DeleteFlashcard) },
        onConfirmDelete = { vm.onIntent(FlashcardDetailUiIntent.ConfirmDeleteFlashcard) },
        onDismissDelete = { vm.onIntent(FlashcardDetailUiIntent.DismissDeleteFlashcard) },
        isDeleteConfirmationVisible = uiState.isDeleteConfirmationVisible,
    )
}