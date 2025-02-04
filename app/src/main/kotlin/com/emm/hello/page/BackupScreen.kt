package com.emm.hello.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
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
import com.emm.hello.ui.theme.HelloTheme

@Composable
fun BackupScreen(
    internalOnSave: () -> Unit,
    internalOnUpdate: () -> Unit,
    internalOnDelete: () -> Unit,
    externalOnSave: () -> Unit,
    externalOnUpdate: () -> Unit,
    externalOnDelete: () -> Unit,
    sharedOnSave: () -> Unit,
    sharedOnUpdate: () -> Unit,
    sharedOnDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FirstBlock(
            title = "Internal Storage App",
            onSave = internalOnSave,
            onUpdate = internalOnUpdate,
            onDelete = internalOnDelete,
        )
        FirstBlock(
            "External Storage App",
            onSave = externalOnSave,
            onUpdate = externalOnUpdate,
            onDelete = externalOnDelete,
        )
        FirstBlock(
            title = "Shared Storage",
            onSave = sharedOnSave,
            onUpdate = sharedOnUpdate,
            onDelete = sharedOnDelete,
        )
    }
}

@Composable
private fun FirstBlock(
    title: String,
    onSave: () -> Unit,
    onUpdate: () -> Unit,
    onDelete: () -> Unit,
) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FilledTonalButton(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10),
            onClick = {
                onSave()
            }
        ) {
            Text(
                text = "Save",
                fontSize = 18.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold
            )
        }
        FilledTonalButton(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10),
            onClick = {
                onUpdate()
            }
        ) {
            Text(
                text = "Update",
                fontSize = 18.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold
            )
        }
        FilledTonalButton(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10),
            onClick = {
                onDelete()
            }
        ) {
            Text(
                text = "Delete",
                fontSize = 18.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun BackupScreenPreview() {
    HelloTheme {
        BackupScreen(
            internalOnSave = {},
            internalOnUpdate = {},
            internalOnDelete = {},
            externalOnSave = {},
            externalOnUpdate = {},
            externalOnDelete = {},
            sharedOnSave = {},
            sharedOnUpdate = {},
            sharedOnDelete = {},
        )
    }
}