package com.emm.hello.newfeatures.study

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
data class StudyRoute(val deckId: String)

fun NavGraphBuilder.study(navController: NavController) {
    composable<StudyRoute> {
        val route = it.toRoute<StudyRoute>()
        val vm: StudyViewModel = koinViewModel(
            parameters = { parametersOf(route.deckId) }
        )

        StudyScreen(
            onNavigateBack = { navController.popBackStack() },
            onReviewAnswer = vm::onProcess,
            state = vm.state
        )
    }
}
