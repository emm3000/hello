package com.emm.hello.newfeatures.study

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
data class StudyRoute(val deckId: String)

fun NavGraphBuilder.study(navController: NavController) {
    composable<StudyRoute> {
        val route = it.toRoute<StudyRoute>()
        val vm: StudyViewModel = koinViewModel(
            parameters = { parametersOf(route.deckId) }
        )
        val state = vm.state.collectAsStateWithLifecycle()

        StudyScreen(
            onNavigateBack = { navController.popBackStack() },
            onReviewAnswer = { flashcard, reviewGrade ->
                vm.onIntent(
                    StudyUiIntent.ReviewAnswered(
                        flashcard = flashcard,
                        reviewGrade = reviewGrade,
                    )
                )
            },
            state = state.value
        )
    }
}
