package com.emm.hello.newfeatures.card

import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel

@Serializable
data object NewCardRoute

fun NavGraphBuilder.newCardRoute(navController: NavController) {
    composable<NewCardRoute> {
        val vm: NewCardViewModel = koinViewModel()
        val state = vm.state.collectAsStateWithLifecycle()
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            vm.effect.collect { effect ->
                when (effect) {
                    is NewCardUiEffect.ShowMessage -> {
                        Toast.makeText(context, effect.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        NewCardScreen(
            onNavigateBack = { navController.popBackStack() },
            state = state.value,
            onIntent = vm::onIntent,
        )
    }
}
