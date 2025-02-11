package com.emm.hello.features.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.data.word.WordEntity

@Composable
fun WordsInColumn(
    words: List<WordEntity>,
    keyboardController: SoftwareKeyboardController?,
    focusManager: FocusManager,
    navigateToDetail: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(start = 5.dp, end = 5.dp, bottom = 100.dp),
    ) {
        items(words, key = WordEntity::id) { wordEntity ->
            WordItem(
                keyboardController = keyboardController,
                focusManager = focusManager,
                navigateToDetail = navigateToDetail,
                wordEntity = wordEntity,
            )
        }
    }
}

@Composable
private fun WordItem(
    keyboardController: SoftwareKeyboardController?,
    focusManager: FocusManager,
    navigateToDetail: (String) -> Unit,
    wordEntity: WordEntity
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable {
                keyboardController?.hide()
                focusManager.clearFocus()
                navigateToDetail(wordEntity.id)
            }
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier
                .weight(1f),
            text = wordEntity.word,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            fontFamily = FontFamily.SansSerif,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Start,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
        if (wordEntity.hasContent) {
            Text(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .clip(RoundedCornerShape(20))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(20)
                    )
                    .padding(horizontal = 5.dp),
                text = "content",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            )
        }
        Icon(
            modifier = Modifier.padding(start = 10.dp),
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
        )
    }
}