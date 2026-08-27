package com.emm.hello.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.emm.hello.core.theme.instrumentAccent
import com.emm.hello.core.theme.instrumentBg
import com.emm.hello.core.theme.instrumentDivider
import com.emm.hello.core.theme.instrumentMuted
import com.emm.hello.core.theme.instrumentOnBg
import com.emm.hello.core.theme.instrumentSurface

private val searchBarHeight = 46.dp
private val searchBarRadius = 22.dp
private val searchBarShape = RoundedCornerShape(searchBarRadius)

/**
 * Ember-styled search bar. 44–48dp height, 22dp radius, surface bg with divider border.
 * Focused: 1.5dp accent border, accent search icon, accent cursor blink animation.
 */
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
        targetValue = if (isFocused) instrumentAccent else instrumentDivider,
        animationSpec = tween(durationMillis = 150),
        label = "search_bar_border",
    )
    val borderWidth = if (isFocused) 1.5.dp else 1.dp
    val iconTint by animateColorAsState(
        targetValue = if (isFocused) instrumentAccent else instrumentMuted,
        animationSpec = tween(150),
        label = "search_icon_tint",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(searchBarHeight)
            .clip(searchBarShape)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = searchBarShape,
            )
            .background(instrumentSurface)
            .padding(horizontal = 16.dp)
            .semantics { contentDescription = "Campo de búsqueda" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = leadingIconContentDescription,
            tint = iconTint,
            modifier = Modifier.size(17.dp),
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp)
                .onFocusChanged { isFocused = it.isFocused },
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.isEmpty() && placeholder != null) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = instrumentMuted,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(instrumentSurface),
                enabled = enabled,
                readOnly = readOnly,
                singleLine = singleLine,
                keyboardOptions = keyboardOptions,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = instrumentOnBg),
                cursorBrush = SolidColor(instrumentAccent),
            )
        }

        if (value.isNotEmpty()) {
            HIconButton(
                icon = Icons.Default.Close,
                contentDescription = clearContentDescription,
                onClick = { onValueChange("") },
                tint = instrumentMuted,
                iconSize = 16.dp,
                buttonSize = 32.dp,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun HSearchBarPreview() {
    HelloTheme {
        Surface(color = instrumentBg) {
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
        Surface(color = instrumentBg) {
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
