package com.emm.hello.newfeatures.card

import android.widget.Toast
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
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel

@Serializable
data object NewCardRoute : NavKey

private enum class NewCardFlowStep {
    Mode,
    Input,
    Review,
}

@Composable
fun NewCardDestination(navigator: Navigator) {
    val vm: NewCardViewModel = koinViewModel()
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var currentStep by rememberSaveable { mutableStateOf(NewCardFlowStep.Mode) }

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
        NewCardFlowStep.Mode -> {
            NewCardModeScreen(
                selectedMode = uiState.typeView,
                onModeSelected = { vm.onIntent(NewCardUiIntent.TypeViewSelected(it)) },
                onContinue = { currentStep = NewCardFlowStep.Input },
                onNavigateBack = { navigator.goBack() },
            )
        }

        NewCardFlowStep.Input -> {
            NewCardInputStepScreen(
                state = uiState,
                onIntent = vm::onIntent,
                onGenerate = { vm.onIntent(NewCardUiIntent.GenerateClicked) },
                onNavigateBack = { currentStep = NewCardFlowStep.Mode },
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
