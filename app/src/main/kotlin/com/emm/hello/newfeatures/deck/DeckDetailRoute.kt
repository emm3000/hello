package com.emm.hello.newfeatures.deck

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.emm.hello.newfeatures.card.CardDetailRoute
import com.emm.hello.newfeatures.study.StudyRoute
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
data class DeckDetailRoute(val deckId: String)

fun NavGraphBuilder.deckDetailRoute(navController: NavController) {
    composable<DeckDetailRoute> {
        val deckDetailRoute: DeckDetailRoute = it.toRoute<DeckDetailRoute>()
        val vm: DeckDetailViewModel = koinViewModel(
            parameters = { parametersOf(deckDetailRoute.deckId) }
        )

        val state: DeckDetailUiState by vm.decks.collectAsStateWithLifecycle()

        DeckDetailScreen(
            onNavigateBack = { navController.popBackStack() },
            onReview = { navController.navigate(StudyRoute(state.deck.id)) },
            state = state,
            onCardClick = { cardId ->
                navController.navigate(CardDetailRoute(cardId))
            }
        )
    }
}