package com.emm.hello

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.emm.hello.core.theme.HelloTheme

@Composable
fun RandomScreen(modifier: Modifier = Modifier) {

    val inputText: MutableState<String> = remember {
        mutableStateOf("")
    }

    val lists: SnapshotStateList<String> = remember {
        mutableStateListOf("random", "gg")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(lists) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20))
                        .border(
                            width = 2.dp,
                            color = Color.Green,
                            shape = RoundedCornerShape(20)
                        )
                        .background(Color.Red)
                        .padding(10.dp),
                    text = it,
                )
            }
        }

        Row(
            modifier = Modifier.imePadding(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                value = inputText.value,
                onValueChange = {
                    inputText.value = it
                },
                placeholder = {
                    Text("Ingresa un texto")
                }
            )
            OutlinedIconButton(
                onClick = {
                    lists.add(inputText.value)
                    inputText.value = ""
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.Send,
                    contentDescription = null,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun RandomScreenPrev() {
    HelloTheme {
        Surface {
            RandomScreen()
        }
    }
}
