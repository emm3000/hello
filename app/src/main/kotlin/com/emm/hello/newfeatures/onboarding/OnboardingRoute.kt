package com.emm.hello.newfeatures.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.emm.hello.navigation.Navigator
import com.emm.hello.newfeatures.dashboard.DashboardRoute
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel

@Serializable
data object OnboardingRoute : NavKey

@Composable
fun OnboardingDestination(navigator: Navigator) {
    val vm: OnboardingViewModel = koinViewModel()
    val state: OnboardingUiState by vm.state.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState(pageCount = { state.pages.size })

    // Intercept system back — ViewModel decides: page back or close
    BackHandler { vm.onIntent(OnboardingUiIntent.BackPressed) }

    // Collect one-shot effects from the ViewModel
    LaunchedEffect(Unit) {
        vm.effect.collect { effect ->
            when (effect) {
                is OnboardingUiEffect.ScrollToPage -> {
                    pagerState.animateScrollToPage(effect.page)
                }
                is OnboardingUiEffect.NavigateToDashboard -> {
                    navigator.replaceAll(DashboardRoute)
                }
                is OnboardingUiEffect.CloseOnboarding -> {
                    navigator.goBack()
                }
            }
        }
    }

    OnboardingScreen(
        state = state,
        pagerState = pagerState,
        onIntent = vm::onIntent,
    )
}
