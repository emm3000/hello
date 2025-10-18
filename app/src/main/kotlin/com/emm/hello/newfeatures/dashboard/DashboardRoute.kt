package com.emm.hello.newfeatures.dashboard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.emm.hello.newfeatures.card.NewCardRoute
import com.emm.hello.newfeatures.deck.DeckDetailRoute
import com.emm.hello.newfeatures.deck.NewDeckRoute
import com.emm.hello.newfeatures.study.StudyRoute
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel

@Serializable
object DashboardRoute

fun NavGraphBuilder.dashboard(navController: NavController) {
    composable<DashboardRoute> {
        val vm: DashboardViewModel = koinViewModel()

        val state: DashboardUiState by vm.state.collectAsStateWithLifecycle()

        val ctx = LocalContext.current

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = {}
        )

        LaunchedEffect(Unit) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val checkSelfPermission = ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
                val hasPermission: Boolean = checkSelfPermission == PackageManager.PERMISSION_GRANTED
                if (hasPermission.not()) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        DashboardScreen(
            state = state,
            newCard = { navController.navigate(NewCardRoute) },
            onDeckDetail = { navController.navigate(DeckDetailRoute(it)) },
            onStartReview = { navController.navigate(StudyRoute) },
            onCreateDeck = { navController.navigate(NewDeckRoute) },
            onNavigateToQuotes = { navController.navigate(QuoteRoute) },
        )
    }
}