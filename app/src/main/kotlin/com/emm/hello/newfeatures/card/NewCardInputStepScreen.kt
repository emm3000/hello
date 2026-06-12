package com.emm.hello.newfeatures.card

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emm.domain.catalog.difficult
import com.emm.domain.catalog.staticCategories
import com.emm.hello.R
import com.emm.hello.core.audio.rememberSpeechToTextManager
import com.emm.hello.core.theme.emberAccent
import com.emm.hello.core.theme.emberBad
import com.emm.hello.core.theme.emberBg
import com.emm.hello.core.theme.emberDivider
import com.emm.hello.core.theme.emberMuted
import com.emm.hello.core.theme.emberOnBg
import com.emm.hello.core.theme.emberSurface
import com.emm.hello.core.theme.geist
import com.emm.hello.core.theme.geistMono
import com.emm.hello.core.theme.instrumentSerif
import com.emm.hello.core.ui.AlertVariant
import com.emm.hello.core.ui.HAlert
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HButtonSize
import com.emm.hello.core.ui.HButtonVariant
import com.emm.hello.core.ui.HChip
import com.emm.hello.core.ui.HInput
import com.emm.hello.core.ui.HSectionLabel
import com.emm.hello.core.ui.HWizTop
import com.emm.hello.newfeatures.card.validation.InputField
import com.emm.hello.newfeatures.card.validation.IssueTextMapper
import com.emm.hello.newfeatures.card.validation.IssueUiTarget
import com.emm.domain.validation.ValidationIssue
import kotlinx.coroutines.launch
import java.util.Locale

private val screenHorizontalPadding = 20.dp

@Composable
fun NewCardInputStepScreen(
    state: NewCardUiState,
    onIntent: (NewCardUiIntent) -> Unit,
    onGenerate: () -> Unit,
    onNavigateBack: () -> Unit,
    onCreateDeck: () -> Unit = {},
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val showBottomSheet = remember { mutableStateOf(false) }
    val issueTextMapper = remember { IssueTextMapper() }
    val sttManager = rememberSpeechToTextManager { voiceText ->
        onIntent(NewCardUiIntent.WordChanged(voiceText))
    }
    val isListening by sttManager.isListening.collectAsStateWithLifecycle()
    val sttError by sttManager.error.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val micPermissionDeniedMessage = "Necesitamos permiso de micrófono para dictar."
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                sttManager.startListening(Locale.US)
            } else {
                snackbarScope.launch { snackbarHostState.showSnackbar(micPermissionDeniedMessage) }
            }
        },
    )

    LaunchedEffect(sttError) {
        val message = sttError
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            sttManager.clearError()
        }
    }

    val isGenerateEnabled = run {
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

    val onMicToggle: () -> Unit = {
        if (isListening) {
            sttManager.stopListening()
        } else {
            val hasPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                sttManager.startListening(Locale.US)
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = emberBg,
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            HWizTop(
                currentStep = 2,
                totalSteps = 3,
                onBack = onNavigateBack,
                subtitle = stringResource(R.string.new_card_wizard_subtitle),
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(
                    start = screenHorizontalPadding,
                    end = screenHorizontalPadding,
                    top = 12.dp,
                    bottom = 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                item { InputHero(state.typeView) }

                item {
                    InputPrimaryField(
                        state = state,
                        isListening = isListening,
                        onIntent = onIntent,
                        onMicToggle = onMicToggle,
                        onShowCategoryPicker = { showBottomSheet.value = true },
                        issueTextMapper = issueTextMapper,
                    )
                }

                if (state.typeView == TypeView.WordOrPhase) {
                    item {
                        ContextInputs(
                            state = state,
                            onIntent = onIntent,
                            issueTextMapper = issueTextMapper,
                        )
                    }
                }

                item { DeckPickerSection(state = state, onIntent = onIntent, onCreateDeck = onCreateDeck) }

                item { DifficultySection(state = state, onIntent = onIntent) }

                if (state.error != null) {
                    item {
                        HAlert(
                            title = state.error.title,
                            description = state.error.localizedDescription(issueTextMapper),
                            variant = AlertVariant.Destructive,
                        )
                    }
                }
            }

            Column(modifier = Modifier.imePadding()) {
                SnackbarHost(snackbarHostState)
                NewCardBottomBar {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (state.showQuotaWarning) {
                            Text(
                                text = pluralStringResource(
                                    R.plurals.new_card_quota_remaining_warning,
                                    state.quotaRemaining,
                                    state.quotaRemaining,
                                ),
                                fontFamily = geistMono,
                                fontWeight = FontWeight.Normal,
                                fontSize = 11.sp,
                                letterSpacing = 0.08.em,
                                color = emberMuted,
                            )
                        }
                        HButton(
                            text = stringResource(R.string.continue_action),
                            onClick = {
                                keyboardController?.hide()
                                onGenerate()
                            },
                            variant = HButtonVariant.Accent,
                            size = HButtonSize.Lg,
                            full = true,
                            enabled = isGenerateEnabled,
                            isLoading = state.isLoading,
                        )
                    }
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

@Composable
private fun InputHero(typeView: TypeView) {
    val headlineRes = when (typeView) {
        TypeView.WordOrPhase -> R.string.new_card_input_headline_word
        TypeView.WithCategories -> R.string.new_card_input_headline_category
        TypeView.WithAiHelp -> R.string.new_card_input_headline_ai
    }
    Text(
        text = stringResource(headlineRes),
        fontFamily = instrumentSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 38.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.5).sp,
        color = emberOnBg,
    )
}

@Composable
private fun InputPrimaryField(
    state: NewCardUiState,
    isListening: Boolean,
    onIntent: (NewCardUiIntent) -> Unit,
    onMicToggle: () -> Unit,
    onShowCategoryPicker: () -> Unit,
    issueTextMapper: IssueTextMapper,
) {
    val errorMessage = state.inputValidationIssues.inputMessageOrNull(
        issueTextMapper = issueTextMapper,
        target = InputField.PrimaryText,
    )
    when (state.typeView) {
        TypeView.WordOrPhase -> BigSerifTextField(
            value = state.word,
            onValueChange = { onIntent(NewCardUiIntent.WordChanged(it)) },
            placeholder = stringResource(R.string.new_card_input_word_placeholder),
            enabled = !state.isLoading,
            errorMessage = errorMessage,
            isListening = isListening,
            listeningLabel = stringResource(R.string.listening_placeholder),
            trailing = {
                MicButton(isListening = isListening, onClick = onMicToggle)
            },
        )

        TypeView.WithAiHelp -> BigSerifTextField(
            value = state.aiRequest,
            onValueChange = { onIntent(NewCardUiIntent.AiRequestChanged(it)) },
            placeholder = stringResource(R.string.new_card_input_ai_placeholder),
            enabled = !state.isLoading,
            singleLine = false,
            errorMessage = errorMessage,
        )

        TypeView.WithCategories -> CategoryPickerRow(
            value = state.category.name,
            placeholder = stringResource(R.string.new_card_input_category_placeholder),
            onClick = onShowCategoryPicker,
        )
    }
}

@Composable
private fun BigSerifTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    errorMessage: String? = null,
    isListening: Boolean = false,
    listeningLabel: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val borderColor = if (errorMessage != null) emberBad else emberAccent
    val pulseTransition = rememberInfiniteTransition(label = "input-listening-pulse")
    val borderWidth by pulseTransition.animateFloat(
        initialValue = if (isListening) 1.5f else 1.5f,
        targetValue = if (isListening) 3.0f else 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "input-border-width",
    )
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(emberSurface, RoundedCornerShape(16.dp))
                .border(borderWidth.dp, borderColor, RoundedCornerShape(16.dp))
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontFamily = instrumentSerif,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Normal,
                        fontSize = 26.sp,
                        lineHeight = 32.sp,
                        color = emberMuted,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    singleLine = singleLine,
                    maxLines = if (singleLine) 1 else 4,
                    textStyle = TextStyle(
                        fontFamily = instrumentSerif,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Normal,
                        fontSize = 28.sp,
                        lineHeight = 34.sp,
                        color = emberOnBg,
                    ),
                    cursorBrush = SolidColor(emberAccent),
                    keyboardOptions = KeyboardOptions(
                        imeAction = if (singleLine) ImeAction.Done else ImeAction.Default,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (trailing != null) {
                Spacer(Modifier.size(10.dp))
                trailing()
            }
        }
        if (errorMessage != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = errorMessage,
                fontFamily = geist,
                fontSize = 12.sp,
                color = emberBad,
            )
        } else if (isListening && listeningLabel != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = listeningLabel.uppercase(),
                fontFamily = geistMono,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                letterSpacing = 0.12.em,
                color = emberAccent,
            )
        }
    }
}

@Composable
private fun MicButton(isListening: Boolean, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic-pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mic-scale",
    )
    Box(
        modifier = Modifier
            .size(40.dp)
            .scale(scale)
            .background(
                color = if (isListening) emberAccent else emberSurface,
                shape = CircleShape,
            )
            .border(1.dp, emberAccent, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
            contentDescription = stringResource(R.string.voice_input_desc),
            tint = if (isListening) emberBg else emberAccent,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun CategoryPickerRow(
    value: String,
    placeholder: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(emberSurface, RoundedCornerShape(16.dp))
            .border(1.5.dp, emberAccent, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = value.ifBlank { placeholder },
            fontFamily = instrumentSerif,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Normal,
            fontSize = 26.sp,
            lineHeight = 32.sp,
            color = if (value.isBlank()) emberMuted else emberOnBg,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = emberAccent,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun ContextInputs(
    state: NewCardUiState,
    onIntent: (NewCardUiIntent) -> Unit,
    issueTextMapper: IssueTextMapper,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        HInput(
            value = state.intendedMeaningEs,
            onValueChange = { onIntent(NewCardUiIntent.IntendedMeaningChanged(it)) },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.intended_meaning_label),
            placeholder = stringResource(R.string.intended_meaning_placeholder),
            supportingText = state.inputWarningIssues.inputMessageOrNull(
                issueTextMapper = issueTextMapper,
                target = InputField.Disambiguation,
            ) ?: stringResource(R.string.intended_meaning_supporting_text),
            errorMessage = state.inputValidationIssues.inputMessageOrNull(
                issueTextMapper = issueTextMapper,
                target = InputField.Disambiguation,
            ),
        )
        HInput(
            value = state.contextSentence,
            onValueChange = { onIntent(NewCardUiIntent.ContextSentenceChanged(it)) },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.context_sentence_label),
            placeholder = stringResource(R.string.context_sentence_placeholder),
            supportingText = state.inputWarningIssues.inputMessageOrNull(
                issueTextMapper = issueTextMapper,
                target = InputField.ContextSentence,
            ) ?: stringResource(R.string.context_sentence_supporting_text),
            errorMessage = state.inputValidationIssues.inputMessageOrNull(
                issueTextMapper = issueTextMapper,
                target = InputField.ContextSentence,
            ),
        )
    }
}

@Composable
private fun DeckPickerSection(
    state: NewCardUiState,
    onIntent: (NewCardUiIntent) -> Unit,
    onCreateDeck: () -> Unit,
) {
    Column {
        HSectionLabel(label = stringResource(R.string.new_card_input_save_in_label))
        Spacer(Modifier.height(10.dp))
        DeckPickerRow(
            decks = state.decks,
            selected = state.deckSelected,
            enabled = !state.isLoading,
            onSelect = { onIntent(NewCardUiIntent.DeckSelected(it)) },
            onCreateDeck = onCreateDeck,
        )
    }
}

@Composable
private fun DeckPickerRow(
    decks: List<com.emm.domain.deck.Deck>,
    selected: com.emm.domain.deck.Deck?,
    enabled: Boolean,
    onSelect: (com.emm.domain.deck.Deck) -> Unit,
    onCreateDeck: () -> Unit,
) {
    if (decks.isEmpty()) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(emberSurface, RoundedCornerShape(14.dp))
                    .border(1.dp, emberAccent, RoundedCornerShape(14.dp))
                    .clickable(enabled = enabled, onClick = onCreateDeck)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Bookmark,
                    contentDescription = null,
                    tint = emberAccent,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.size(14.dp))
                Text(
                    text = stringResource(R.string.new_card_input_create_deck_cta),
                    fontFamily = geist,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = emberAccent,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = emberAccent,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.new_card_input_no_decks_hint),
                fontFamily = geist,
                fontSize = 12.sp,
                color = emberMuted,
            )
        }
        return
    }
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(emberSurface, RoundedCornerShape(14.dp))
                .border(1.dp, emberDivider, RoundedCornerShape(14.dp))
                .clickable(enabled = enabled) { expanded = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Bookmark,
                contentDescription = null,
                tint = emberAccent,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selected?.name ?: stringResource(R.string.new_card_input_deck_empty),
                    fontFamily = geist,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = emberOnBg,
                )
                val metadata = selected?.description?.takeIf(String::isNotBlank)
                if (metadata != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = metadata,
                        fontFamily = geistMono,
                        fontSize = 11.sp,
                        letterSpacing = 0.04.em,
                        color = emberMuted,
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = emberMuted,
                modifier = Modifier.size(20.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            decks.forEach { deck ->
                DropdownMenuItem(
                    text = { Text(deck.name) },
                    onClick = {
                        onSelect(deck)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun DifficultySection(state: NewCardUiState, onIntent: (NewCardUiIntent) -> Unit) {
    Column {
        HSectionLabel(label = stringResource(R.string.new_card_input_difficulty_label))
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            difficult.forEach { level ->
                val isActive = state.difficulty == level
                HChip(
                    label = difficultyDisplayLabel(level),
                    active = isActive,
                    accent = isActive,
                    onClick = { onIntent(NewCardUiIntent.DifficultySelected(level)) },
                )
            }
        }
    }
}

@Composable
internal fun List<ValidationIssue>.inputMessageOrNull(
    issueTextMapper: IssueTextMapper,
    target: InputField,
): String? {
    val issue = firstOrNull { issueTextMapper.map(it).target == IssueUiTarget.Input(target) }
        ?: return null
    return stringResource(issueTextMapper.map(issue).textResId)
}

private fun difficultyDisplayLabel(level: String): String = when (level) {
    "basico" -> "Básico"
    "intermedio" -> "Intermedio"
    "avanzado" -> "Avanzado"
    else -> level.replaceFirstChar { if (it.isLowerCase()) it.uppercaseChar().toString() else it.toString() }
}
