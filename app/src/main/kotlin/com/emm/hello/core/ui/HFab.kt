package com.emm.hello.core.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.emberAccent
import com.emm.hello.core.theme.geist

private val fabIconFg = Color(0xFF15110D)

/**
 * Ember FAB. Visual-only (caller positions it).
 * 60dp circular when no [label]; pill with label otherwise.
 * emberAccent bg, dark foreground, drop shadow.
 */
@Composable
fun HFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Add,
    label: String? = null,
) {
    if (label != null) {
        FloatingActionButton(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(28.dp),
            containerColor = emberAccent,
            contentColor = fabIconFg,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 14.dp,
                pressedElevation = 6.dp,
            ),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(22.dp),
                )
                Row(Modifier.width(10.dp)) {}
                Text(
                    text = label,
                    fontFamily = geist,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.5.sp,
                    letterSpacing = (-0.1).sp,
                )
            }
        }
    } else {
        FloatingActionButton(
            onClick = onClick,
            modifier = modifier.size(60.dp),
            shape = RoundedCornerShape(30.dp),
            containerColor = emberAccent,
            contentColor = fabIconFg,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 14.dp,
                pressedElevation = 6.dp,
            ),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun HFabPreview() {
    HelloTheme {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HFab(onClick = {})
            Row(Modifier.width(16.dp)) {}
            HFab(onClick = {}, label = "Nueva tarjeta")
        }
    }
}
