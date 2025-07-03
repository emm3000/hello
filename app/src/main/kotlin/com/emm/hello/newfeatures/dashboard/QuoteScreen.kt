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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.sp
import com.emm.domain.quote.Quote
import com.emm.hello.core.theme.HelloTheme

@Composable
fun QuotesScreen(
    quotes: List<Quote>,
    createCard: (Quote) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        items(quotes, key = Quote::id) { quote ->
            QuoteItem(quote = quote, onClick = createCard)
        }
    }
}

@Composable
fun QuoteItem(quote: Quote, onClick: (Quote) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = quote.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = quote.phrase,
                        style = MaterialTheme.typography.bodyLarge,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Expand",
                    modifier = Modifier.rotate(if (expanded) 180f else 0f)
                )
            }

            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                QuoteDetailRow(icon = Icons.Default.Translate, title = "Translation", content = quote.translation)
                QuoteDetailRow(icon = Icons.Default.Info, title = "Description", content = quote.description)
                QuoteDetailRow(icon = Icons.Default.School, title = "Example", content = quote.example)
                QuoteDetailRow(icon = Icons.Default.RecordVoiceOver, title = "Pronunciation", content = quote.pronunciation)
                if (quote.category.isNotEmpty()) {
                    QuoteDetailRow(icon = Icons.Default.Category, title = "Category", content = quote.category)
                }

                if (quote.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        quote.tags.forEach { tag ->
                            SuggestionChip(onClick = {}, label = { Text(tag) })
                        }
                    }
                }
                Button(
                    onClick = {
                        onClick(quote)
                    },
                    enabled = quote.hasCard.not()
                ) {
                    if (quote.hasCard.not()) {
                        Text("Create Flashcard")
                    } else {
                        Text("Has card")
                    }
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
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(text = content, style = MaterialTheme.typography.bodyMedium)
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
        description = "A Latin aphorism, usually translated 'seize the day', taken from book 1 of the Roman poet Horace's work Odes.",
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
        description = "A Latin phrase popularly attributed to Julius Caesar who, according to Appian, used the phrase in a letter to the Roman Senate around 47 BC after he had achieved a quick victory in his short war against Pharnaces II of Pontus at the Battle of Zela.",
        translation = "Vine, vi, vencí",
        example = "After the successful product launch, the CEO proudly said 'Veni, vidi, vici'.",
        context = "Achievement",
        pronunciation = "/ˈweːniː ˈwiːdiː ˈwiːkiː/",
        formality = "Formal",
        tags = listOf("history", "latin", "victory"),
        category = "History",
        hasCard = false,
    )
)

