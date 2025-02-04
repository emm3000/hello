package com.emm.hello.features.backup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.hello.core.theme.HelloTheme

@Composable
fun BackupScreen(
    exportAsJson: () -> Unit,
    populateDb: () -> Unit,
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
        Text(
            text = "Backup",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = exportAsJson,
            shape = RoundedCornerShape(15),
            contentPadding = PaddingValues(vertical = 15.dp)
        ) {
            Text(
                text = "Exportar datos como JSON",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = populateDb,
            shape = RoundedCornerShape(15),
            contentPadding = PaddingValues(vertical = 15.dp)
        ) {
            Text(
                text = "Importar datos como JSON",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun BackupScreenPreview() {
    HelloTheme {
        BackupScreen(
            exportAsJson = {},
            populateDb = {},
        )
    }
}