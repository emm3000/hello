package com.emm.hello.newfeatures

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.emm.hello.core.ui.AlertVariant
import com.emm.hello.core.ui.ButtonVariant
import com.emm.hello.core.ui.HAlert
import com.emm.hello.core.ui.HButton
import com.emm.hello.newfeatures.card.cardDetailRoute
import com.emm.hello.newfeatures.card.newCardRoute
import com.emm.hello.newfeatures.dashboard.DashboardRoute
import com.emm.hello.newfeatures.dashboard.dashboard
import com.emm.hello.newfeatures.deck.deckDetailRoute
import com.emm.hello.newfeatures.deck.newDeckRoute
import com.emm.hello.newfeatures.pairing.pairingRoute
import com.emm.hello.newfeatures.study.study
import com.emm.hello.startup.AppStartupState
import com.emm.hello.startup.AppStartupViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun NewRoot() {
    val startupViewModel: AppStartupViewModel = koinViewModel()
    when (val startupState = startupViewModel.state.collectAsStateWithLifecycle().value) {
        AppStartupState.Initializing -> StartupLoadingScreen()
        is AppStartupState.Error -> StartupErrorScreen(
            message = startupState.message,
            onRetry = startupViewModel::retry,
        )
        AppStartupState.Ready -> AppNavigation()
    }
}

@Composable
private fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = DashboardRoute,
        modifier = Modifier.background(MaterialTheme.colorScheme.background)
    ) {
        dashboard(navController)
        study(navController)
        newCardRoute(navController)
        newDeckRoute(navController)
        deckDetailRoute(navController)
        cardDetailRoute(navController)
        pairingRoute(navController)
    }
}

@Composable
private fun StartupLoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Inicializando identidad y sincronización...",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Esperando que la app quede lista antes de abrir el dashboard.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StartupErrorScreen(
    message: String,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            HAlert(
                title = "No se pudo inicializar la app",
                description = message,
                variant = AlertVariant.Destructive,
            )
            HButton(
                text = "Reintentar",
                onClick = onRetry,
                variant = ButtonVariant.Default,
            )
        }
    }
}
