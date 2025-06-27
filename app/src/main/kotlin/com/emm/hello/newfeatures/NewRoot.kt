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
import com.emm.hello.newfeatures.dashboard.DashboardScreen
import com.emm.hello.newfeatures.dashboard.DashboardUiState
import com.emm.hello.newfeatures.dashboard.DashboardViewModel
import com.emm.hello.newfeatures.deck.DeckDetailScreen
import com.emm.hello.newfeatures.deck.NewDeckScreen
import com.emm.hello.newfeatures.deck.NewDeckViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun NewRoot() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NewRoutes.Dashboard,
        modifier = Modifier.background(MaterialTheme.colorScheme.background)
    ) {

        composable<NewRoutes.Dashboard> {
            val vm: DashboardViewModel = koinViewModel()

            val state: DashboardUiState by vm.state.collectAsStateWithLifecycle()

            DashboardScreen(
                state = state,
                newCard = {
                    navController.navigate(NewRoutes.NewCard)
                },
                onDeckDetail = {
                    navController.navigate(NewRoutes.DeckDetail("random"))
                },
                onStartReview = {
                    navController.navigate(NewRoutes.Study)
                },
                onCreateDeck = {
                    navController.navigate(NewRoutes.NewDeck)
                }
            )
        }
        composable<NewRoutes.Study> {
            StudyScreen { navController.popBackStack() }
        }
        composable<NewRoutes.NewCard> {
            NewCardScreen {
                navController.popBackStack()
            }
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
            DeckDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onReview = { navController.navigate(NewRoutes.Study) },
                onCardClick = {
                    navController.navigate(NewRoutes.CardDetail(it))
                }
            )
        }
        composable<NewRoutes.CardDetail> {
            CardDetailScreen { navController.popBackStack() }
        }
    }
}