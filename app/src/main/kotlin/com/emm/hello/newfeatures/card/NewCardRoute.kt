package com.emm.hello.newfeatures.card

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.emm.hello.navigation.Navigator
import com.emm.hello.newfeatures.deck.NewDeckRoute
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel

@Serializable
data object NewCardRoute : NavKey

private enum class NewCardFlowStep {
    Input,
    Review,
}

@Composable
fun NewCardDestination(navigator: Navigator) {
    val vm: NewCardViewModel = koinViewModel()
    val uiState by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var currentStep by rememberSaveable { mutableStateOf(NewCardFlowStep.Input) }

    // Mirror the HWizTop back arrow for system/predictive back: step within the
    // wizard instead of popping NewCardRoute and discarding typed input. Disabled
    // on the first step so the gesture falls through to goBack(), matching the arrow.
    BackHandler(enabled = currentStep == NewCardFlowStep.Review) {
        currentStep = NewCardFlowStep.Input
    }

    LaunchedEffect(Unit) {
        vm.effect.collect { effect ->
            when (effect) {
                is NewCardUiEffect.ShowMessage -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_LONG).show()
                }

                NewCardUiEffect.OpenReview -> {
                    currentStep = NewCardFlowStep.Review
                }

                NewCardUiEffect.CloseFlow -> {
                    navigator.goBack()
                }
            }
        }
    }

    when (currentStep) {
        NewCardFlowStep.Input -> {
            NewCardInputStepScreen(
                state = uiState,
                onIntent = vm::onIntent,
                onGenerate = { vm.onIntent(NewCardUiIntent.GenerateClicked) },
                onNavigateBack = { navigator.goBack() },
                onCreateDeck = { navigator.navigateTo(NewDeckRoute()) },
            )
        }

        NewCardFlowStep.Review -> {
            NewCardReviewScreen(
                state = uiState,
                onIntent = vm::onIntent,
                onNavigateBack = { currentStep = NewCardFlowStep.Input },
            )
        }
    }
}
