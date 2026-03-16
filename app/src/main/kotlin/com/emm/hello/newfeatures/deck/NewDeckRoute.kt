package com.emm.hello.newfeatures.deck

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
object NewDeckRoute

fun NavGraphBuilder.newDeckRoute(navController: NavController) {
    composable<NewDeckRoute> {
        val vm: NewDeckViewModel = koinViewModel()
        val state = vm.state.collectAsStateWithLifecycle()
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            vm.effect.collect { effect ->
                when (effect) {
                    NewDeckUiEffect.NavigateBack -> navController.popBackStack()
                    is NewDeckUiEffect.ShowMessage -> {
                        Toast.makeText(context, effect.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        NewDeckScreen(
            onNavigateBack = { navController.popBackStack() },
            state = state.value,
            onIntent = vm::onIntent,
        )
    }
}
