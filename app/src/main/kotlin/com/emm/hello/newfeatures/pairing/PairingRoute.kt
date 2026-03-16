package com.emm.hello.newfeatures.pairing

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
object PairingRoute

fun NavGraphBuilder.pairingRoute(navController: NavController) {
    composable<PairingRoute> {
        val vm: PairingViewModel = koinViewModel()
        val uiState = vm.uiState.collectAsStateWithLifecycle()
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            vm.effect.collect { effect ->
                when (effect) {
                    is PairingUiEffect.ShowMessage -> {
                        Toast.makeText(context, effect.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        PairingScreen(
            state = uiState.value,
            onBack = { navController.popBackStack() },
            onIntent = vm::onIntent,
        )
    }
}
