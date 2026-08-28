package com.emm.hello.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.ink
import com.emm.hello.core.theme.inkFaint
import com.emm.hello.core.theme.inkMuted
import com.emm.hello.core.theme.metadata
import com.emm.hello.core.theme.pageBackground
import com.emm.hello.core.theme.spacing

@Composable
fun HEmptyState(
    headline: String,
    modifier: Modifier = Modifier,
    accentWord: String? = null,
    body: String? = null,
    footnote: String? = null,
    primaryCta: @Composable (() -> Unit)? = null,
    ghostCta: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.xxl, vertical = MaterialTheme.spacing.xl),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        val headlineText: AnnotatedString = accentWord
            ?.takeIf { it.isNotEmpty() && headline.contains(it) }
            ?.let { word ->
                val before: String = headline.substringBefore(word)
                val after: String = headline.substringAfter(word)
                buildAnnotatedString {
                    append(before)
                    withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                        append(word)
                    }
                    append(after)
                }
            }
            ?: AnnotatedString(headline)

        Text(
            text = headlineText,
            style = MaterialTheme.typography.headlineLarge,
            color = ink,
            textAlign = TextAlign.Start,
        )

        if (body != null) {
            Spacer(Modifier.height(MaterialTheme.spacing.md))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = inkMuted,
                textAlign = TextAlign.Start,
            )
        }

        if (primaryCta != null) {
            Spacer(Modifier.height(MaterialTheme.spacing.xl))
            primaryCta()
        }

        if (ghostCta != null) {
            Spacer(Modifier.height(MaterialTheme.spacing.sm))
            ghostCta()
        }

        if (footnote != null) {
            Spacer(Modifier.weight(1f, fill = true))
            Text(
                text = footnote.uppercase(),
                style = MaterialTheme.typography.metadata,
                color = inkFaint,
                textAlign = TextAlign.Start,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun HEmptyStateDefaultPreview() {
    HelloTheme {
        Surface(color = pageBackground) {
            HEmptyState(
                headline = "Aún no tienes mazos.",
                body = "Crea tu primer mazo para empezar a estudiar.",
                primaryCta = {
                    HButton(
                        text = "Crear mazo",
                        onClick = {},
                        variant = HButtonVariant.Primary,
                        full = true,
                    )
                },
                ghostCta = {
                    HButton(
                        text = "Importar backup",
                        onClick = {},
                        variant = HButtonVariant.Text,
                        full = true,
                    )
                },
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun HEmptyStateAccentWordPreview() {
    HelloTheme {
        Surface(color = pageBackground) {
            HEmptyState(
                headline = "Nada con \"serendipia\".",
                accentWord = "\"serendipia\"",
                body = "Prueba otra palabra o crea una tarjeta con esta.",
                primaryCta = {
                    HButton(
                        text = "Crear tarjeta",
                        onClick = {},
                        variant = HButtonVariant.Primary,
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
        Surface(color = pageBackground) {
            HEmptyState(
                headline = "Hoy no toca repasar.",
                body = "Vuelve mañana para continuar con tu racha.",
                footnote = "próxima sesión en 18 h",
            )
        }
    }
}
