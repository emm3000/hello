package com.emm.hello.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.emm.hello.core.theme.instrumentAccent
import com.emm.hello.core.theme.instrumentBg
import com.emm.hello.core.theme.instrumentMuted
import com.emm.hello.core.theme.instrumentOnBg
import com.emm.hello.core.theme.instrumentWarn
import com.emm.hello.core.theme.geist
import com.emm.hello.core.theme.geistMono

enum class HDictSenseTone { Default, Warn }

@Composable
fun HDictSense(
    index: Int,
    label: String,
    body: String,
    modifier: Modifier = Modifier,
    tone: HDictSenseTone = HDictSenseTone.Default,
) {
    val numeralColor = when (tone) {
        HDictSenseTone.Default -> instrumentAccent
        HDictSenseTone.Warn -> instrumentWarn
    }
    val labelColor = when (tone) {
        HDictSenseTone.Default -> instrumentMuted
        HDictSenseTone.Warn -> instrumentWarn
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = index.toString(),
            fontWeight = FontWeight.SemiBold,
            fontSize = 26.sp,
            lineHeight = 28.sp,
            color = numeralColor,
            modifier = Modifier.width(32.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label.uppercase(),
                fontFamily = geistMono,
                fontWeight = FontWeight.Medium,
                fontSize = 10.5.sp,
                letterSpacing = 0.12.em,
                color = labelColor,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = body,
                fontFamily = geist,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = instrumentOnBg,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun HDictSensePreview() {
    HelloTheme {
        Surface(color = instrumentBg) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                HDictSense(
                    index = 1,
                    label = "Definición",
                    body = "Concerned with beauty or the appreciation of beauty.",
                )
                HDictSense(
                    index = 2,
                    label = "Cuándo usarlo",
                    body = "Adjetivo neutro para describir el atractivo visual de cosas o " +
                        "ambientes. Más común en contextos de diseño que en el habla diaria.",
                )
                HDictSense(
                    index = 3,
                    label = "Por qué es útil",
                    body = "Permite hablar de gusto visual sin opinar sobre belleza intrínseca.",
                )
                HDictSense(
                    index = 4,
                    label = "Error común",
                    body = "No usar como sinónimo de “bonito” a secas; es más específico.",
                    tone = HDictSenseTone.Warn,
                )
                HDictSense(
                    index = 5,
                    label = "No confundir con",
                    body = "Ascetic — austero, espiritualmente desapegado. Suena parecido pero opuesto.",
                    tone = HDictSenseTone.Warn,
                )
            }
        }
    }
}
