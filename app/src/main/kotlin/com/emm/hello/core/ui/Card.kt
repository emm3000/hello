package com.emm.hello.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.ink
import com.emm.hello.core.theme.pageBackground
import com.emm.hello.core.theme.hairline
import com.emm.hello.core.theme.surface

@Composable
fun HCard(
    modifier: Modifier = Modifier,
    due: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = modifier) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = surface,
            border = BorderStroke(1.dp, hairline),
        ) {
            Column(content = content)
        }
        if (due) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(
                        color = ink,
                        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                    ),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun HCardPreview() {
    HelloTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(pageBackground)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Serendipity",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
            HCard(modifier = Modifier.fillMaxWidth(), due = true) {
                Text(
                    text = "Ephemeral",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}
