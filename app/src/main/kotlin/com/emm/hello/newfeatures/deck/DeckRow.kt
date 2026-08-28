package com.emm.hello.newfeatures.deck

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import com.emm.domain.deck.Deck
import com.emm.hello.R
import com.emm.hello.core.theme.hairline
import com.emm.hello.core.theme.inkFaint
import com.emm.hello.core.theme.inkMuted
import com.emm.hello.core.theme.ink
import com.emm.hello.core.theme.metadata
import com.emm.hello.core.ui.HCard

@Composable
fun DeckRow(
    deck: Deck,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HCard(
        modifier = modifier.fillMaxWidth(),
        due = false,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 20.dp),
        ) {
            Text(
                text = deck.name,
                style = MaterialTheme.typography.titleLarge,
                color = ink,
            )

            if (deck.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = deck.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = inkMuted,
                )
            }

            Spacer(Modifier.height(14.dp))

            DeckRowFooter(deck = deck)
        }
    }
}

@Composable
private fun DeckRowFooter(deck: Deck) {
    val cardCount: Int = deck.cardsCount.toInt()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = pluralStringResource(R.plurals.cards_count, cardCount, cardCount),
            style = MaterialTheme.typography.metadata,
            color = inkFaint,
        )

        if (deck.tags.isNotEmpty()) {
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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                deck.tags.forEach { tag ->
                    Text(
                        text = tag.value,
                        style = MaterialTheme.typography.metadata,
                        color = inkMuted,
                    )
                }
            }
        }
    }
}
