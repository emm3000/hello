package com.emm.hello.newfeatures.card

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.emm.hello.navigation.Navigator
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
data class EditFlashcardRoute(val cardId: String, val deckId: String) : NavKey

@Composable
fun EditFlashcardDestination(navigator: Navigator, cardId: String, deckId: String) {
    val vm: EditFlashcardViewModel = koinViewModel(
        parameters = { parametersOf(cardId, deckId) }
    )

    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        vm.effect.collect { effect ->
            when (effect) {
                EditFlashcardUiEffect.NavigateBack -> navigator.goBack()
                is EditFlashcardUiEffect.ShowMessage -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    EditFlashcardScreen(
        state = uiState,
        onNavigateBack = { navigator.goBack() },
        onIntent = vm::onIntent,
    )
}