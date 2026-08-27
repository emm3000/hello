package com.emm.hello.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.instrumentAccent
import com.emm.hello.core.theme.instrumentBg
import com.emm.hello.core.theme.instrumentDivider
import com.emm.hello.core.theme.instrumentFaint
import com.emm.hello.core.theme.instrumentMuted
import com.emm.hello.core.theme.instrumentOnBg
import com.emm.hello.core.theme.geist
import com.emm.hello.core.theme.geistMono
import com.emm.hello.core.theme.instrumentSerif

/**
 * Reusable empty state. Optional glyph slot, serif headline, muted body,
 * optional CTA slots, optional mono footnote.
 *
 * Default glyph: `·` centered in a 64dp bordered circle with accent Instrument Serif italic 32sp.
 * [headline] supports an [accentWord] for inline accent color (e.g. "Nada con \"X\"").
 */
@Composable
fun HEmptyState(
    headline: String,
    modifier: Modifier = Modifier,
    accentWord: String? = null,
    body: String? = null,
    footnote: String? = null,
    glyph: @Composable (() -> Unit)? = null,
    primaryCta: @Composable (() -> Unit)? = null,
    ghostCta: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        if (glyph != null) {
            glyph()
        } else {
            DefaultGlyph()
        }

        Spacer(Modifier.height(24.dp))

        if (accentWord != null && accentWord.isNotEmpty() && headline.contains(accentWord)) {
            val before = headline.substringBefore(accentWord)
            val after = headline.substringAfter(accentWord)
            val styledText = buildAnnotatedString {
                append(before)
                withStyle(
                    SpanStyle(
                        color = instrumentAccent,
                        fontStyle = FontStyle.Italic,
                        fontSynthesis = FontSynthesis.None,
                    ),
                ) { append(accentWord) }
                append(after)
            }
            Text(
                text = styledText,
                fontFamily = instrumentSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 38.sp,
                lineHeight = 40.sp,
                letterSpacing = (-0.5).sp,
                color = instrumentOnBg,
                textAlign = TextAlign.Start,
            )
        } else {
            Text(
                text = headline,
                fontFamily = instrumentSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 38.sp,
                lineHeight = 40.sp,
                letterSpacing = (-0.5).sp,
                color = instrumentOnBg,
                textAlign = TextAlign.Start,
            )
        }

        if (body != null) {
            Spacer(Modifier.height(14.dp))
            Text(
                text = body,
                fontFamily = geist,
                fontWeight = FontWeight.Normal,
                fontSize = 13.5.sp,
                lineHeight = 19.sp,
                color = instrumentMuted,
                textAlign = TextAlign.Start,
            )
        }

        if (primaryCta != null) {
            Spacer(Modifier.height(26.dp))
            primaryCta()
        }

        if (ghostCta != null) {
            Spacer(Modifier.height(8.dp))
            ghostCta()
        }

        if (footnote != null) {
            Spacer(Modifier.weight(1f, fill = true))
            Text(
                text = footnote.uppercase(),
                fontFamily = geistMono,
                fontWeight = FontWeight.Normal,
                fontSize = 10.5.sp,
                letterSpacing = 0.12.em,
                color = instrumentFaint,
                textAlign = TextAlign.Start,
            )
        }
    }
}

@Composable
private fun DefaultGlyph() {
    Surface(
        shape = CircleShape,
        color = instrumentBg,
        border = BorderStroke(1.dp, instrumentDivider),
        modifier = Modifier.size(52.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "·",
                fontFamily = instrumentSerif,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Normal,
                fontSize = 28.sp,
                color = instrumentAccent,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun HEmptyStateDefaultPreview() {
    HelloTheme {
        Surface(color = instrumentBg) {
            HEmptyState(
                headline = "Aún no tienes mazos.",
                body = "Crea tu primer mazo para empezar a estudiar.",
                primaryCta = {
                    HButton(
                        text = "Crear mazo",
                        onClick = {},
                        variant = HButtonVariant.Accent,
                        full = true,
                    )
                },
                ghostCta = {
                    HButton(
                        text = "Importar backup",
                        onClick = {},
                        variant = HButtonVariant.Ghost,
                        full = true,
                    )
                },
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun HEmptyStateSearchPreview() {
    HelloTheme {
        Surface(color = instrumentBg) {
            HEmptyState(
                headline = "Nada con \"serendipia\".",
                accentWord = "\"serendipia\"",
                body = "Prueba otra palabra o crea una tarjeta con esta.",
                footnote = "la búsqueda distingue tildes",
                primaryCta = {
                    HButton(
                        text = "Crear tarjeta",
                        onClick = {},
                        variant = HButtonVariant.Accent,
                        full = true,
                    )
                },
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun HEmptyStateWithFootnotePreview() {
    HelloTheme {
        Surface(color = instrumentBg) {
            HEmptyState(
                headline = "Hoy no toca repasar.",
                body = "Vuelve mañana para continuar con tu racha.",
                footnote = "próxima sesión en 18 h",
            )
        }
    }
}
