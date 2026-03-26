package com.emm.hello.newfeatures.card

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emm.hello.core.ui.HSeparator

@Composable
fun NewCardBottomBar(
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        androidx.compose.foundation.layout.Column {
            HSeparator()
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.padding(PaddingValues(horizontal = 16.dp, vertical = 12.dp))
            ) {
                content()
            }
        }
    }
}
