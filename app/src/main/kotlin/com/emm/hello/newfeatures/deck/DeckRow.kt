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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.emm.domain.deck.Deck
import com.emm.hello.R
import com.emm.hello.core.theme.geistMono
import com.emm.hello.core.theme.instrumentDivider
import com.emm.hello.core.theme.instrumentFaint
import com.emm.hello.core.theme.instrumentMuted
import com.emm.hello.core.theme.instrumentPrimary
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
                fontWeight = FontWeight.Normal,
                fontSize = 22.sp,
                color = instrumentPrimary,
                letterSpacing = (-0.2).sp,
                lineHeight = (22 * 1.1f).sp,
            )

            if (deck.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = deck.description,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    color = instrumentMuted,
                    lineHeight = (14 * 1.4f).sp,
                )
            }

            Spacer(Modifier.height(14.dp))

            DeckRowFooter(deck = deck)
        }
    }
}

@Composable
private fun DeckRowFooter(deck: Deck) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.cards_count, deck.cardsCount),
            fontFamily = geistMono,
            fontWeight = FontWeight.Normal,
            fontSize = 10.5.sp,
            letterSpacing = 0.08.em,
            color = instrumentFaint,
        )

        if (deck.tags.isNotEmpty()) {
            Canvas(
                modifier = Modifier
                    .width(14.dp)
                    .height(1.dp),
            ) {
                drawLine(
                    color = instrumentDivider,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                deck.tags.forEach { tag ->
                    Text(
                        text = tag.value,
                        fontFamily = geistMono,
                        fontWeight = FontWeight.Normal,
                        fontSize = 10.5.sp,
                        letterSpacing = 0.06.em,
                        color = instrumentMuted,
                    )
                }
            }
        }
    }
}
