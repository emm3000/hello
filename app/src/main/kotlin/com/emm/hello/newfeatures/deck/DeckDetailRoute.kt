package com.emm.hello.newfeatures.deck

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.emm.hello.navigation.Navigator
import com.emm.hello.newfeatures.card.CardDetailRoute
import com.emm.hello.newfeatures.card.NewCardRoute
import com.emm.hello.newfeatures.study.StudyRoute
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
data class DeckDetailRoute(val deckId: String) : NavKey

@Composable
fun DeckDetailDestination(navigator: Navigator, deckId: String) {
    val vm: DeckDetailViewModel = koinViewModel(
        parameters = { parametersOf(deckId) }
    )

    val uiState: DeckDetailUiState by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        vm.effect.collect { effect ->
            when (effect) {
                is DeckDetailUiEffect.NavigateToEditDeck -> {
                    navigator.navigateTo(NewDeckRoute(deckId = effect.deckId))
                }
                DeckDetailUiEffect.DeckDeleted -> {
                    navigator.goBack()
                }
                is DeckDetailUiEffect.ShowMessage -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    DeckDetailScreen(
        onNavigateBack = { navigator.goBack() },
        onReview = { navigator.navigateTo(StudyRoute(uiState.deck.id.value)) },
        state = uiState,
        onCardClick = { cardId -> navigator.navigateTo(CardDetailRoute(cardId = cardId, deckId = uiState.deck.id.value)) },
        onAddCard = { navigator.navigateTo(NewCardRoute) },
        onSearchChange = { vm.onIntent(DeckDetailUiIntent.SearchCardsChanged(it)) },
        onEditClick = { vm.onIntent(DeckDetailUiIntent.EditDeck) },
        onDeleteClick = { vm.onIntent(DeckDetailUiIntent.DeleteDeck) },
        onConfirmDelete = { vm.onIntent(DeckDetailUiIntent.ConfirmDeleteDeck) },
        onDismissDelete = { vm.onIntent(DeckDetailUiIntent.DismissDeleteDeck) },
    )
}