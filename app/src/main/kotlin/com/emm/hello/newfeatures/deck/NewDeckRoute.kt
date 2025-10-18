package com.emm.hello.newfeatures.deck

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
        NewDeckScreen(
            onNavigateBack = { navController.popBackStack() },
            state = vm.state,
            onAction = vm::onAction,
        )
    }
}