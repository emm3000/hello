package com.emm.hello.legacy.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.hello.core.theme.HelloTheme

@Composable
fun NoContent(
    showLoading: Boolean,
    generateContent: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(bottom = 15.dp),
            text = "Content not found, can you generate content?",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.ExtraBold
        )

        Box(
            modifier = Modifier
                .padding(bottom = 150.dp)
                .height(70.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (showLoading) {
                LettersLoading()
            } else {
                OutlinedButton(
                    onClick = generateContent,
                    shape = RoundedCornerShape(20)
                ) {
                    Text(
                        text = "Generate content",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun NoContentPrev() {
    HelloTheme {
        Surface {
            NoContent(true) { }
        }
    }
}

@PreviewLightDark
@Composable
private fun NoContent2Prev() {
    HelloTheme {
        Surface {
            NoContent(false) { }
        }
    }
}