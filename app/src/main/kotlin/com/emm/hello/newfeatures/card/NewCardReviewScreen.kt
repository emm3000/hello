package com.emm.hello.newfeatures.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emm.hello.R
import com.emm.hello.newfeatures.card.validation.IssueTextMapper
import com.emm.hello.core.ui.AlertVariant
import com.emm.hello.core.ui.HAlert

private const val REVIEW_STEP_NUMBER = 3
private const val TOTAL_STEP_COUNT = 3

@Composable
fun NewCardReviewScreen(
    state: NewCardUiState,
    onIntent: (NewCardUiIntent) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val issueTextMapper = IssueTextMapper()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.review_screen_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(
                                R.string.step_counter,
                                REVIEW_STEP_NUMBER,
                                TOTAL_STEP_COUNT,
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(
                start = 0.dp,
                top = innerPadding.calculateTopPadding(),
                end = 0.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when {
                state.learningNotePreview != null -> {
                    item {
                        ResultPreviewSection(
                            state = state,
                            keyboardController = keyboardController,
                            onIntent = onIntent,
                        )
                    }
                }

                state.isLoading -> {
                    item { LoadingPreviewSkeleton() }
                }

                state.error != null -> {
                    item {
                        HAlert(
                            title = state.error.title,
                            description = state.error.localizedDescription(issueTextMapper),
                            variant = AlertVariant.Destructive,
                        )
                    }
                }

                else -> {
                    item {
                        HAlert(
                            title = stringResource(R.string.review_empty_title),
                            description = stringResource(R.string.review_empty_description),
                            variant = AlertVariant.Default,
                        )
                    }
                }
            }
        }
    }
}
