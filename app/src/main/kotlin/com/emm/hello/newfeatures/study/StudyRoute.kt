package com.emm.hello.newfeatures.study

import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
        val uiState = vm.uiState.collectAsStateWithLifecycle()
        val context = LocalContext.current
        var showFinishDialog by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            vm.effect.collect { effect ->
                when (effect) {
                    StudyUiEffect.NavigateBack -> navController.popBackStack()
                    StudyUiEffect.SessionFinished -> {
                        showFinishDialog = true
                        Toast.makeText(context, "Sesion completada", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        StudyScreen(
            onBackRequested = { vm.onIntent(StudyUiIntent.BackClicked) },
            onFinishDialogDismissed = {
                showFinishDialog = false
                vm.onIntent(StudyUiIntent.FinishDialogDismissed)
            },
            onReviewAnswer = { item, reviewGrade ->
                vm.onIntent(
                    StudyUiIntent.ReviewAnswered(
                        item = item,
                        reviewGrade = reviewGrade,
                    )
                )
            },
            state = uiState.value,
            showFinishDialog = showFinishDialog,
        )
    }
}
