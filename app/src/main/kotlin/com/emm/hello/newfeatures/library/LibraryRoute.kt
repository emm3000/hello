package com.emm.hello.newfeatures.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.emm.hello.navigation.Navigator
import com.emm.hello.newfeatures.capture.CaptureRoute
import com.emm.hello.newfeatures.card.CardDetailRoute
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel

@Serializable
data object LibraryRoute : NavKey

@Composable
fun LibraryDestination(navigator: Navigator) {
    val vm: LibraryViewModel = koinViewModel()
    val uiState by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        vm.effect.collect { effect ->
            when (effect) {
                is LibraryUiEffect.OpenCard -> navigator.navigateTo(
                    CardDetailRoute(cardId = effect.cardId, deckId = effect.deckId),
                )
                LibraryUiEffect.OpenCapture -> navigator.navigateTo(CaptureRoute)
            }
        }
    }

    LibraryScreen(
        state = uiState,
        onBack = navigator::goBack,
        onIntent = vm::onIntent,
    )
}
