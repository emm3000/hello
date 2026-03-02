
package com.emm.hello.newfeatures.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.domain.flashcard.Example
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardReview
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.ui.ButtonVariant
import com.emm.hello.core.ui.CardVariant
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HCard
import com.emm.hello.core.ui.HSeparator

@Composable
fun FlashcardDetailScreen(
    modifier: Modifier = Modifier,
    flashcard: Flashcard = Flashcard.Empty,
    onNavigateBack: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        flashcard.word,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Implement edit */ }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { /* TODO: Implement delete */ }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Main card ─────────────────────────────────────────────
            HCard(
                modifier = Modifier.fillMaxWidth(),
                variant = CardVariant.Elevated,
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = flashcard.word,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = flashcard.phonetic,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(Modifier.height(8.dp))
                    HSeparator()
                    Spacer(Modifier.height(12.dp))

                    DetailItem(label = "Traducción", value = flashcard.translation)
                    Spacer(Modifier.height(12.dp))
                    DetailItem(label = "Significado", value = flashcard.meaning)

                    if (flashcard.examples.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        HSeparator()
                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "Ejemplos",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(8.dp))

                        flashcard.examples.forEachIndexed { index, example ->
                            key(example.exampleId) {
                                ExampleItem(index = index + 1, example = example)
                                if (index < flashcard.examples.lastIndex) {
                                    Spacer(Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ExampleItem(index: Int, example: Example) {
    var showTranslation by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "$index. ${example.text}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (showTranslation) {
            Text(
                text = example.translation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HButton(
                text = if (showTranslation) "Ocultar traducción" else "Ver traducción",
                onClick = { showTranslation = !showTranslation },
                variant = ButtonVariant.Ghost,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CardDetailScreenPreview() {
    HelloTheme {
        val sampleCard = Flashcard(
            id = "1",
            word = "Aesthetic",
            phonetic = "/esˈTHedik/",
            translation = "Estético",
            meaning = "Concerned with beauty or the appreciation of beauty.",
            examples = listOf(
                Example(
                    exampleId = "ex1",
                    text = "The new building has a very aesthetic design.",
                    translation = "El nuevo edificio tiene un diseño muy estético.",
                    type = "",
                ),
                Example(
                    exampleId = "ex2",
                    text = "Her Instagram page is very aesthetic.",
                    translation = "Su página de Instagram es muy estética.",
                    type = "",
                ),
            ),
            review = FlashcardReview.Empty,
        )
        FlashcardDetailScreen(flashcard = sampleCard)
    }
}
