package com.emm.hello.features.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
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
import com.emm.data.deprecated.word.WordEntity

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun WordsInFlowColumn(
    words: List<WordEntity>,
    keyboardController: SoftwareKeyboardController?,
    focusManager: FocusManager,
    navigateToDetail: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(top = 10.dp, bottom = 100.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        words.forEach { wordEntity ->
            key(wordEntity.id) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20))
                        .border(
                            width = 1.dp,
                            color = if (wordEntity.hasContent) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            } else MaterialTheme.colorScheme.inverseOnSurface,
                            shape = RoundedCornerShape(20)
                        )
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .clickable {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            navigateToDetail(wordEntity.id)
                        }
                        .padding(vertical = 5.dp, horizontal = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier,
                        text = wordEntity.word,
                        fontWeight = FontWeight.Medium,
                        fontSize = 17.sp,
                        fontFamily = FontFamily.SansSerif,
                        color = if (wordEntity.hasContent) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        } else LocalContentColor.current,
                        textAlign = TextAlign.Start,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}