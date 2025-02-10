package com.emm.hello.features.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.domain.Example
import com.emm.domain.SourceType
import com.emm.domain.Word
import com.emm.domain.WordContent
import com.emm.hello.core.theme.HelloTheme

@Composable
fun DictionaryScreen(
    state: DetailUiState,
    generateContent: (SourceType) -> Unit,
    modifier: Modifier = Modifier,
) {

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
    ) {
        if (state.scrapContentWord != null) {
            DictionaryScreen(state.scrapContentWord)
        } else {
            NoContent(
                showLoading = state.isLoading,
                generateContent = { generateContent(SourceType.SCRAPPING) }
            )
        }
    }
}

@Composable
private fun DictionaryScreen(contentWord: WordContent) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = contentWord.word,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = contentWord.pos,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        items(contentWord.examples) {
            ContentItem(it)
        }
    }
}

@Composable
private fun ContentItem(example: Example) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(2),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.8f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(13.dp)
        ) {
            Text(
                text = "${example.number}. ${example.title}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            if (example.sentences.isNotEmpty()) Spacer(Modifier.height(10.dp))
            example.sentences.forEach { sentences ->
                SentencesItem(sentences)
            }
        }
    }
}

@Composable
private fun SentencesItem(it: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "✅",
            fontSize = 15.sp,
        )
        Text(
            text = it,
            fontSize = 16.sp,
            fontWeight = FontWeight.Light,
            color = LocalContentColor.current.copy(alpha = 0.7f)
        )
    }
}

@PreviewLightDark
@Composable
private fun DictionaryScreenPreview() {
    HelloTheme {
        Surface {
            DictionaryScreen(
                generateContent = {},
                state = DetailUiState(
                    currentWord = Word(
                        id = "non",
                        word = "consectetur",
                        hasContent = true,
                        createdAt = 0L
                    ),
                    scrapContentWord = WordContent(
                        wordContentId = "",
                        word = "gaa",
                        pos = "gaaa x2",
                        sourceType = SourceType.SCRAPPING,
                        examples = listOf(
                            Example(
                                number = "1",
                                title = "random title",
                                sentences = listOf(
                                    "random title d ASLCK AS KNCASL CNals kcas lm",
                                    "random title 3"
                                )
                            ), Example(
                                number = "1",
                                title = "random title",
                                sentences = listOf()
                            )
                        )
                    )
                )
            )
        }
    }
}