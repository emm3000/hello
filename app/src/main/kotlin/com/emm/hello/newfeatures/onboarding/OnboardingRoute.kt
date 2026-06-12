package com.emm.hello.newfeatures.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import com.emm.hello.navigation.Navigator
import kotlinx.serialization.Serializable

@Serializable
data object OnboardingRoute : NavKey

/**
 * Placeholder destination for OnboardingRoute. Replaced by the full carousel in PR #2.
 */
@Suppress("UnusedParameter")
@Composable
fun OnboardingDestination(navigator: Navigator) {
    // TODO(PR#2): replace with full OnboardingScreen composable
    Box(modifier = Modifier.fillMaxSize())
}
