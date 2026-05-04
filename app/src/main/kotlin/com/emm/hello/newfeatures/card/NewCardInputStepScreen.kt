package com.emm.hello.newfeatures.card

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emm.data.catalog.staticCategories
import com.emm.hello.R
import com.emm.hello.newfeatures.card.validation.IssueTextMapper
import com.emm.hello.core.audio.rememberSpeechToTextManager
import com.emm.hello.core.theme.metadata
import com.emm.hello.core.theme.spacing
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HSelect
import com.emm.hello.core.ui.SectionBlock
import java.util.Locale

private const val INPUT_STEP_NUMBER = 2
private const val TOTAL_STEP_COUNT = 3

@Composable
fun NewCardInputStepScreen(
    state: NewCardUiState,
    onIntent: (NewCardUiIntent) -> Unit,
    onGenerate: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val showBottomSheet = remember { mutableStateOf(false) }
    val issueTextMapper = remember { IssueTextMapper() }
    val sttManager = rememberSpeechToTextManager { voiceText ->
        onIntent(NewCardUiIntent.WordChanged(voiceText))
    }
    val isListening by sttManager.isListening.collectAsStateWithLifecycle()
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) sttManager.startListening(Locale.US)
        }
    )

    val isGenerateEnabled = remember(state.isLoading, state.deckSelected, state.word, state.aiRequest, state.typeView) {
        val hasWord = state.word.isNotBlank()
        val hasAiRequest = state.aiRequest.isNotBlank()
        val hasDeck = state.deckSelected != null
        val notLoading = !state.isLoading
        when (state.typeView) {
            TypeView.WordOrPhase -> notLoading && hasDeck && hasWord
            TypeView.WithCategories -> notLoading && hasDeck
            TypeView.WithAiHelp -> notLoading && hasDeck && hasAiRequest
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
                        Text(
                            text = stringResource(R.string.input_screen_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(
                                R.string.step_counter,
                                INPUT_STEP_NUMBER,
                                TOTAL_STEP_COUNT,
                            ),
                            style = MaterialTheme.typography.metadata,
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
        bottomBar = {
            NewCardBottomBar {
                HButton(
                    text = stringResource(R.string.generate_card),
                    onClick = {
                        keyboardController?.hide()
                        onGenerate()
                    },
                    enabled = isGenerateEnabled,
                    isLoading = state.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MaterialTheme.spacing.lg),
            contentPadding = PaddingValues(
                start = 0.dp,
                top = innerPadding.calculateTopPadding(),
                end = 0.dp,
                bottom = innerPadding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg + MaterialTheme.spacing.xs),
        ) {
            item {
                SectionBlock(
                    title = stringResource(R.string.input_section_title),
                    description = when (state.typeView) {
                        TypeView.WordOrPhase -> stringResource(R.string.input_section_word_description)
                        TypeView.WithCategories -> stringResource(R.string.input_section_category_description)
                        TypeView.WithAiHelp -> stringResource(R.string.input_section_ai_description)
                    },
                ) {
                    NewCardInputSection(
                        state = state,
                        isListening = isListening,
                        onIntent = onIntent,
                        onToggleVoiceInput = {
                            if (isListening) {
                                sttManager.stopListening()
                            } else {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasPermission) {
                                    sttManager.startListening(Locale.US)
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        onShowCategoryPicker = { showBottomSheet.value = true },
                    )
                }
            }

            item {
                SectionBlock(
                    title = stringResource(R.string.destination_section_title),
                    description = stringResource(R.string.destination_section_description),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)) {
                        HSelect(
                            items = state.decks,
                            itemSelected = state.deckSelected,
                            enabled = !state.isLoading,
                            onItemSelected = { onIntent(NewCardUiIntent.DeckSelected(it)) },
                            label = stringResource(R.string.deck_label),
                            placeholder = stringResource(R.string.select_deck_placeholder),
                            itemLabel = { it.name },
                        )
                        LabeledCheckbox(
                            label = stringResource(R.string.default_deck_checkbox),
                            checked = state.isCheck,
                            isEnabled = state.deckSelected != null,
                            onCheckedChange = { onIntent(NewCardUiIntent.CheckChanged(it)) },
                        )
                    }
                }
            }

            if (state.error != null) {
                item {
                    com.emm.hello.core.ui.HAlert(
                        title = state.error.title,
                        description = state.error.localizedDescription(issueTextMapper),
                        variant = com.emm.hello.core.ui.AlertVariant.Destructive,
                    )
                }
            }
        }
    }

    BottomSheetDialogForPickCategory(
        onDismissRequest = { showBottomSheet.value = it },
        showBottomSheet = showBottomSheet.value,
        accounts = staticCategories,
        selectedCategory = state.category,
        onAction = { onIntent(NewCardUiIntent.CategorySelected(it)) },
    )
}
