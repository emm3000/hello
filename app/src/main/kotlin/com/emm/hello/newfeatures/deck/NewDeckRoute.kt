package com.emm.hello.newfeatures.deck

import android.content.res.Resources
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.emm.hello.navigation.Navigator
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
data class NewDeckRoute(val deckId: String? = null) : NavKey

@Composable
fun NewDeckDestination(navigator: Navigator, deckId: String? = null) {
    val formMode = if (deckId != null) DeckFormMode.Edit(deckId) else DeckFormMode.Create
    val vm: NewDeckViewModel = koinViewModel(
        parameters = { parametersOf(formMode) }
    )
    val uiState by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources: Resources = LocalResources.current

    LaunchedEffect(Unit) {
        vm.effect.collect { effect ->
            when (effect) {
                NewDeckUiEffect.NavigateBack -> navigator.goBack()
                NewDeckUiEffect.DeckDeleted -> navigator.goBack()
                is NewDeckUiEffect.ShowMessage -> {
                    Toast.makeText(context, resources.getString(effect.messageRes), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    NewDeckScreen(
        onNavigateBack = { navigator.goBack() },
        state = uiState,
        onIntent = vm::onIntent,
    )
}
