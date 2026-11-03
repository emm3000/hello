package com.emm.hello.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.lerp
import com.emm.domain.reminder.StudyReminderSettings
import com.emm.hello.R
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.helloShapes
import com.emm.hello.core.theme.ink
import com.emm.hello.core.theme.inkFaint
import com.emm.hello.core.theme.inkMuted
import com.emm.hello.core.theme.spacing
import com.emm.hello.core.theme.surface
import com.emm.hello.core.theme.surfaceRaised
import java.time.LocalTime
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

private const val VISIBLE_ROWS: Int = 5
private const val ROWS_ABOVE_CENTRE: Int = VISIBLE_ROWS / 2
private const val EDGE_ROW_ALPHA: Float = 0.5f
private const val OFF_CENTRE_SCALE: Float = 0.9f

@Composable
fun HTimePickerDialog(
    initialTime: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.settings_reminder_time_title),
    confirmText: String = stringResource(R.string.save),
    cancelText: String = stringResource(R.string.cancel),
) {
    val startTime: LocalTime = remember(initialTime) { initialTime.roundedToMinuteStep() }
    var hour: Int by rememberSaveable { mutableIntStateOf(startTime.hour) }
    var minute: Int by rememberSaveable { mutableIntStateOf(startTime.minute) }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        shape = MaterialTheme.helloShapes.container,
        containerColor = surface,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                WheelTimePicker(
                    hour = hour,
                    minute = minute,
                    onHourChange = { hour = it },
                    onMinuteChange = { minute = it },
                )
            }
        },
        confirmButton = {
            HButton(
                text = confirmText,
                onClick = { onConfirm(LocalTime.of(hour, minute)) },
                variant = HButtonVariant.Primary,
            )
        },
        dismissButton = {
            HButton(
                text = cancelText,
                onClick = onDismiss,
                variant = HButtonVariant.Text,
            )
        },
    )
}

@Composable
private fun WheelTimePicker(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val style: TextStyle = MaterialTheme.typography.headlineLarge
    val density: Density = LocalDensity.current
    val rowHeight: Dp = with(density) { style.lineHeight.toDp() } + MaterialTheme.spacing.md

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(vertical = rowHeight * ROWS_ABOVE_CENTRE)
                .clip(MaterialTheme.helloShapes.control)
                .background(surfaceRaised),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            TimeWheel(
                range = hourWheelRange,
                initialValue = hour,
                onValueChange = onHourChange,
                label = stringResource(R.string.time_picker_hour),
                style = style,
                rowHeight = rowHeight,
            )
            Text(text = ":", style = style, color = inkMuted)
            TimeWheel(
                range = minuteWheelRange,
                initialValue = minute,
                onValueChange = onMinuteChange,
                label = stringResource(R.string.time_picker_minute),
                style = style,
                rowHeight = rowHeight,
            )
        }
    }
}

@Composable
private fun TimeWheel(
    range: WheelRange,
    initialValue: Int,
    onValueChange: (Int) -> Unit,
    label: String,
    style: TextStyle,
    rowHeight: Dp,
) {
    val density: Density = LocalDensity.current
    val rowHeightPx: Float = with(density) { rowHeight.toPx() }
    val listState: LazyListState = rememberLazyListState(
        initialFirstVisibleItemIndex = range.startIndexFor(initialValue),
    )
    val flingBehavior: FlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val centredIndex: State<Int> = remember(listState, rowHeightPx) {
        derivedStateOf { listState.centredIndex(rowHeightPx) }
    }
    val centredValue: Int = range.valueAt(centredIndex.value)
    val haptics: HapticFeedback = LocalHapticFeedback.current
    val currentOnValueChange: (Int) -> Unit by rememberUpdatedState(onValueChange)
    val scope: CoroutineScope = rememberCoroutineScope()
    val wheelWidth: Dp = rememberWheelWidth(range, style) + MaterialTheme.spacing.xl * 2

    LaunchedEffect(listState, range) {
        snapshotFlow { range.valueAt(centredIndex.value) }
            .distinctUntilChanged()
            .drop(1)
            .collect { value ->
                haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                currentOnValueChange(value)
            }
    }

    LazyColumn(
        state = listState,
        flingBehavior = flingBehavior,
        contentPadding = PaddingValues(vertical = rowHeight * ROWS_ABOVE_CENTRE),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(wheelWidth)
            .height(rowHeight * VISIBLE_ROWS)
            .clearAndSetSemantics {
                contentDescription = label
                stateDescription = centredValue.toString()
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = centredValue.toFloat(),
                    range = 0f..range.maxValue.toFloat(),
                    steps = range.rowCount - 2,
                )
                setProgress { target ->
                    val index: Int = range.nearestIndexFor(target.roundToInt(), centredIndex.value)
                    scope.launch { listState.animateScrollToItem(index) }
                    true
                }
            },
    ) {
        items(count = range.virtualCount) { index ->
            Box(
                modifier = Modifier
                    .height(rowHeight)
                    .graphicsLayer {
                        val distance: Float = listState.rowDistanceFromCentre(index, rowHeightPx)
                        alpha = rowAlpha(distance)
                        val scale: Float = lerp(1f, OFF_CENTRE_SCALE, distance.coerceIn(0f, 1f))
                        scaleX = scale
                        scaleY = scale
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = range.valueAt(index).toTwoDigits(),
                    style = style,
                    color = ink,
                )
            }
        }
    }
}

@Composable
private fun rememberWheelWidth(range: WheelRange, style: TextStyle): Dp {
    val measurer: TextMeasurer = rememberTextMeasurer()
    val density: Density = LocalDensity.current
    return remember(range, style, density) {
        val widestPx: Int = (0 until range.rowCount).maxOf { row ->
            measurer.measure(range.valueAt(row).toTwoDigits(), style).size.width
        }
        with(density) { widestPx.toDp() }
    }
}

private fun LazyListState.centredIndex(rowHeightPx: Float): Int =
    if (firstVisibleItemScrollOffset > rowHeightPx / 2) firstVisibleItemIndex + 1 else firstVisibleItemIndex

private fun LazyListState.rowDistanceFromCentre(index: Int, rowHeightPx: Float): Float {
    val scrolledRows: Float = firstVisibleItemIndex + firstVisibleItemScrollOffset / rowHeightPx
    return abs(index - scrolledRows)
}

private fun rowAlpha(distance: Float): Float {
    val nearFade: Float = lerp(1f, inkFaint.alpha, distance.coerceIn(0f, 1f))
    val farFade: Float = lerp(1f, EDGE_ROW_ALPHA, (distance - 1f).coerceIn(0f, 1f))
    return nearFade * farFade
}

private fun Int.toTwoDigits(): String = toString().padStart(2, '0')

@PreviewLightDark
@Composable
private fun WheelTimePickerPreview() {
    HelloTheme {
        Surface(color = surface) {
            var hour: Int by remember { mutableIntStateOf(StudyReminderSettings.DEFAULT_TIME.hour) }
            var minute: Int by remember { mutableIntStateOf(StudyReminderSettings.DEFAULT_TIME.minute) }
            WheelTimePicker(
                hour = hour,
                minute = minute,
                onHourChange = { hour = it },
                onMinuteChange = { minute = it },
                modifier = Modifier.padding(MaterialTheme.spacing.lg),
            )
        }
    }
}
