package com.emm.hello.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.ink
import com.emm.hello.core.theme.pageBackground
import com.emm.hello.core.theme.surface

private val toggleWidth = 40.dp
private val toggleHeight = 22.dp
private val thumbSize = 16.dp
private val toggleShape = RoundedCornerShape(11.dp)

@Composable
fun HToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) ink else surface,
        animationSpec = spring(),
        label = "toggle_track",
    )
    val thumbOffsetX by animateFloatAsState(
        targetValue = if (checked) (toggleWidth - thumbSize - 5.dp).value else 3f,
        animationSpec = spring(stiffness = 500f),
        label = "toggle_thumb",
    )

    Box(
        modifier = modifier
            .width(toggleWidth)
            .height(toggleHeight)
            .clip(toggleShape)
            .background(trackColor)
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .semantics {
                role = Role.Switch
                stateDescription = if (checked) "activado" else "desactivado"
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffsetX.dp)
                .size(thumbSize)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun HTogglePreview() {
    HelloTheme {
        Surface(color = pageBackground) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                var on1 by remember { mutableStateOf(true) }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HToggle(checked = on1, onCheckedChange = { on1 = it })
                    Text("Activado", color = androidx.compose.ui.graphics.Color.White)
                }

                var on2 by remember { mutableStateOf(false) }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HToggle(checked = on2, onCheckedChange = { on2 = it })
                    Text("Desactivado", color = androidx.compose.ui.graphics.Color.White)
                }

                HToggle(checked = true, onCheckedChange = {}, enabled = false)
            }
        }
    }
}
