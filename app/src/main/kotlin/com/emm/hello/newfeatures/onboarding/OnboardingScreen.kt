package com.emm.hello.newfeatures.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.emm.hello.R
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.instrumentAccent
import com.emm.hello.core.theme.instrumentBg
import com.emm.hello.core.theme.instrumentFaint
import com.emm.hello.core.theme.instrumentMuted
import com.emm.hello.core.theme.instrumentOnBg
import com.emm.hello.core.theme.instrumentSurface2
import com.emm.hello.core.theme.geist
import com.emm.hello.core.theme.instrumentSerif
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HButtonVariant

private val dotSize = 8.dp
private val dotActiveWidth = 24.dp
private val dotSpacing = 6.dp
private val illustrationSize = 160.dp
private val pageHorizontalPadding = 32.dp

@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    pagerState: PagerState,
    onIntent: (OnboardingUiIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = instrumentBg,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars),
        ) {
            SkipRow(
                onSkip = { onIntent(OnboardingUiIntent.SkipClicked) },
            )

            // Pager — syncs user swipes back to the ViewModel via PageChanged intent
            LaunchedEffect(pagerState) {
                snapshotFlow { pagerState.currentPage }.collect { page ->
                    onIntent(OnboardingUiIntent.PageChanged(page))
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { pageIndex ->
                val page = state.pages[pageIndex]
                OnboardingPageContent(
                    page = page,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            PageDotIndicator(
                pageCount = state.pages.size,
                currentPage = state.currentPage,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            Spacer(modifier = Modifier.height(32.dp))

            PrimaryCtaButton(
                isLastPage = state.isLastPage,
                onIntent = onIntent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = pageHorizontalPadding),
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Skip row — top-right text button
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SkipRow(onSkip: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        HButton(
            text = stringResource(R.string.onboarding_skip),
            onClick = onSkip,
            variant = HButtonVariant.Ghost,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Individual page content — illustration + title + body
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = pageHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        OnboardingIllustrationArt(illustration = page.illustration)

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = stringResource(page.titleRes),
            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = instrumentSerif),
            color = instrumentOnBg,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(page.bodyRes),
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = geist),
            color = instrumentMuted,
            textAlign = TextAlign.Center,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Illustration — transparent line-art on the ember disc
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OnboardingIllustrationArt(
    illustration: OnboardingIllustration,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(illustrationSize)
            .clip(CircleShape)
            .background(instrumentSurface2),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(illustration.drawableRes),
            contentDescription = null,
            modifier = Modifier.size(illustrationSize),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dot indicator — horizontal row of pills
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PageDotIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    val indicatorDescription = stringResource(
        R.string.onboarding_page_indicator_description,
        currentPage + 1,
        pageCount,
    )
    Row(
        modifier = modifier.semantics { contentDescription = indicatorDescription },
        horizontalArrangement = Arrangement.spacedBy(dotSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val isActive = index == currentPage
            val width = if (isActive) dotActiveWidth else dotSize
            Box(
                modifier = Modifier
                    .size(width = width, height = dotSize)
                    .clip(CircleShape)
                    .background(if (isActive) instrumentAccent else instrumentFaint),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Primary CTA — label changes on the last page
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PrimaryCtaButton(
    isLastPage: Boolean,
    onIntent: (OnboardingUiIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = if (isLastPage) {
        stringResource(R.string.onboarding_cta_start)
    } else {
        stringResource(R.string.onboarding_cta_next)
    }
    val intent = if (isLastPage) OnboardingUiIntent.FinishClicked else OnboardingUiIntent.NextClicked

    HButton(
        text = label,
        onClick = { onIntent(intent) },
        variant = HButtonVariant.Accent,
        full = true,
        modifier = modifier,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

@PreviewLightDark
@Composable
private fun OnboardingScreenPreview() {
    HelloTheme {
        OnboardingScreen(
            state = OnboardingUiState(),
            pagerState = rememberPagerState { OnboardingPage.entries.size },
            onIntent = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun OnboardingScreenLastPagePreview() {
    HelloTheme {
        val state = OnboardingUiState(currentPage = OnboardingPage.entries.lastIndex)
        OnboardingScreen(
            state = state,
            pagerState = rememberPagerState(
                initialPage = OnboardingPage.entries.lastIndex,
                pageCount = { OnboardingPage.entries.size },
            ),
            onIntent = {},
        )
    }
}
