package com.emm.hello.newfeatures.hoy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.emm.domain.study.DashboardStats
import com.emm.domain.study.NextDueBatch
import com.emm.hello.R
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.schibsted
import com.emm.hello.core.theme.ink
import com.emm.hello.core.theme.inkFaint
import com.emm.hello.core.theme.inkMuted
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HButtonSize
import com.emm.hello.core.ui.HButtonVariant
import com.emm.hello.core.ui.HFab
import com.emm.hello.core.ui.HIconButton
import com.emm.hello.core.ui.HLoadingSpinner
import com.emm.hello.core.ui.HSectionLabel
import java.time.Instant

@Composable
fun HoyScreen(
    modifier: Modifier = Modifier,
    state: HoyUiState = HoyUiState(),
    onCapture: () -> Unit = {},
    onStudy: () -> Unit = {},
    onSettings: () -> Unit = {},
    onLibrary: () -> Unit = {},
    onVisible: () -> Unit = {},
) {
    LaunchedEffect(Unit) {
        onVisible()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            WordmarkRow(onSettings = onSettings, onLibrary = onLibrary)

            if (state.isLoading) {
                LoadingContent(modifier = Modifier.weight(1f))
            } else {
                SessionContent(
                    modifier = Modifier.weight(1f),
                    state = state,
                    onStudy = onStudy,
                    onCapture = onCapture,
                )
            }
        }

        HFab(
            onClick = onCapture,
            label = stringResource(R.string.hoy_fab_label),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp),
        )
    }
}

@Composable
private fun WordmarkRow(
    onSettings: () -> Unit,
    onLibrary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 8.dp, top = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        fontSize = 22.sp,
                        color = ink,
                        letterSpacing = (-0.2).sp,
                    ),
                ) {
                    append("Hello")
                }
                withStyle(
                    SpanStyle(
                        fontSize = 22.sp,
                        color = ink,
                        letterSpacing = (-0.2).sp,
                    ),
                ) {
                    append(".")
                }
            },
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            HIconButton(
                icon = Icons.AutoMirrored.Filled.List,
                contentDescription = stringResource(R.string.library_content_description),
                onClick = onLibrary,
                tint = inkMuted,
                iconSize = 20.dp,
            )
            HIconButton(
                icon = Icons.Default.Settings,
                contentDescription = stringResource(R.string.settings_content_description),
                onClick = onSettings,
                tint = inkMuted,
                iconSize = 20.dp,
            )
        }
    }
}

@Composable
private fun SessionContent(
    state: HoyUiState,
    onStudy: () -> Unit,
    onCapture: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(18.dp))

        if (state.hasSessionReady) {
            SessionCta(
                dueCount = state.cardsDueToday,
                estimatedMinutes = state.estimatedSessionMinutes,
                onStudy = onStudy,
            )
        } else {
            RestingHero(nextDue = state.nextDue, onCapture = onCapture)
        }

        val stats: DashboardStats? = state.stats
        if (stats != null) {
            Spacer(Modifier.height(36.dp))
            HSectionLabel(label = stringResource(R.string.hoy_section_progress))
            Spacer(Modifier.height(10.dp))
            HoyStatsSection(stats = stats)
        }

        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun SessionCta(
    dueCount: Int,
    estimatedMinutes: Int,
    onStudy: () -> Unit,
) {
    HButton(
        onClick = onStudy,
        variant = HButtonVariant.Accent,
        size = HButtonSize.Xl,
        full = true,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.hoy_study_now),
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                letterSpacing = (-0.01).em,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(
                    R.string.hoy_session_supporting,
                    pluralStringResource(R.plurals.hoy_card_count, dueCount, dueCount),
                    estimatedMinutes,
                ).uppercase(),
                fontFamily = schibsted,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                letterSpacing = 0.1.em,
            )
        }
    }
}

@Composable
private fun RestingHero(
    nextDue: NextDueBatch?,
    onCapture: () -> Unit,
) {
    Column {
        Text(
            text = stringResource(R.string.hoy_hero_calm),
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp,
            lineHeight = (32 * 1.06f).sp,
            letterSpacing = (-0.02).em,
            color = inkMuted,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = nextDueLabel(nextDue),
            fontFamily = schibsted,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            letterSpacing = 0.1.em,
            color = inkFaint,
        )
        Spacer(Modifier.height(20.dp))
        HButton(
            text = stringResource(R.string.hoy_resting_cta),
            onClick = onCapture,
            variant = HButtonVariant.Accent,
            size = HButtonSize.Lg,
            full = true,
        )
    }
}

@Composable
private fun nextDueLabel(nextDue: NextDueBatch?): String {
    if (nextDue == null) return stringResource(R.string.hoy_next_due_none).uppercase()

    val cards: String = pluralStringResource(
        R.plurals.hoy_card_count,
        nextDue.cardCount,
        nextDue.cardCount,
    )
    return when (nextDue.daysFromToday) {
        0 -> stringResource(R.string.hoy_next_due_later_today, cards)
        1 -> stringResource(R.string.hoy_next_due_tomorrow, cards)
        else -> stringResource(R.string.hoy_next_due_in_days, cards, nextDue.daysFromToday)
    }.uppercase()
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        HLoadingSpinner(color = ink)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08090A)
@Composable
private fun HoyScreenPreview() {
    HelloTheme {
        HoyScreen(
            state = HoyUiState(
                isLoading = false,
                stats = DashboardStats(
                    cardsStudiedToday = 12,
                    cardsDueToday = 8,
                    currentStreak = 4,
                    cardsDueThisWeek = 26,
                ),
            ),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08090A)
@Composable
private fun HoyScreenRestingPreview() {
    HelloTheme {
        HoyScreen(
            state = HoyUiState(
                isLoading = false,
                stats = DashboardStats(
                    cardsStudiedToday = 8,
                    cardsDueToday = 0,
                    currentStreak = 3,
                    cardsDueThisWeek = 5,
                    nextDue = NextDueBatch(
                        at = Instant.parse("2026-08-28T09:00:00Z"),
                        cardCount = 5,
                        daysFromToday = 1,
                    ),
                ),
            ),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08090A)
@Composable
private fun HoyScreenLoadingPreview() {
    HelloTheme {
        HoyScreen(state = HoyUiState(isLoading = true))
    }
}
