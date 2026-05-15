package com.emm.hello.newfeatures.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.emm.hello.navigation.Navigator
import com.emm.hello.newfeatures.card.NewCardRoute
import com.emm.hello.newfeatures.deck.DeckDetailRoute
import com.emm.hello.newfeatures.deck.NewDeckRoute
import com.emm.hello.newfeatures.settings.SettingsRoute
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel

@Serializable
object DashboardRoute : NavKey

@Composable
fun DashboardDestination(navigator: Navigator) {
    val vm: DashboardViewModel = koinViewModel()

    val uiState: DashboardUiState by vm.state.collectAsStateWithLifecycle()

    DashboardScreen(
        state = uiState,
        newCard = { navigator.navigateTo(NewCardRoute) },
        onDeckDetail = { navigator.navigateTo(DeckDetailRoute(it)) },
        onCreateDeck = { navigator.navigateTo(NewDeckRoute()) },
        onSettings = { navigator.navigateTo(SettingsRoute) },
        onVisible = { vm.onVisible() },
        onSearchQueryChanged = { vm.onIntent(QueryChanged(it)) },
        onTagToggled = { vm.onIntent(TagToggled(it)) },
        onClearFilters = { vm.onIntent(ClearFilters) },
    )
}
