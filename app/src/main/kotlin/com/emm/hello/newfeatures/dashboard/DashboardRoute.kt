package com.emm.hello.newfeatures.dashboard

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.emm.hello.R
import com.emm.hello.navigation.Navigator
import com.emm.hello.newfeatures.capture.CaptureRoute
import com.emm.hello.newfeatures.deck.DeckDetailRoute
import com.emm.hello.newfeatures.deck.NewDeckRoute
import com.emm.hello.newfeatures.library.LibraryRoute
import com.emm.hello.newfeatures.settings.SettingsRoute
import com.emm.hello.newfeatures.study.StudyRoute
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel

@Serializable
object DashboardRoute : NavKey

@Composable
fun DashboardDestination(navigator: Navigator) {
    val vm: DashboardViewModel = koinViewModel()

    val uiState: DashboardUiState by vm.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val undoLabel = stringResource(R.string.undo_action_label)
    val deckDeletedTemplate = stringResource(R.string.undo_deck_deleted_message)

    LaunchedEffect(Unit) {
        vm.effect.collect { effect ->
            when (effect) {
                is NavigateToStudy -> navigator.navigateTo(StudyRoute(effect.deckId))
                is ShowUndoDeckDeleted -> {
                    val message = deckDeletedTemplate.format(effect.deckName)
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = message,
                            actionLabel = undoLabel,
                            duration = SnackbarDuration.Long,
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            vm.onIntent(UndoDeleteDeck(deckId = effect.deckId, deletedAt = effect.deletedAt))
                        }
                    }
                }
            }
        }
    }

    DashboardScreen(
        state = uiState,
        snackbarHostState = snackbarHostState,
        newCard = { navigator.navigateTo(CaptureRoute) },
        onStudy = { vm.onIntent(StudyClicked) },
        onDeckDetail = { navigator.navigateTo(DeckDetailRoute(it)) },
        onCreateDeck = { navigator.navigateTo(NewDeckRoute()) },
        onSettings = { navigator.navigateTo(SettingsRoute) },
        onLibrary = { navigator.navigateTo(LibraryRoute) },
        onVisible = { vm.onIntent(ScreenVisible) },
        onSearchQueryChanged = { vm.onIntent(QueryChanged(it)) },
        onTagToggled = { vm.onIntent(TagToggled(it)) },
        onClearFilters = { vm.onIntent(ClearFilters) },
    )
}
