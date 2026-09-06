package com.emm.hello.core.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerColors
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.domain.reminder.StudyReminderSettings
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.ink
import com.emm.hello.core.theme.onInk
import com.emm.hello.core.theme.surface
import com.emm.hello.core.theme.surfaceRaised
import java.time.LocalTime

private val timePickerDialogShape = RoundedCornerShape(16.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HTimePickerDialog(
    initialTime: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Reminder time",
    confirmText: String = "Save",
    cancelText: String = "Cancel",
) {
    val state: TimePickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true,
    )
    val colors: TimePickerColors = TimePickerDefaults.colors(
        selectorColor = ink,
        clockDialColor = surfaceRaised,
        clockDialSelectedContentColor = onInk,
        clockDialUnselectedContentColor = ink,
        periodSelectorSelectedContainerColor = ink,
        periodSelectorSelectedContentColor = onInk,
        periodSelectorUnselectedContainerColor = surfaceRaised,
        timeSelectorSelectedContainerColor = ink,
        timeSelectorSelectedContentColor = onInk,
        timeSelectorUnselectedContainerColor = surfaceRaised,
    )

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        shape = timePickerDialogShape,
        containerColor = surface,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            TimePicker(state = state, colors = colors)
        },
        confirmButton = {
            HButton(
                text = confirmText,
                onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) },
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

@Preview
@Composable
private fun HTimePickerDialogPreview() {
    HelloTheme {
        Surface {
            var show by remember { mutableStateOf(true) }
            if (show) {
                HTimePickerDialog(
                    initialTime = StudyReminderSettings.DEFAULT_TIME,
                    onConfirm = { show = false },
                    onDismiss = { show = false },
                )
            }
        }
    }
}
