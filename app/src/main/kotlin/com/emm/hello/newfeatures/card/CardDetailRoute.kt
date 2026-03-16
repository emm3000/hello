package com.emm.hello.newfeatures.card

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
data class CardDetailRoute(val cardId: String)

fun NavGraphBuilder.cardDetailRoute(navController: NavController) {
    composable<CardDetailRoute> {
        val cardDetailRoute: CardDetailRoute = it.toRoute<CardDetailRoute>()
        val vm: FlashcardDetailViewModel = koinViewModel(
            parameters = { parametersOf(cardDetailRoute.cardId) }
        )

        val uiState by vm.uiState.collectAsStateWithLifecycle()
        FlashcardDetailScreen(
            flashcard = uiState,
        ) { navController.popBackStack() }
    }
}
