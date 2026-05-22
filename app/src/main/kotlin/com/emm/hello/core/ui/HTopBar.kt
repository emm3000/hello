package com.emm.hello.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.emberBg
import com.emm.hello.core.theme.emberMuted
import com.emm.hello.core.theme.emberPrimary
import com.emm.hello.core.theme.geist
import com.emm.hello.core.theme.geistMono

private val topBarHeight = 56.dp
private val backButtonSize = 44.dp

/**
 * Ember top bar. 56dp tall, optional back button (44dp circular hit area),
 * optional centered title (Geist 16sp) or uppercase mono label ([mono] = true → Geist Mono 11sp),
 * optional trailing [actions] slot. Transparent bg by default.
 */
@Composable
fun HTopBar(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    title: String? = null,
    mono: Boolean = false,
    transparent: Boolean = true,
    actions: @Composable (() -> Unit)? = null,
) {
    val bg = if (transparent) Color.Transparent else emberBg

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(topBarHeight)
            .background(bg)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(backButtonSize),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = emberPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
        } else {
            Spacer(Modifier.size(backButtonSize))
        }

        if (title != null) {
            if (mono) {
                Text(
                    text = title.uppercase(),
                    fontFamily = geistMono,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    letterSpacing = 0.08.em,
                    color = emberMuted,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = backButtonSize),
                )
            } else {
                Text(
                    text = title,
                    fontFamily = geist,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = emberPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = if (actions == null) backButtonSize else 0.dp),
                )
            }
        } else {
            Spacer(Modifier.weight(1f))
        }

        if (actions != null) {
            actions()
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun HTopBarPreview() {
    HelloTheme {
        Surface(color = emberBg) {
            HTopBar(
                onBack = {},
                title = "Detalle del mazo",
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun HTopBarMonoPreview() {
    HelloTheme {
        Surface(color = emberBg) {
            HTopBar(
                onBack = {},
                title = "Ajustes",
                mono = true,
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Más opciones",
                            tint = emberPrimary,
                        )
                    }
                },
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun HTopBarNoBackPreview() {
    HelloTheme {
        Surface(color = emberBg) {
            HTopBar(title = "Inicio")
        }
    }
}
