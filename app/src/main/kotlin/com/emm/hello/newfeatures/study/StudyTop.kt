package com.emm.hello.newfeatures.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.emm.hello.R
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.instrumentBg
import com.emm.hello.core.theme.instrumentMuted
import com.emm.hello.core.theme.geistMono
import com.emm.hello.core.ui.HIconButton
import com.emm.hello.core.ui.HProgressBar

private val barHeight = 56.dp
private val closeButtonSize = 44.dp
private val stateLabelReservedHeight = 22.dp

@Composable
internal fun StudyTop(
    progress: Float,
    currentCount: Int,
    totalCount: Int,
    stateLabel: String?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    showCounter: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HIconButton(
                icon = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.exit_session_desc),
                onClick = onClose,
                iconSize = 20.dp,
                buttonSize = closeButtonSize,
            )

            HProgressBar(
                progress = progress,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            )

            if (showCounter && totalCount > 0) {
                Text(
                    text = "$currentCount/$totalCount",
                    fontFamily = geistMono,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    letterSpacing = 0.08.em,
                    color = instrumentMuted,
                    modifier = Modifier.padding(end = 12.dp),
                )
            } else {
                Spacer(Modifier.size(closeButtonSize))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(stateLabelReservedHeight),
            contentAlignment = Alignment.Center,
        ) {
            if (!stateLabel.isNullOrBlank()) {
                Text(
                    text = stateLabel,
                    fontFamily = geistMono,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.5.sp,
                    letterSpacing = 0.12.em,
                    color = instrumentMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun StudyTopPreview() {
    HelloTheme {
        Surface(color = instrumentBg) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                StudyTop(
                    progress = 0f,
                    currentCount = 0,
                    totalCount = 0,
                    stateLabel = null,
                    onClose = {},
                    showCounter = false,
                )
                StudyTop(
                    progress = 0.3f,
                    currentCount = 3,
                    totalCount = 10,
                    stateLabel = "RECORDAR",
                    onClose = {},
                )
                StudyTop(
                    progress = 0.9f,
                    currentCount = 9,
                    totalCount = 10,
                    stateLabel = "RESPUESTA",
                    onClose = {},
                )
            }
        }
    }
}
