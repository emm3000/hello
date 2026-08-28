package com.emm.hello.newfeatures.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.hello.R
import com.emm.hello.core.theme.pageBackground
import com.emm.hello.core.theme.inkMuted
import com.emm.hello.core.theme.ink
import com.emm.hello.core.theme.spacing
import com.emm.hello.core.ui.AlertVariant
import com.emm.hello.core.ui.HAlert
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HButtonSize
import com.emm.hello.core.ui.HButtonVariant
import com.emm.hello.core.ui.HWizTop
import com.emm.hello.newfeatures.card.validation.IssueTextMapper

private const val REVIEW_STEP_NUMBER = 2
private const val TOTAL_STEP_COUNT = 2

@Composable
fun NewCardReviewScreen(
    state: NewCardUiState,
    onIntent: (NewCardUiIntent) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val issueTextMapper = IssueTextMapper()
    val hasPreview = state.learningNotePreview != null

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = pageBackground,
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            HWizTop(
                currentStep = REVIEW_STEP_NUMBER,
                totalSteps = TOTAL_STEP_COUNT,
                onBack = onNavigateBack,
                subtitle = stringResource(R.string.new_card_wizard_subtitle),
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    start = MaterialTheme.spacing.screenGutter,
                    end = MaterialTheme.spacing.screenGutter,
                    top = 12.dp,
                    bottom = 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                when {
                    hasPreview -> {
                        item {
                            ResultPreviewSection(
                                state = state,
                                keyboardController = keyboardController,
                                onIntent = onIntent,
                            )
                        }
                    }

                    state.isLoading -> {
                        item { LoadingPreviewSkeleton(word = state.word) }
                    }

                    state.error?.quotaResetAt != null -> {
                        item {
                            QuotaExceededState(
                                word = state.word,
                                resetAt = state.error.quotaResetAt,
                                onCreateManually = onNavigateBack,
                                onNotifyTomorrow = onNavigateBack,
                            )
                        }
                    }

                    state.error != null -> {
                        item {
                            HAlert(
                                title = state.error.title,
                                description = state.error.localizedDescription(issueTextMapper),
                                variant = AlertVariant.Destructive,
                            )
                        }
                        item {
                            HButton(
                                text = stringResource(R.string.retry_action),
                                onClick = { onIntent(NewCardUiIntent.GenerateClicked) },
                                variant = HButtonVariant.Secondary,
                                size = HButtonSize.Md,
                                full = true,
                            )
                        }
                    }

                    else -> {
                        item { ReviewEmptyState() }
                    }
                }
            }

            if (hasPreview) {
                NewCardBottomBar {
                    HButton(
                        text = stringResource(R.string.save_in_deck, state.deckSelected?.name.orEmpty()),
                        onClick = {
                            keyboardController?.hide()
                            onIntent(NewCardUiIntent.SaveClicked)
                        },
                        variant = HButtonVariant.Accent,
                        size = HButtonSize.Lg,
                        full = true,
                        enabled = !state.isLoading && state.canSavePreview,
                        isLoading = state.isLoading,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewEmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.review_empty_title),
            fontWeight = FontWeight.Normal,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            color = ink,
        )
        Text(
            text = stringResource(R.string.review_empty_description),
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            color = inkMuted,
        )
    }
}
