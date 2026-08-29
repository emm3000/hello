package com.emm.hello.newfeatures.study

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.emm.hello.navigation.Navigator
import com.emm.hello.newfeatures.capture.CaptureRoute
import com.emm.hello.newfeatures.suggest.SuggestRoute
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Study session target.
 *
 * [deckId] null means "study all cards due today across every deck" (the global Today CTA).
 * A non-null [deckId] studies only that deck (no surface offers this yet).
 */
@Serializable
data class StudyRoute(val deckId: String? = null) : NavKey {
    companion object {
        /**
         * Sentinel passed to Koin/[StudyViewModel] for the all-decks session, since Koin parameter
         * injection ([parametersOf]) resolves a non-null [String]. The route layer keeps [deckId]
         * nullable; [StudyDestination] normalizes null to this sentinel before injection.
         */
        const val ALL_DUE_DECKS: String = "__all_due_decks__"
    }
}

@Composable
fun StudyDestination(navigator: Navigator, deckId: String?) {
    val vm: StudyViewModel = koinViewModel(
        parameters = { parametersOf(deckId ?: StudyRoute.ALL_DUE_DECKS) }
    )
    val uiState = vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        vm.effect.collect { effect ->
            when (effect) {
                StudyUiEffect.NavigateBack -> navigator.goBack()
                StudyUiEffect.NavigateToCapture -> navigator.navigateTo(CaptureRoute)
                StudyUiEffect.NavigateToSuggest -> navigator.navigateTo(SuggestRoute)
            }
        }
    }

    StudyScreen(
        onExit = { vm.onIntent(StudyUiIntent.ExitClicked) },
        onReviewAnswer = { item, reviewGrade ->
            vm.onIntent(
                StudyUiIntent.ReviewAnswered(
                    item = item,
                    reviewGrade = reviewGrade,
                )
            )
        },
        onCreateCard = { vm.onIntent(StudyUiIntent.CreateCardClicked) },
        onGetNewWords = { vm.onIntent(StudyUiIntent.GetNewWordsClicked) },
        onRetryLoad = { vm.onIntent(StudyUiIntent.RetryLoad) },
        state = uiState.value,
    )
}
