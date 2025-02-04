package com.emm.hello.features.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.data.WordEntity
import com.emm.hello.core.theme.HelloTheme
import java.util.UUID

@Composable
fun DetailScreen(
    wordEntity: WordEntity,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = wordEntity.word.uppercase(),
            color = MaterialTheme.colorScheme.onBackground,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 25.sp
        )
    }
}

@PreviewLightDark
@Composable
private fun DetailScreenPreview() {
    HelloTheme {
        DetailScreen(
            wordEntity = WordEntity(
                id = UUID.randomUUID().toString(),
                word = "random word",
                createdAt = ""
            )
        )
    }
}