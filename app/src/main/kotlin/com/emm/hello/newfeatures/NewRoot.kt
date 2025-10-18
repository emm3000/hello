package com.emm.hello.newfeatures

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.emm.data.remote.DataStore
import com.emm.domain.flashcard.CreateFlashcardInput
import com.emm.domain.flashcard.FlashcardRepository
import com.emm.domain.quote.Quote
import com.emm.domain.quote.QuoteRepository
import com.emm.hello.newfeatures.card.FlashcardDetailScreen
import com.emm.hello.newfeatures.card.FlashcardDetailViewModel
import com.emm.hello.newfeatures.card.NewCardScreen
import com.emm.hello.newfeatures.card.NewCardViewModel
import com.emm.hello.newfeatures.dashboard.DashboardRoute
import com.emm.hello.newfeatures.dashboard.QuotesScreen
import com.emm.hello.newfeatures.dashboard.dashboard
import com.emm.hello.newfeatures.deck.DeckDetailScreen
import com.emm.hello.newfeatures.deck.DeckDetailUiState
import com.emm.hello.newfeatures.deck.DeckDetailViewModel
import com.emm.hello.newfeatures.deck.NewDeckScreen
import com.emm.hello.newfeatures.deck.NewDeckViewModel
import com.emm.hello.newfeatures.study.StudyScreen
import com.emm.hello.newfeatures.study.StudyViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
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
        composable<NewRoutes.Quotes>(
            deepLinks = listOf(
                navDeepLink { uriPattern = "gema://quotes" }
            )
        ) {
            val quotesRepository: QuoteRepository = koinInject()
            val cardRepository: FlashcardRepository = koinInject()
            val dataStore: DataStore = koinInject()

            val quotes: List<Quote> by quotesRepository.allQuotes().collectAsStateWithLifecycle(emptyList())

            val scope = rememberCoroutineScope()

            QuotesScreen(quotes) {
                scope.launch {
                    if (dataStore.defaultDeck.isNotEmpty()) {
                        val input = CreateFlashcardInput(
                            id = it.id,
                            deckId = dataStore.defaultDeck,
                            word = it.phrase,
                            meaning = it.description,
                            translation = it.translation,
                            phonetic = it.pronunciation
                        )
                        cardRepository.create(input)
                    }
                }
            }
        }
        composable<NewRoutes.Study> {
            val route = it.toRoute<NewRoutes.Study>()
            val vm: StudyViewModel = koinViewModel(
                parameters = { parametersOf(route.deckId) }
            )

            StudyScreen(
                onNavigateBack = { navController.popBackStack() },
                onReviewAnswer = vm::onProcess,
                state = vm.state
            )
        }
        composable<NewRoutes.NewCard> {
            val vm: NewCardViewModel = koinViewModel()

            NewCardScreen(
                onNavigateBack = { navController.popBackStack() },
                state = vm.state,
                onAction = vm::onAction,
            )
        }
        composable<NewRoutes.NewDeck> {
            val vm: NewDeckViewModel = koinViewModel()
            NewDeckScreen(
                onNavigateBack = { navController.popBackStack() },
                state = vm.state,
                onAction = vm::onAction,
            )
        }
        composable<NewRoutes.DeckDetail> {
            val deckDetail: NewRoutes.DeckDetail = it.toRoute<NewRoutes.DeckDetail>()
            val vm: DeckDetailViewModel = koinViewModel(
                parameters = { parametersOf(deckDetail.deckId) }
            )

            val state: DeckDetailUiState by vm.decks.collectAsStateWithLifecycle()

            DeckDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onReview = { navController.navigate(NewRoutes.Study(state.deck.id)) },
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