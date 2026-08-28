package com.emm.hello.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.hairline
import com.emm.hello.core.theme.helloShapes
import com.emm.hello.core.theme.ink
import com.emm.hello.core.theme.inkFaint
import com.emm.hello.core.theme.inkMuted
import com.emm.hello.core.theme.pageBackground
import com.emm.hello.core.theme.spacing
import com.emm.hello.core.theme.surface

@Composable
fun HSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    clearContentDescription: String = "Limpiar búsqueda",
    leadingIconContentDescription: String? = null,
) {
    var isFocused by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) ink else hairline,
        animationSpec = tween(durationMillis = 150),
        label = "search_bar_border",
    )
    val iconTint by animateColorAsState(
        targetValue = if (isFocused) ink else inkMuted,
        animationSpec = tween(durationMillis = 150),
        label = "search_icon_tint",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clip(MaterialTheme.helloShapes.control)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = MaterialTheme.helloShapes.control,
            )
            .background(surface)
            .padding(horizontal = MaterialTheme.spacing.md)
            .semantics { contentDescription = "Campo de búsqueda" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = leadingIconContentDescription,
            tint = iconTint,
            modifier = Modifier.size(20.dp),
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { isFocused = it.isFocused },
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.isEmpty() && placeholder != null) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = inkFaint,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                readOnly = readOnly,
                singleLine = singleLine,
                keyboardOptions = keyboardOptions,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = ink),
                cursorBrush = SolidColor(ink),
            )
        }

        if (value.isNotEmpty()) {
            HIconButton(
                icon = Icons.Default.Close,
                contentDescription = clearContentDescription,
                onClick = { onValueChange("") },
                tint = inkMuted,
                iconSize = 20.dp,
                buttonSize = 36.dp,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun HSearchBarPreview() {
    HelloTheme {
        Surface(color = pageBackground) {
            Box(modifier = Modifier.padding(16.dp)) {
                HSearchBar(
                    value = "",
                    onValueChange = {},
                    placeholder = "Buscar mazos…",
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun HSearchBarWithValuePreview() {
    HelloTheme {
        Surface(color = pageBackground) {
            Box(modifier = Modifier.padding(16.dp)) {
                HSearchBar(
                    value = "serendipity",
                    onValueChange = {},
                    placeholder = "Buscar mazos…",
                )
            }
        }
    }
}
