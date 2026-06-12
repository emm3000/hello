package com.emm.hello.newfeatures.study

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.emm.hello.navigation.Navigator
import com.emm.hello.newfeatures.card.NewCardRoute
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
data class StudyRoute(val deckId: String) : NavKey

@Composable
fun StudyDestination(navigator: Navigator, deckId: String) {
    val vm: StudyViewModel = koinViewModel(
        parameters = { parametersOf(deckId) }
    )
    val uiState = vm.state.collectAsStateWithLifecycle()
    var showFinishDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.effect.collect { effect ->
            when (effect) {
                StudyUiEffect.NavigateBack -> navigator.goBack()
                StudyUiEffect.SessionFinished -> {
                    showFinishDialog = true
                }
                StudyUiEffect.NavigateToNewCard -> navigator.navigateTo(NewCardRoute)
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
        onCreateCard = { vm.onIntent(StudyUiIntent.CreateCardClicked) },
        state = uiState.value,
        showFinishDialog = showFinishDialog,
    )
}
