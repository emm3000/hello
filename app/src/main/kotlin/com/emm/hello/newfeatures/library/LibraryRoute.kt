package com.emm.hello.newfeatures.library

import android.widget.Toast
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.emm.hello.R
import com.emm.hello.navigation.Navigator
import com.emm.hello.newfeatures.capture.CaptureRoute
import com.emm.hello.newfeatures.card.CardDetailRoute
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel

@Serializable
data object LibraryRoute : NavKey

@Composable
fun LibraryDestination(navigator: Navigator) {
    val vm: LibraryViewModel = koinViewModel()
    val uiState by vm.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val undoLabel: String = stringResource(R.string.undo_action_label)
    val cardDeletedMessage: String = stringResource(R.string.undo_card_deleted_message)

    LaunchedEffect(Unit) {
        vm.effect.collect { effect ->
            when (effect) {
                is LibraryUiEffect.OpenCard -> navigator.navigateTo(
                    CardDetailRoute(cardId = effect.cardId, deckId = effect.deckId),
                )
                LibraryUiEffect.OpenCapture -> navigator.navigateTo(CaptureRoute)
                is LibraryUiEffect.ShowMessage -> {
                    Toast.makeText(context, context.getString(effect.messageRes), Toast.LENGTH_LONG).show()
                }
                is LibraryUiEffect.ShowUndoCardDeleted -> {
                    scope.launch {
                        val result: SnackbarResult = snackbarHostState.showSnackbar(
                            message = cardDeletedMessage,
                            actionLabel = undoLabel,
                            duration = SnackbarDuration.Long,
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            vm.onIntent(
                                LibraryUiIntent.UndoDeleteCard(
                                    flashcardId = effect.flashcardId,
                                    deletedAt = effect.deletedAt,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }

    LibraryScreen(
        state = uiState,
        snackbarHostState = snackbarHostState,
        onBack = navigator::goBack,
        onIntent = vm::onIntent,
    )
}
