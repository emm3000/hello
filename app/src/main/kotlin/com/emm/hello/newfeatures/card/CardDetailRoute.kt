package com.emm.hello.newfeatures.card

import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
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
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            vm.effect.collect { effect ->
                when (effect) {
                    is FlashcardDetailUiEffect.LoadFailed -> {
                        Toast.makeText(context, effect.message, Toast.LENGTH_LONG).show()
                        navController.popBackStack()
                    }
                }
            }
        }

        FlashcardDetailScreen(
            flashcard = uiState.flashcard,
        ) { navController.popBackStack() }
    }
}
