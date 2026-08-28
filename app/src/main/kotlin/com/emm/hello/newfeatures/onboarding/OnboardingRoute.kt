package com.emm.hello.newfeatures.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.emm.hello.navigation.Navigator
import com.emm.hello.newfeatures.today.TodayRoute
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel

@Serializable
data object OnboardingRoute : NavKey

@Composable
fun OnboardingDestination(navigator: Navigator) {
    val vm: OnboardingViewModel = koinViewModel()
    val state: OnboardingUiState by vm.state.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState(pageCount = { state.pages.size })

    BackHandler { vm.onIntent(OnboardingUiIntent.BackPressed) }

    LaunchedEffect(Unit) {
        vm.effect.collect { effect ->
            when (effect) {
                is OnboardingUiEffect.ScrollToPage -> {
                    pagerState.animateScrollToPage(effect.page)
                }
                is OnboardingUiEffect.NavigateToHoy -> {
                    navigator.replaceAll(TodayRoute)
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
