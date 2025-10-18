package com.emm.hello.newfeatures

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.emm.hello.newfeatures.card.FlashcardDetailScreen
import com.emm.hello.newfeatures.card.FlashcardDetailViewModel
import com.emm.hello.newfeatures.card.newCardRoute
import com.emm.hello.newfeatures.dashboard.DashboardRoute
import com.emm.hello.newfeatures.dashboard.dashboard
import com.emm.hello.newfeatures.dashboard.quote
import com.emm.hello.newfeatures.deck.DeckDetailScreen
import com.emm.hello.newfeatures.deck.DeckDetailUiState
import com.emm.hello.newfeatures.deck.DeckDetailViewModel
import com.emm.hello.newfeatures.deck.newDeckRoute
import com.emm.hello.newfeatures.study.StudyRoute
import com.emm.hello.newfeatures.study.study
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun NewRoot() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = DashboardRoute,
        modifier = Modifier.background(MaterialTheme.colorScheme.background)
    ) {

        dashboard(navController)
        quote(navController)
        study(navController)
        newCardRoute(navController)
        newDeckRoute(navController)
        composable<NewRoutes.DeckDetail> {
            val deckDetail: NewRoutes.DeckDetail = it.toRoute<NewRoutes.DeckDetail>()
            val vm: DeckDetailViewModel = koinViewModel(
                parameters = { parametersOf(deckDetail.deckId) }
            )

            val state: DeckDetailUiState by vm.decks.collectAsStateWithLifecycle()

            DeckDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onReview = { navController.navigate(StudyRoute(state.deck.id)) },
                state = state,
                onCardClick = { cardId ->
                    navController.navigate(NewRoutes.CardDetail(cardId))
                }
            )
        }
        composable<NewRoutes.CardDetail> {
            val cardDetail: NewRoutes.CardDetail = it.toRoute<NewRoutes.CardDetail>()
            val vm: FlashcardDetailViewModel = koinViewModel(
                parameters = { parametersOf(cardDetail.cardId) }
            )

            val state by vm.state.collectAsStateWithLifecycle()
            FlashcardDetailScreen(
                flashcard = state,
            ) { navController.popBackStack() }
        }
    }
}