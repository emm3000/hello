package com.emm.hello.newfeatures.dashboard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.emm.hello.navigation.Navigator
import com.emm.hello.newfeatures.card.NewCardRoute
import com.emm.hello.newfeatures.deck.DeckDetailRoute
import com.emm.hello.newfeatures.deck.NewDeckRoute
import com.emm.hello.newfeatures.settings.SettingsRoute
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel

@Serializable
object DashboardRoute : NavKey

@Composable
fun DashboardDestination(navigator: Navigator) {
    val vm: DashboardViewModel = koinViewModel()

    val uiState: DashboardUiState by vm.uiState.collectAsStateWithLifecycle()

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
        state = uiState,
        newCard = { navigator.navigateTo(NewCardRoute) },
        onDeckDetail = { navigator.navigateTo(DeckDetailRoute(it)) },
        onCreateDeck = { navigator.navigateTo(NewDeckRoute()) },
        onSettings = { navigator.navigateTo(SettingsRoute) },
        onVisible = { vm.onVisible() },
        onSearchQueryChanged = { vm.onIntent(QueryChanged(it)) },
        onTagToggled = { vm.onIntent(TagToggled(it)) },
        onClearFilters = { vm.onIntent(ClearFilters) },
    )
}
