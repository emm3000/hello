package com.emm.hello.newfeatures.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emm.domain.study.DashboardStats
import com.emm.domain.study.NextDueBatch
import com.emm.hello.R
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.cardHues
import com.emm.hello.core.theme.helloShapes
import com.emm.hello.core.theme.ink
import com.emm.hello.core.theme.inkMuted
import com.emm.hello.core.theme.metadata
import com.emm.hello.core.theme.pageBackground
import com.emm.hello.core.theme.spacing
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HButtonVariant
import com.emm.hello.core.ui.HIconButton
import com.emm.hello.core.ui.HRing
import java.time.Instant
import kotlin.math.roundToInt

@Composable
fun TodayScreen(
    modifier: Modifier = Modifier,
    state: TodayUiState = TodayUiState(),
    onCapture: () -> Unit = {},
    onStudy: () -> Unit = {},
    onSettings: () -> Unit = {},
    onLibrary: () -> Unit = {},
    onVisible: () -> Unit = {},
) {
    LaunchedEffect(Unit) {
        onVisible()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(pageBackground)
            .systemBarsPadding()
            .padding(horizontal = MaterialTheme.spacing.screenGutter)
            .padding(bottom = MaterialTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg),
    ) {
        TodayHeader(state = state, onSettings = onSettings)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            DueStack(state = state)
        }
        TodayActions(
            state = state,
            onStudy = onStudy,
            onCapture = onCapture,
            onLibrary = onLibrary,
        )
    }
}

@Composable
private fun TodayHeader(
    state: TodayUiState,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.today_label).uppercase(),
            style = MaterialTheme.typography.metadata,
            color = inkMuted,
            modifier = Modifier.weight(1f),
        )
        HIconButton(
            icon = Icons.Default.Settings,
            contentDescription = stringResource(R.string.settings_content_description),
            onClick = onSettings,
            tint = inkMuted,
            iconSize = 20.dp,
            buttonSize = 44.dp,
        )
        if (!state.isLoading) {
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
            val ringDescription: String = stringResource(
                R.string.today_ring_content_description,
                (state.ringProgress * 100).roundToInt(),
            )
            HRing(
                progress = state.ringProgress,
                modifier = Modifier.semantics { contentDescription = ringDescription },
            )
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
            Text(
                text = stringResource(R.string.today_day_number, state.dayNumber),
                style = MaterialTheme.typography.titleSmall,
                color = ink,
            )
        }
    }
}

private const val STACK_ASPECT_RATIO: Float = 300f / 220f
private const val STACK_BACK_ROTATION: Float = -5f
private const val STACK_MIDDLE_ROTATION: Float = 4f
private val stackBackOffset: Dp = 10.dp
private val stackMiddleOffset: Dp = 6.dp
private val stackMaxWidth: Dp = 300.dp

@Composable
private fun DueStack(state: TodayUiState, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .widthIn(max = stackMaxWidth)
            .fillMaxWidth()
            .aspectRatio(STACK_ASPECT_RATIO),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = stackBackOffset)
                .graphicsLayer { rotationZ = STACK_BACK_ROTATION }
                .clip(MaterialTheme.helloShapes.container)
                .background(cardHues[2]),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = stackMiddleOffset)
                .graphicsLayer { rotationZ = STACK_MIDDLE_ROTATION }
                .clip(MaterialTheme.helloShapes.container)
                .background(cardHues[1]),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(MaterialTheme.helloShapes.container)
                .background(cardHues[0]),
            contentAlignment = Alignment.BottomStart,
        ) {
            Column(
                modifier = Modifier.padding(MaterialTheme.spacing.xl),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
            ) {
                StackCopy(state = state)
            }
        }
    }
}

@Composable
private fun StackCopy(state: TodayUiState) {
    when {
        state.isLoading -> Unit
        state.hasSessionReady -> {
            Text(
                text = pluralStringResource(
                    R.plurals.today_word_count,
                    state.cardsDueToday,
                    state.cardsDueToday,
                ),
                style = MaterialTheme.typography.displaySmall,
                color = ink,
            )
            Text(
                text = stringResource(R.string.today_estimate, state.estimatedSessionMinutes),
                style = MaterialTheme.typography.bodyMedium,
                color = inkMuted,
            )
        }
        else -> {
            Text(
                text = stringResource(R.string.today_nothing_due),
                style = MaterialTheme.typography.displaySmall,
                color = ink,
            )
            Text(
                text = nextDueCopy(state.nextDue),
                style = MaterialTheme.typography.bodyMedium,
                color = inkMuted,
            )
        }
    }
}

@Composable
private fun nextDueCopy(nextDue: NextDueBatch?): String {
    if (nextDue == null) return stringResource(R.string.today_next_due_none)

    val words: String = pluralStringResource(
        R.plurals.today_word_count,
        nextDue.cardCount,
        nextDue.cardCount,
    )
    return when (nextDue.daysFromToday) {
        0 -> stringResource(R.string.today_next_due_later_today, words)
        1 -> stringResource(R.string.today_next_due_tomorrow, words)
        else -> stringResource(R.string.today_next_due_in_days, words, nextDue.daysFromToday)
    }
}

@Composable
private fun TodayActions(
    state: TodayUiState,
    onStudy: () -> Unit,
    onCapture: () -> Unit,
    onLibrary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
    ) {
        when {
            state.isLoading -> Unit
            state.hasSessionReady -> {
                HButton(
                    text = stringResource(R.string.today_start),
                    onClick = onStudy,
                    variant = HButtonVariant.Primary,
                    full = true,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
                ) {
                    HButton(
                        text = stringResource(R.string.today_add_word),
                        onClick = onCapture,
                        variant = HButtonVariant.Secondary,
                        icon = Icons.Default.Add,
                        modifier = Modifier.weight(1f),
                    )
                    HButton(
                        text = stringResource(R.string.today_library),
                        onClick = onLibrary,
                        variant = HButtonVariant.Secondary,
                        icon = Icons.AutoMirrored.Filled.List,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            else -> {
                HButton(
                    text = stringResource(R.string.today_add_word),
                    onClick = onCapture,
                    variant = HButtonVariant.Primary,
                    full = true,
                    icon = Icons.Default.Add,
                )
                HButton(
                    text = stringResource(R.string.today_library),
                    onClick = onLibrary,
                    variant = HButtonVariant.Secondary,
                    full = true,
                    icon = Icons.AutoMirrored.Filled.List,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TodayScreenDuePreview() {
    HelloTheme {
        TodayScreen(
            state = TodayUiState(
                isLoading = false,
                stats = DashboardStats(
                    cardsStudiedToday = 0,
                    cardsDueToday = 8,
                    currentStreak = 5,
                    cardsDueThisWeek = 20,
                ),
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TodayScreenMidSessionPreview() {
    HelloTheme {
        TodayScreen(
            state = TodayUiState(
                isLoading = false,
                stats = DashboardStats(
                    cardsStudiedToday = 3,
                    cardsDueToday = 5,
                    currentStreak = 6,
                    cardsDueThisWeek = 12,
                ),
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TodayScreenNothingDuePreview() {
    HelloTheme {
        TodayScreen(
            state = TodayUiState(
                isLoading = false,
                stats = DashboardStats(
                    cardsStudiedToday = 8,
                    cardsDueToday = 0,
                    currentStreak = 6,
                    cardsDueThisWeek = 5,
                    nextDue = NextDueBatch(
                        at = Instant.now(),
                        cardCount = 3,
                        daysFromToday = 1,
                    ),
                ),
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TodayScreenLoadingPreview() {
    HelloTheme {
        TodayScreen(state = TodayUiState())
    }
}
