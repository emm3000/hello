package com.emm.hello.newfeatures.dashboard

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.domain.quote.Quote
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.ui.BadgeVariant
import com.emm.hello.core.ui.ButtonVariant
import com.emm.hello.core.ui.CardVariant
import com.emm.hello.core.ui.HBadge
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HCard
import com.emm.hello.core.ui.HSeparator

@Composable
fun QuotesScreen(
    quotes: List<Quote>,
    createCard: (Quote) -> Unit = {},
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        items(quotes, key = Quote::id) { quote ->
            QuoteItem(quote = quote, onClick = createCard)
        }
    }
}

@Composable
fun QuoteItem(quote: Quote, onClick: (Quote) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    HCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { expanded = !expanded },
        variant = CardVariant.Elevated,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = quote.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = quote.phrase,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = if (expanded) "Contraer" else "Expandir",
                    modifier = Modifier.rotate(if (expanded) 180f else 0f),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (expanded) {
                Spacer(Modifier.height(12.dp))
                HSeparator()
                Spacer(Modifier.height(12.dp))

                QuoteDetailRow(
                    icon = Icons.Default.Translate,
                    title = "Translation",
                    content = quote.translation,
                )
                QuoteDetailRow(
                    icon = Icons.Default.Info,
                    title = "Description",
                    content = quote.description,
                )
                QuoteDetailRow(
                    icon = Icons.Default.School,
                    title = "Example",
                    content = quote.example,
                )
                QuoteDetailRow(
                    icon = Icons.Default.RecordVoiceOver,
                    title = "Pronunciation",
                    content = quote.pronunciation,
                )
                if (quote.category.isNotEmpty()) {
                    QuoteDetailRow(
                        icon = Icons.Default.Category,
                        title = "Category",
                        content = quote.category,
                    )
                }

                if (quote.tags.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        quote.tags.forEach { tag ->
                            SuggestionChip(onClick = {}, label = { Text(tag) })
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                HButton(
                    text = if (quote.hasCard) "Ya tiene tarjeta" else "Crear flashcard",
                    onClick = { onClick(quote) },
                    enabled = !quote.hasCard,
                    variant = if (quote.hasCard) ButtonVariant.Secondary else ButtonVariant.Default,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (quote.hasCard) {
                    Spacer(Modifier.height(4.dp))
                    HBadge(
                        label = "Flashcard ya creada",
                        variant = BadgeVariant.Success,
                    )
                }
            }
        }
    }
}

@Composable
fun QuoteDetailRow(icon: ImageVector, title: String, content: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Preview(showBackground = true, name = "Quotes Screen Light")
@Composable
fun QuotesScreenPreviewLight() {
    HelloTheme(darkTheme = false) {
        Surface {
            QuotesScreen(quotes = sampleQuotes)
        }
    }
}

@Preview(showBackground = true, name = "Quotes Screen Dark")
@Composable
fun QuotesScreenPreviewDark() {
    HelloTheme(darkTheme = true) {
        Surface {
            QuotesScreen(quotes = sampleQuotes)
        }
    }
}

private val sampleQuotes = listOf(
    Quote(
        id = "1",
        title = "Carpe Diem",
        phrase = "Seize the day",
        description = "A Latin aphorism, usually translated 'seize the day'.",
        translation = "Aprovecha el día",
        example = "I'm going to go skydiving, carpe diem!",
        context = "Motivational",
        pronunciation = "/ˌkɑːrpeɪ ˈdiːɛm/",
        formality = "Informal",
        tags = listOf("motivation", "latin", "classic"),
        category = "Philosophy",
        hasCard = false,
    ),
    Quote(
        id = "2",
        title = "Veni, Vidi, Vici",
        phrase = "I came, I saw, I conquered",
        description = "A Latin phrase attributed to Julius Caesar.",
        translation = "Vine, vi, vencí",
        example = "After the successful product launch, the CEO said 'Veni, vidi, vici'.",
        context = "Achievement",
        pronunciation = "/ˈweːniː ˈwiːdiː ˈwiːkiː/",
        formality = "Formal",
        tags = listOf("history", "latin", "victory"),
        category = "History",
        hasCard = true,
    ),
)
