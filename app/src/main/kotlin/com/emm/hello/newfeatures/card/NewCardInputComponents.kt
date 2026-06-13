package com.emm.hello.newfeatures.card

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.emm.hello.core.ui.HCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.emm.hello.core.theme.metadata
import com.emm.hello.core.theme.spacing

@Composable
internal fun LabeledCheckbox(
    label: String,
    checked: Boolean,
    isEnabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { onCheckedChange(!checked) }, enabled = isEnabled)
            .padding(MaterialTheme.spacing.sm),
    ) {
        HCheckbox(checked = checked, onCheckedChange = null)
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
internal fun SupportingText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.metadata,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
