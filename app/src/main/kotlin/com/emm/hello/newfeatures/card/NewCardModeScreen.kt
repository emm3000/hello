package com.emm.hello.newfeatures.card

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.emm.hello.R
import com.emm.hello.core.theme.instrumentAccent
import com.emm.hello.core.theme.instrumentBg
import com.emm.hello.core.theme.instrumentDivider
import com.emm.hello.core.theme.instrumentMuted
import com.emm.hello.core.theme.instrumentOnBg
import com.emm.hello.core.theme.instrumentSurface
import com.emm.hello.core.theme.geist
import com.emm.hello.core.theme.spacing
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HButtonSize
import com.emm.hello.core.ui.HButtonVariant
import com.emm.hello.core.ui.HWizTop

@Composable
fun NewCardModeScreen(
    selectedMode: TypeView,
    onModeSelected: (TypeView) -> Unit,
    onContinue: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = instrumentBg,
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            HWizTop(
                currentStep = 1,
                totalSteps = 3,
                onBack = onNavigateBack,
                subtitle = stringResource(R.string.new_card_wizard_subtitle),
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(
                    start = MaterialTheme.spacing.screenGutter,
                    end = MaterialTheme.spacing.screenGutter,
                    top = 12.dp,
                    bottom = 32.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    HeroHeader()
                }
                items(modeOptions, key = { it.mode.name }) { option ->
                    ModeOptionCard(
                        option = option,
                        isSelected = selectedMode == option.mode,
                        onClick = { onModeSelected(option.mode) },
                    )
                }
            }

            NewCardBottomBar {
                HButton(
                    text = stringResource(R.string.continue_action),
                    onClick = onContinue,
                    variant = HButtonVariant.Accent,
                    size = HButtonSize.Lg,
                    full = true,
                )
            }
        }
    }
}

@Composable
private fun HeroHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 18.dp),
    ) {
        Text(
            text = stringResource(R.string.new_card_mode_headline),
            fontWeight = FontWeight.SemiBold,
            fontSize = 38.sp,
            lineHeight = 42.sp,
            letterSpacing = (-0.02).em,
            color = instrumentOnBg,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.new_card_mode_body),
            fontFamily = geist,
            fontWeight = FontWeight.Normal,
            fontSize = 13.5.sp,
            lineHeight = 19.sp,
            color = instrumentMuted,
        )
    }
}

private data class ModeOption(
    val mode: TypeView,
    val letterRes: Int,
    val titleRes: Int,
    val descriptionRes: Int,
    val exampleRes: Int,
)

private val modeOptions = listOf(
    ModeOption(
        mode = TypeView.WordOrPhase,
        letterRes = R.string.new_card_mode_letter_word,
        titleRes = R.string.new_card_mode_word,
        descriptionRes = R.string.mode_word_description,
        exampleRes = R.string.new_card_mode_example_word,
    ),
    ModeOption(
        mode = TypeView.WithCategories,
        letterRes = R.string.new_card_mode_letter_category,
        titleRes = R.string.new_card_mode_category,
        descriptionRes = R.string.mode_category_description,
        exampleRes = R.string.new_card_mode_example_category,
    ),
    ModeOption(
        mode = TypeView.WithAiHelp,
        letterRes = R.string.new_card_mode_letter_ai,
        titleRes = R.string.new_card_mode_ai,
        descriptionRes = R.string.mode_ai_description,
        exampleRes = R.string.new_card_mode_example_ai,
    ),
)

@Composable
private fun ModeOptionCard(
    option: ModeOption,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val borderColor = if (isSelected) instrumentAccent else instrumentDivider
    val borderWidth = if (isSelected) 1.5.dp else 1.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(instrumentSurface)
            .border(borderWidth, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = stringResource(option.letterRes),
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp,
            lineHeight = 32.sp,
            color = if (isSelected) instrumentAccent else instrumentMuted,
            modifier = Modifier.width(36.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(option.titleRes),
                fontFamily = geist,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                color = instrumentOnBg,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(option.descriptionRes),
                fontFamily = geist,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = instrumentMuted,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(option.exampleRes),
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                color = instrumentAccent,
            )
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = instrumentAccent,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
