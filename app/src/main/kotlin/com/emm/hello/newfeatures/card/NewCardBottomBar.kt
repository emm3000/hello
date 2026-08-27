package com.emm.hello.newfeatures.card

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emm.hello.core.theme.instrumentBg
import com.emm.hello.core.ui.HSeparator

@Composable
fun NewCardBottomBar(
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = instrumentBg,
    ) {
        Column(
            modifier = Modifier.navigationBarsPadding(),
        ) {
            HSeparator()
            Box(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                content()
            }
        }
    }
}
