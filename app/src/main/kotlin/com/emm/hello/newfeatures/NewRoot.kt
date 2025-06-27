package com.emm.hello.newfeatures

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun NewRoot() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NewRoutes.Dashboard,
        modifier = Modifier.background(MaterialTheme.colorScheme.background)
    ) {

        composable<NewRoutes.Dashboard> {
            DashboardScreen(
                newCard = {
                    navController.navigate(NewRoutes.NewCard)
                },
                onDeckDetail = {
                    navController.navigate(NewRoutes.DeckDetail("random"))
                },
                onStartReview = {
                    navController.navigate(NewRoutes.Study)
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