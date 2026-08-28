package com.emm.hello.newfeatures.study

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.hello.R
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.ink
import com.emm.hello.core.theme.pageBackground
import com.emm.hello.core.theme.schibsted
import com.emm.hello.core.ui.HIconButton
import com.emm.hello.core.ui.HProgressBar

private val barMinHeight = 44.dp

@Composable
internal fun StudyTop(
    position: String?,
    progress: Float?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = barMinHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (position != null) {
                Text(
                    text = position,
                    fontFamily = schibsted,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = ink,
                )
            }

            Spacer(Modifier.weight(1f))

            actions()

            HIconButton(
                icon = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.exit_session_desc),
                onClick = onClose,
                iconSize = 24.dp,
                buttonSize = 44.dp,
            )
        }

        if (progress != null) {
            HProgressBar(
                progress = progress,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF4F3F1)
@Composable
private fun StudyTopPreview() {
    HelloTheme {
        Surface(color = pageBackground) {
            Column {
                StudyTop(
                    position = "3 / 10",
                    progress = 0.3f,
                    onClose = {},
                )
                StudyTop(
                    position = null,
                    progress = null,
                    onClose = {},
                )
            }
        }
    }
}
