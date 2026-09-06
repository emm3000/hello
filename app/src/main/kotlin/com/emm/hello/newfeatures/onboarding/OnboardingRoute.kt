package com.emm.hello.newfeatures.onboarding

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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

    val permissionLauncher: ManagedActivityResultLauncher<String, Boolean> = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { vm.onIntent(OnboardingUiIntent.NotificationPermissionSettled) }

    BackHandler { vm.onIntent(OnboardingUiIntent.BackPressed) }

    LaunchedEffect(Unit) {
        vm.effect.collect { effect ->
            when (effect) {
                is OnboardingUiEffect.NavigateToToday -> navigator.replaceAll(TodayRoute)
                is OnboardingUiEffect.RequestNotificationPermission -> {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                is OnboardingUiEffect.CloseOnboarding -> navigator.goBack()
            }
        }
    }

    OnboardingScreen(onIntent = vm::onIntent)
}
