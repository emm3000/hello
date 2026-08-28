package com.emm.hello.newfeatures.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

    BackHandler { vm.onIntent(OnboardingUiIntent.BackPressed) }

    LaunchedEffect(Unit) {
        vm.effect.collect { effect ->
            when (effect) {
                is OnboardingUiEffect.NavigateToToday -> navigator.replaceAll(TodayRoute)
                is OnboardingUiEffect.CloseOnboarding -> navigator.goBack()
            }
        }
    }

    OnboardingScreen(onIntent = vm::onIntent)
}
