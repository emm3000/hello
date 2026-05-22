package com.emm.hello.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.emberAccent
import com.emm.hello.core.theme.emberBg
import com.emm.hello.core.theme.emberMuted
import com.emm.hello.core.theme.geistMono

/**
 * Uppercase Geist Mono 10.5sp section label, emberMuted color.
 * Optional [action] slot (caller provides a composable, usually a text button).
 * 12dp marginBottom via [Modifier.padding] on the Column below.
 */
@Composable
fun HSectionLabel(
    label: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label.uppercase(),
            fontFamily = geistMono,
            fontWeight = FontWeight.Medium,
            fontSize = 10.5.sp,
            letterSpacing = 0.12.em,
            color = emberMuted,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (action != null) {
            action()
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun HSectionLabelPreview() {
    HelloTheme {
        Surface(color = emberBg) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HSectionLabel(label = "Tus mazos")
                HSectionLabel(
                    label = "Tarjetas",
                    action = {
                        Text(
                            text = "5 con repaso ↗",
                            fontFamily = geistMono,
                            fontSize = 10.5.sp,
                            color = emberAccent,
                        )
                    },
                )
            }
        }
    }
}
