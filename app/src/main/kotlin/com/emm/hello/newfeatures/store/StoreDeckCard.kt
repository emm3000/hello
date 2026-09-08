package com.emm.hello.newfeatures.store

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.emm.domain.generation.LevelBand
import com.emm.hello.R
import com.emm.hello.core.theme.hairline
import com.emm.hello.core.theme.ink
import com.emm.hello.core.theme.inkFaint
import com.emm.hello.core.theme.inkMuted
import com.emm.hello.core.theme.metadata
import com.emm.hello.core.theme.successInk
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HButtonVariant
import com.emm.hello.core.ui.HCard

@Composable
fun StoreDeckCard(
    item: StoreDeckItem,
    isInstalling: Boolean,
    onInstall: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HCard(
        modifier = modifier.fillMaxWidth(),
        due = false,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 20.dp),
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleLarge,
                color = ink,
            )

            if (item.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = inkMuted,
                )
            }

            Spacer(Modifier.height(14.dp))

            StoreDeckCardFooter(item = item)

            Spacer(Modifier.height(16.dp))

            StoreDeckCardAction(
                isInstalled = item.isInstalled,
                isInstalling = isInstalling,
                onInstall = onInstall,
                onOpen = onOpen,
            )
        }
    }
}

@Composable
private fun StoreDeckCardFooter(item: StoreDeckItem) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        StoreDeckCardMeta(item = item)
        if (!item.isInstalled && item.tags.isNotEmpty()) {
            Text(
                text = item.tags.joinToString(separator = " · ") { tag: String -> tag.uppercase() },
                style = MaterialTheme.typography.metadata,
                color = inkMuted,
            )
        }
    }
}

@Composable
private fun StoreDeckCardMeta(item: StoreDeckItem) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        if (item.isInstalled) {
            Text(
                text = stringResource(R.string.store_installed).uppercase(),
                style = MaterialTheme.typography.metadata,
                color = successInk,
            )
            FooterDivider()
        }

        Text(
            text = pluralStringResource(R.plurals.cards_count, item.cardsCount, item.cardsCount).uppercase(),
            style = MaterialTheme.typography.metadata,
            color = inkFaint,
        )

        FooterDivider()

        Text(
            text = stringResource(item.levelBand.labelRes()).uppercase(),
            style = MaterialTheme.typography.metadata,
            color = inkMuted,
        )
    }
}

@Composable
private fun StoreDeckCardAction(
    isInstalled: Boolean,
    isInstalling: Boolean,
    onInstall: () -> Unit,
    onOpen: () -> Unit,
) {
    if (isInstalled) {
        HButton(
            text = stringResource(R.string.store_open_deck),
            onClick = onOpen,
            variant = HButtonVariant.Secondary,
            full = true,
        )
    } else {
        HButton(
            text = stringResource(R.string.store_install),
            onClick = onInstall,
            variant = HButtonVariant.Secondary,
            enabled = !isInstalling,
            isLoading = isInstalling,
            icon = Icons.Outlined.Download,
            full = true,
        )
    }
}

@Composable
private fun FooterDivider() {
    Canvas(
        modifier = Modifier
            .width(14.dp)
            .height(1.dp),
    ) {
        drawLine(
            color = hairline,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = 1.dp.toPx(),
        )
    }
}

@StringRes
private fun LevelBand.labelRes(): Int = when (this) {
    LevelBand.A1_A2 -> R.string.store_level_a1_a2
    LevelBand.B1_B2 -> R.string.store_level_b1_b2
    LevelBand.C1_PLUS -> R.string.store_level_c1_plus
}
