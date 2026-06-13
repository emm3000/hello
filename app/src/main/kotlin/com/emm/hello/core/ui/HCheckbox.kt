package com.emm.hello.core.ui

import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.emm.hello.core.theme.HelloTheme

/**
 * Thin H* wrapper over M3 [Checkbox] so feature screens never call raw Material3.
 * Keeps the default M3 rendering used by the Ember theme.
 *
 * Pass [onCheckedChange] = null to make the checkbox read-only (the containing
 * row handles the click, as in [LabeledCheckbox]).
 */
@Composable
fun HCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun HCheckboxPreview() {
    HelloTheme {
        HCheckbox(checked = true, onCheckedChange = null)
    }
}
