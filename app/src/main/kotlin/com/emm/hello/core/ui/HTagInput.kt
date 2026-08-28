package com.emm.hello.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.emm.hello.R
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.spacing

@Composable
fun HTagInput(
    tags: List<String>,
    onTagsChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var inputValue by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        if (tags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
            ) {
                tags.forEach { tag ->
                    HTagChip(
                        tag = tag,
                        removable = enabled,
                        onRemove = { onTagsChange(tags - tag) },
                    )
                }
            }
        }

        HInput(
            value = inputValue,
            onValueChange = { raw ->
                if (!raw.endsWith(",")) inputValue = raw
            },
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.tags_label),
            placeholder = stringResource(R.string.tags_placeholder),
            supportingText = stringResource(R.string.tags_supporting_text),
            enabled = enabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    normalizeAndAddTag(inputValue, tags, onTagsChange) { inputValue = "" }
                },
            ),
        )
    }
}

private fun normalizeAndAddTag(
    raw: String,
    currentTags: List<String>,
    onTagsChange: (List<String>) -> Unit,
    onClear: () -> Unit,
) {
    val normalized = raw.trimEnd(',').trim().lowercase()
    if (normalized.isEmpty()) {
        onClear()
        return
    }
    val updated = currentTags.toMutableList()
    if (updated.none { it.equals(normalized, ignoreCase = true) }) {
        updated.add(normalized)
        onTagsChange(updated)
    }
    onClear()
}

@Composable
private fun HTagInputPreview() {
    HelloTheme {
        Surface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                var tags1 by remember { mutableStateOf(listOf("spanish", "work")) }
                HTagInput(
                    tags = tags1,
                    onTagsChange = { tags1 = it },
                )

                var tags2 by remember { mutableStateOf<List<String>>(emptyList()) }
                HTagInput(
                    tags = tags2,
                    onTagsChange = { tags2 = it },
                    enabled = false,
                )
            }
        }
    }
}
