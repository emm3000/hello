package com.emm.hello.newfeatures.deck

import android.content.res.Resources
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.emm.hello.R
import com.emm.hello.navigation.Navigator
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel

@Serializable
data object DecksRoute : NavKey

@Composable
fun DecksDestination(navigator: Navigator) {
    val vm: DecksViewModel = koinViewModel()
    val uiState: DecksUiState by vm.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val resources: Resources = LocalResources.current
    val undoLabel: String = stringResource(R.string.undo_action_label)
    val deckDeletedTemplate: String = stringResource(R.string.undo_deck_deleted_message)

    LaunchedEffect(Unit) {
        vm.effect.collect { effect ->
            when (effect) {
                is DecksUiEffect.OpenDeckForm -> navigator.navigateTo(NewDeckRoute(effect.deckId))
                is DecksUiEffect.ShowMessage -> {
                    Toast.makeText(context, resources.getString(effect.messageRes), Toast.LENGTH_LONG).show()
                }
                is DecksUiEffect.ShowUndoDeckDeleted -> {
                    val message: String = deckDeletedTemplate.format(effect.deckName)
                    scope.launch {
                        val result: SnackbarResult = snackbarHostState.showSnackbar(
                            message = message,
                            actionLabel = undoLabel,
                            duration = SnackbarDuration.Long,
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            vm.onIntent(
                                DecksUiIntent.UndoDeleteDeck(
                                    deckId = effect.deckId,
                                    deletedAt = effect.deletedAt,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }

    DecksScreen(
        state = uiState,
        snackbarHostState = snackbarHostState,
        onBack = navigator::goBack,
        onIntent = vm::onIntent,
    )
}
