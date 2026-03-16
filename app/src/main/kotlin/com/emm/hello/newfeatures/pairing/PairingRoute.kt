package com.emm.hello.newfeatures.pairing

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
        PairingScreen(
            state = vm.state,
            onBack = { navController.popBackStack() },
            onRefresh = vm::refreshDevices,
            onCreateCode = vm::createCode,
            onJoinCodeChange = vm::onJoinCodeChange,
            onJoinWithCode = vm::joinWithCode,
            onRevoke = vm::revokeDevice,
        )
    }
}
