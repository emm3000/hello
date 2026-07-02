package com.emm.hello.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.emberAccent
import com.emm.hello.core.theme.emberBg
import com.emm.hello.core.theme.emberDivider
import com.emm.hello.core.theme.emberMuted
import com.emm.hello.core.theme.geistMono

private val barHeight = 56.dp
private val backButtonSize = 44.dp
private val segmentHeight = 3.dp
private val segmentGap = 4.dp

/**
 * Wizard top bar: optional back arrow, N-segment progress, `current/total` mono counter,
 * and an optional uppercase mono subtitle below. Designed for multi-step flows like
 * New Card wizard. Transparent background.
 *
 * @param currentStep 1-based index of the active step.
 * @param totalSteps total step count (segments rendered = this value).
 */
@Composable
fun HWizTop(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    subtitle: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                HIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    onClick = onBack,
                    iconSize = 20.dp,
                    buttonSize = backButtonSize,
                )
            } else {
                Spacer(Modifier.size(backButtonSize))
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(segmentGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(totalSteps) { index ->
                    val isActive = index < currentStep
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(segmentHeight)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isActive) emberAccent else emberDivider),
                    )
                }
            }

            Text(
                text = "$currentStep/$totalSteps",
                fontFamily = geistMono,
                fontSize = 11.sp,
                letterSpacing = 0.08.em,
                color = emberMuted,
                modifier = Modifier.padding(end = 12.dp),
            )
        }

        if (subtitle != null) {
            Text(
                text = subtitle.uppercase(),
                fontFamily = geistMono,
                fontSize = 10.5.sp,
                letterSpacing = 0.12.em,
                color = emberMuted,
                modifier = Modifier.padding(start = backButtonSize + 12.dp, bottom = 6.dp),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun HWizTopPreview() {
    HelloTheme {
        Surface(color = emberBg) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                HWizTop(
                    currentStep = 1,
                    totalSteps = 3,
                    onBack = {},
                )
                HWizTop(
                    currentStep = 2,
                    totalSteps = 3,
                    onBack = {},
                    subtitle = "Nueva tarjeta",
                )
                HWizTop(
                    currentStep = 3,
                    totalSteps = 3,
                    onBack = {},
                    subtitle = "Revisar",
                )
            }
        }
    }
}
