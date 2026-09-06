package com.emm.hello.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.ink
import com.emm.hello.core.theme.onInk
import com.emm.hello.core.theme.outline
import com.emm.hello.core.theme.surfaceRaised

@Composable
fun HSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors: SwitchColors = SwitchDefaults.colors(
        checkedTrackColor = ink,
        checkedThumbColor = onInk,
        checkedBorderColor = ink,
        uncheckedTrackColor = surfaceRaised,
        uncheckedThumbColor = outline,
        uncheckedBorderColor = outline,
    )
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF4F3F1)
@Composable
private fun HSwitchPreview() {
    HelloTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HSwitch(checked = true, onCheckedChange = {})
            HSwitch(checked = false, onCheckedChange = {})
            HSwitch(checked = true, onCheckedChange = {}, enabled = false)
        }
    }
}
