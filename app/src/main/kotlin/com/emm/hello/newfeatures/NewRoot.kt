package com.emm.hello.newfeatures

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.emm.hello.core.ui.AlertVariant
import com.emm.hello.core.ui.HButtonVariant
import com.emm.hello.core.ui.HAlert
import com.emm.hello.core.ui.HButton
import com.emm.hello.navigation.LaunchDestination
import com.emm.hello.navigation.Navigator
import com.emm.hello.newfeatures.card.CardDetailDestination
import com.emm.hello.newfeatures.card.CardDetailRoute
import com.emm.hello.newfeatures.card.EditFlashcardDestination
import com.emm.hello.newfeatures.card.EditFlashcardRoute
import com.emm.hello.newfeatures.capture.CaptureDestination
import com.emm.hello.newfeatures.capture.CaptureRoute
import com.emm.hello.newfeatures.today.TodayDestination
import com.emm.hello.newfeatures.today.TodayRoute
import com.emm.hello.newfeatures.deck.DecksDestination
import com.emm.hello.newfeatures.deck.DecksRoute
import com.emm.hello.newfeatures.deck.NewDeckDestination
import com.emm.hello.newfeatures.deck.NewDeckRoute
import com.emm.hello.newfeatures.library.LibraryDestination
import com.emm.hello.newfeatures.library.LibraryRoute
import com.emm.hello.newfeatures.onboarding.OnboardingDestination
import com.emm.hello.newfeatures.onboarding.OnboardingRoute
import com.emm.hello.newfeatures.settings.SettingsDestination
import com.emm.hello.newfeatures.settings.SettingsRoute
import com.emm.hello.newfeatures.study.StudyDestination
import com.emm.hello.newfeatures.study.StudyRoute
import com.emm.hello.newfeatures.suggest.SuggestDestination
import com.emm.hello.newfeatures.suggest.SuggestRoute
import com.emm.hello.startup.AppStartupState
import com.emm.hello.startup.AppStartupViewModel
import org.koin.androidx.compose.koinViewModel

private const val NAV_TRANSITION_DURATION_MS = 350

@Composable
fun NewRoot(
    launchDestination: LaunchDestination? = null,
    onLaunchDestinationConsumed: () -> Unit = {},
) {
    val startupViewModel: AppStartupViewModel = koinViewModel()
    when (val startupState = startupViewModel.state.collectAsStateWithLifecycle().value) {
        AppStartupState.Initializing -> StartupLoadingScreen()
        is AppStartupState.Error -> StartupErrorScreen(
            message = startupState.message,
            onRetry = startupViewModel::retry,
        )
        is AppStartupState.Ready -> AppNavigation(
            hasSeenWelcome = startupState.hasSeenWelcome,
            launchDestination = launchDestination,
            onLaunchDestinationConsumed = onLaunchDestinationConsumed,
        )
    }
}

@Composable
private fun AppNavigation(
    hasSeenWelcome: Boolean,
    launchDestination: LaunchDestination?,
    onLaunchDestinationConsumed: () -> Unit,
) {
    val startKey = remember(hasSeenWelcome) { if (hasSeenWelcome) TodayRoute else OnboardingRoute }
    val backStack = rememberNavBackStack(startKey)
    val navigator = remember(backStack) { Navigator(backStack) }

    LaunchedEffect(launchDestination) {
        if (launchDestination == null) return@LaunchedEffect
        if (hasSeenWelcome) {
            navigator.resetTo(TodayRoute, launchDestination.toNavKey())
        }
        onLaunchDestinationConsumed()
    }

    NavDisplay(
        backStack = backStack,
        onBack = navigator::goBack,
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
        transitionSpec = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(NAV_TRANSITION_DURATION_MS),
            ) togetherWith
                slideOutHorizontally(
                    targetOffsetX = { -it / 3 },
                    animationSpec = tween(NAV_TRANSITION_DURATION_MS),
                )
        },
        popTransitionSpec = {
            slideInHorizontally(
                initialOffsetX = { -it / 3 },
                animationSpec = tween(NAV_TRANSITION_DURATION_MS),
            ) togetherWith
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(NAV_TRANSITION_DURATION_MS),
                )
        },
        predictivePopTransitionSpec = {
            slideInHorizontally(
                initialOffsetX = { -it / 3 },
                animationSpec = tween(NAV_TRANSITION_DURATION_MS),
            ) togetherWith
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(NAV_TRANSITION_DURATION_MS),
                )
        },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<OnboardingRoute> { OnboardingDestination(navigator) }
            entry<TodayRoute> { TodayDestination(navigator) }
            entry<StudyRoute> { key -> StudyDestination(navigator, key.deckId) }
            entry<CaptureRoute> { CaptureDestination(navigator) }
            entry<SuggestRoute> { SuggestDestination(navigator) }
            entry<LibraryRoute> { LibraryDestination(navigator) }
            entry<NewDeckRoute> { key -> NewDeckDestination(navigator, key.deckId) }
            entry<CardDetailRoute> { key -> CardDetailDestination(navigator, key.cardId, key.deckId) }
            entry<EditFlashcardRoute> { key -> EditFlashcardDestination(navigator, key.cardId) }
            entry<SettingsRoute> { SettingsDestination(navigator) }
            entry<DecksRoute> { DecksDestination(navigator) }
        }
    )
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
                text = "Preparando tus datos locales...",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Enseguida abrimos tu biblioteca local.",
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
                title = "Couldn't initialize the app",
                description = message,
                variant = AlertVariant.Destructive,
            )
            HButton(
                text = "Retry",
                onClick = onRetry,
                variant = HButtonVariant.Primary,
            )
        }
    }
}

private fun LaunchDestination.toNavKey(): NavKey = when (this) {
    LaunchDestination.StudyDue -> StudyRoute(deckId = null)
}
