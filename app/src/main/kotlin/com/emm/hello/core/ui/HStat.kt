package com.emm.hello.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.emberAccent
import com.emm.hello.core.theme.emberBg
import com.emm.hello.core.theme.emberMuted
import com.emm.hello.core.theme.emberPrimary
import com.emm.hello.core.theme.geist
import com.emm.hello.core.theme.instrumentSerif

enum class HStatTone { Default, Accent, Muted }

/**
 * Inline Instrument Serif 26sp number + Geist 12.5sp muted label.
 * Tones: default | accent | muted.
 */
@Composable
fun HStat(
    num: String,
    label: String,
    modifier: Modifier = Modifier,
    tone: HStatTone = HStatTone.Default,
) {
    val numColor = when (tone) {
        HStatTone.Default -> emberPrimary
        HStatTone.Accent -> emberAccent
        HStatTone.Muted -> emberMuted
    }

    Row(
        modifier = modifier,
        verticalAlignment = androidx.compose.ui.Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = num,
            fontFamily = instrumentSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 26.sp,
            lineHeight = 26.sp,
            color = numColor,
        )
        Text(
            text = label,
            fontFamily = geist,
            fontWeight = FontWeight.Normal,
            fontSize = 12.5.sp,
            letterSpacing = 0.1.sp,
            color = emberMuted,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun HStatPreview() {
    HelloTheme {
        Surface(color = emberBg) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                HStat(num = "38", label = "tarjetas")
                HStat(num = "7", label = "para hoy", tone = HStatTone.Accent)
                HStat(num = "6.2", label = "días prom.", tone = HStatTone.Muted)
            }
        }
    }
}
