package com.emm.hello.newfeatures.card

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel

@Serializable
object NewCardRoute

fun NavGraphBuilder.newCardRoute(navController: NavController) {
    composable<NewCardRoute> {
        val vm: NewCardViewModel = koinViewModel()

        NewCardScreen(
            onNavigateBack = { navController.popBackStack() },
            state = vm.state,
            onAction = vm::onAction,
        )
    }
}