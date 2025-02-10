package com.emm.hello.features.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.emm.domain.SourceType
import com.emm.domain.Word
import com.emm.domain.WordContent
import com.emm.hello.core.theme.HelloTheme
import java.util.UUID

@Composable
fun IaScreen(
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
        if (state.iaContentWord != null) {
            IaScreen(state.iaContentWord)
        } else {
            NoContent(
                showLoading = state.isLoading,
                generateContent = { generateContent(SourceType.IA) }
            )
        }
    }
}

@Composable
fun IaScreen(contentWord: WordContent) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = contentWord.pos,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@PreviewLightDark
@Composable
private fun IaScreenPreview() {
    HelloTheme {
        Surface {
            IaScreen(
                generateContent = {},
                state = DetailUiState(
                    currentWord = Word(
                        id = "non",
                        word = "consectetur",
                        hasContent = true,
                        createdAt = 0L
                    ),
                    iaContentWord = WordContent(
                        wordContentId = "",
                        word = "gaa",
                        pos = UUID.randomUUID().toString().repeat(100),
                        sourceType = SourceType.SCRAPPING,
                        examples = listOf()
                    )
                )
            )
        }
    }
}