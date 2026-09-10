package com.emm.hello.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emm.hello.R
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.ink

@Composable
fun HSpeakerButton(
    isSpeaking: Boolean,
    onSpeak: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = stringResource(
        if (isSpeaking) R.string.stop_speech_desc else R.string.speak_desc,
    ),
    tint: Color = ink,
    iconSize: Dp = 24.dp,
    buttonSize: Dp = 48.dp,
    enabled: Boolean = true,
) {
    HIconButton(
        icon = if (isSpeaking) Icons.Filled.Stop else Icons.AutoMirrored.Filled.VolumeUp,
        contentDescription = contentDescription,
        onClick = { if (isSpeaking) onStop() else onSpeak() },
        modifier = modifier,
        tint = tint,
        iconSize = iconSize,
        buttonSize = buttonSize,
        enabled = enabled,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun HSpeakerButtonPreview() {
    HelloTheme {
        HSpeakerButton(
            isSpeaking = false,
            onSpeak = {},
            onStop = {},
        )
    }
}
