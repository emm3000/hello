package com.emm.hello.newfeatures.study

import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
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
data class StudyRoute(val deckId: String)

fun NavGraphBuilder.study(navController: NavController) {
    composable<StudyRoute> {
        val route = it.toRoute<StudyRoute>()
        val vm: StudyViewModel = koinViewModel(
            parameters = { parametersOf(route.deckId) }
        )
        val state = vm.state.collectAsStateWithLifecycle()
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            vm.effect.collect { effect ->
                when (effect) {
                    StudyUiEffect.NavigateBack -> navController.popBackStack()
                    StudyUiEffect.SessionFinished -> {
                        Toast.makeText(context, "Sesion completada", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        StudyScreen(
            onBackRequested = { vm.onIntent(StudyUiIntent.BackClicked) },
            onFinishDialogDismissed = { vm.onIntent(StudyUiIntent.FinishDialogDismissed) },
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
