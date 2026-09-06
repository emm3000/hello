package com.emm.hello.newfeatures.suggest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.emm.domain.suggestion.SuggestedWord
import com.emm.hello.R
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.bricolage
import com.emm.hello.core.theme.cardPeriwinkle
import com.emm.hello.core.theme.ink
import com.emm.hello.core.theme.inkMuted
import com.emm.hello.core.theme.schibsted
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HButtonVariant
import com.emm.hello.core.ui.HChip
import com.emm.hello.core.ui.HEmptyState
import com.emm.hello.core.ui.HLoadingSpinner
import com.emm.hello.core.ui.HTopBar

@Composable
fun SuggestScreen(
    state: SuggestUiState,
    onIntent: (SuggestUiIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = cardPeriwinkle) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 24.dp),
        ) {
            HTopBar(onBack = { onIntent(SuggestUiIntent.BackClicked) })

            when {
                state.isLoading -> SuggestLoadingState(modifier = Modifier.weight(1f))
                state.isOffline -> SuggestOfflineState(onIntent = onIntent, modifier = Modifier.weight(1f))
                state.loadFailed -> SuggestErrorState(onIntent = onIntent, modifier = Modifier.weight(1f))
                state.words.isEmpty() -> SuggestEmptyState(onIntent = onIntent, modifier = Modifier.weight(1f))
                else -> SuggestLoadedContent(state = state, onIntent = onIntent, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SuggestLoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HLoadingSpinner(color = ink)
            Text(
                text = stringResource(R.string.suggest_loading),
                fontFamily = schibsted,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                color = inkMuted,
            )
        }
    }
}

@Composable
private fun SuggestErrorState(onIntent: (SuggestUiIntent) -> Unit, modifier: Modifier = Modifier) {
    HEmptyState(
        modifier = modifier.fillMaxSize(),
        headline = stringResource(R.string.suggest_error_title),
        body = stringResource(R.string.suggest_error_body),
        primaryCta = {
            HButton(
                text = stringResource(R.string.suggest_retry),
                onClick = { onIntent(SuggestUiIntent.Retry) },
                variant = HButtonVariant.Primary,
                full = true,
            )
        },
        ghostCta = {
            HButton(
                text = stringResource(R.string.suggest_not_now),
                onClick = { onIntent(SuggestUiIntent.BackClicked) },
                variant = HButtonVariant.Text,
                full = true,
            )
        },
    )
}

@Composable
private fun SuggestOfflineState(onIntent: (SuggestUiIntent) -> Unit, modifier: Modifier = Modifier) {
    HEmptyState(
        modifier = modifier.fillMaxSize(),
        headline = stringResource(R.string.suggest_offline_title),
        body = stringResource(R.string.suggest_offline_body),
        primaryCta = {
            HButton(
                text = stringResource(R.string.suggest_retry),
                onClick = { onIntent(SuggestUiIntent.Retry) },
                variant = HButtonVariant.Primary,
                full = true,
            )
        },
        ghostCta = {
            HButton(
                text = stringResource(R.string.suggest_not_now),
                onClick = { onIntent(SuggestUiIntent.BackClicked) },
                variant = HButtonVariant.Text,
                full = true,
            )
        },
    )
}

@Composable
private fun SuggestEmptyState(onIntent: (SuggestUiIntent) -> Unit, modifier: Modifier = Modifier) {
    HEmptyState(
        modifier = modifier.fillMaxSize(),
        headline = stringResource(R.string.suggest_empty_title),
        body = stringResource(R.string.suggest_empty_body),
        primaryCta = {
            HButton(
                text = stringResource(R.string.suggest_retry),
                onClick = { onIntent(SuggestUiIntent.Retry) },
                variant = HButtonVariant.Primary,
                full = true,
            )
        },
    )
}

@Composable
private fun SuggestLoadedContent(
    state: SuggestUiState,
    onIntent: (SuggestUiIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.suggest_eyebrow).uppercase(),
                fontFamily = schibsted,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                letterSpacing = 0.08.em,
                color = inkMuted,
            )
            Text(
                text = state.situation,
                fontFamily = bricolage,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp,
                lineHeight = 36.sp,
                color = ink,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.words.forEach { word ->
                    HChip(
                        label = "${word.word} · ${word.translation}",
                        active = word.word in state.selectedWords,
                        onClick = { onIntent(SuggestUiIntent.WordToggled(word.word)) },
                    )
                }
            }
        }

        SuggestDock(state = state, onIntent = onIntent)
    }
}

@Composable
private fun SuggestDock(state: SuggestUiState, onIntent: (SuggestUiIntent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val addLabel: String = if (state.selectedCount > 0) {
            pluralStringResource(R.plurals.suggest_add_selected, state.selectedCount, state.selectedCount)
        } else {
            stringResource(R.string.suggest_pick_some)
        }
        HButton(
            text = addLabel,
            onClick = { onIntent(SuggestUiIntent.AddSelected) },
            enabled = state.canAdd,
            variant = HButtonVariant.Primary,
            full = true,
        )
        HButton(
            text = stringResource(R.string.suggest_not_now),
            onClick = { onIntent(SuggestUiIntent.BackClicked) },
            variant = HButtonVariant.Text,
            full = true,
        )
    }
}

@PreviewLightDark
@Composable
private fun SuggestScreenPreview() {
    HelloTheme {
        SuggestScreen(
            state = SuggestUiState(
                isLoading = false,
                situation = "At a coffee shop",
                words = listOf(
                    SuggestedWord(word = "receipt", translation = "recibo"),
                    SuggestedWord(word = "borrow", translation = "prestar"),
                    SuggestedWord(word = "napkin", translation = "servilleta"),
                ),
                selectedWords = setOf("receipt"),
            ),
            onIntent = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun SuggestScreenLoadingPreview() {
    HelloTheme {
        SuggestScreen(state = SuggestUiState(isLoading = true), onIntent = {})
    }
}

@PreviewLightDark
@Composable
private fun SuggestScreenErrorPreview() {
    HelloTheme {
        SuggestScreen(state = SuggestUiState(isLoading = false, loadFailed = true), onIntent = {})
    }
}

@PreviewLightDark
@Composable
private fun SuggestScreenOfflinePreview() {
    HelloTheme {
        SuggestScreen(state = SuggestUiState(isLoading = false, isOffline = true), onIntent = {})
    }
}

@PreviewLightDark
@Composable
private fun SuggestScreenEmptyPreview() {
    HelloTheme {
        SuggestScreen(
            state = SuggestUiState(isLoading = false, situation = "At a coffee shop"),
            onIntent = {},
        )
    }
}
