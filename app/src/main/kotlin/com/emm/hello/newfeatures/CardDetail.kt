package com.emm.hello.newfeatures

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.hello.core.theme.HelloTheme

// Data class para todos los detalles de la tarjeta
data class FullCardData(
    val word: String,
    val meaning: String,
    val example: String,
    val translation: String,
    val phonetic: String,
    val audioUrl: String? = null,
    val tags: List<String>
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CardDetailScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {}
) {
    // Datos de ejemplo
    val card = FullCardData(
        word = "Ephemeral",
        meaning = "Lasting for a very short time.",
        example = "The beauty of the cherry blossoms is ephemeral.",
        translation = "Efímero",
        phonetic = "/əˈfem(ə)rəl/",
        audioUrl = "some_url",
        tags = listOf("adjective", "formal", "vocabulary")
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(card.word) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Editar */ }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                    IconButton(onClick = { /* TODO: Borrar */ }) {
                        Icon(Icons.Default.Delete, contentDescription = "Borrar")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { DetailItem("Meaning", card.meaning) }
            item { DetailItem("Example", card.example, isExample = true) }
            item { DetailItem("Translation", card.translation) }
            item { DetailItem("Phonetic", card.phonetic) }

            if (card.audioUrl != null) {
                item { AudioPlayer() }
            }

            if (card.tags.isNotEmpty()) {
                item { TagsSection(tags = card.tags) }
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String, isExample: Boolean = false) {
    Column {
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = if (isExample) MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic) else MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun AudioPlayer() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = { /* TODO: Reproducir audio */ }) {
            Icon(Icons.Default.VolumeUp, contentDescription = "Reproducir audio", modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("Play Pronunciation")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagsSection(tags: List<String>) {
    Column {
        Text("Tags", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tags.forEach { tag ->
                SuggestionChip(onClick = { }, label = { Text(tag) })
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun CardDetailScreenPreview() {
    HelloTheme {
        CardDetailScreen()
    }
}