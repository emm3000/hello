package com.emm.hello.newfeatures.store

import android.content.Context
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
import com.emm.hello.newfeatures.deck.NewDeckRoute
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel

@Serializable
data object StoreRoute : NavKey

@Composable
fun StoreDestination(navigator: Navigator) {
    val vm: StoreViewModel = koinViewModel()
    val uiState: StoreUiState by vm.state.collectAsStateWithLifecycle()
    val context: Context = LocalContext.current
    val resources: Resources = LocalResources.current

    LaunchedEffect(Unit) {
        vm.effect.collect { effect ->
            when (effect) {
                StoreUiEffect.NavigateBack -> navigator.goBack()
                is StoreUiEffect.OpenDeck -> navigator.navigateTo(NewDeckRoute(effect.deckId))
                is StoreUiEffect.ShowMessage -> {
                    Toast.makeText(context, resources.getString(effect.messageRes), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    StoreScreen(
        state = uiState,
        onIntent = vm::onIntent,
    )
}
