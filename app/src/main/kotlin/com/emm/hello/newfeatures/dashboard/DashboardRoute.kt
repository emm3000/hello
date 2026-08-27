package com.emm.hello.newfeatures.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.emm.hello.navigation.Navigator
import com.emm.hello.newfeatures.capture.CaptureRoute
import com.emm.hello.newfeatures.library.LibraryRoute
import com.emm.hello.newfeatures.settings.SettingsRoute
import com.emm.hello.newfeatures.study.StudyRoute
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel

@Serializable
object DashboardRoute : NavKey

@Composable
fun DashboardDestination(navigator: Navigator) {
    val vm: DashboardViewModel = koinViewModel()
    val uiState: DashboardUiState by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        vm.effect.collect { effect ->
            when (effect) {
                is NavigateToStudy -> navigator.navigateTo(StudyRoute(effect.deckId))
            }
        }
    }

    DashboardScreen(
        state = uiState,
        onCapture = { navigator.navigateTo(CaptureRoute) },
        onStudy = { vm.onIntent(StudyClicked) },
        onSettings = { navigator.navigateTo(SettingsRoute) },
        onLibrary = { navigator.navigateTo(LibraryRoute) },
        onVisible = { vm.onIntent(ScreenVisible) },
    )
}
