package com.emm.hello.newfeatures.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.emm.data.remote.DataStore
import com.emm.domain.flashcard.CreateFlashcardInput
import com.emm.domain.flashcard.FlashcardRepository
import com.emm.domain.quote.Quote
import com.emm.domain.quote.QuoteRepository
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
object QuoteRoute

fun NavGraphBuilder.quote(navController: NavController) {
    composable<QuoteRoute>(
        deepLinks = listOf(
            navDeepLink { uriPattern = "gema://quotes" }
        )
    ) {
        val quotesRepository: QuoteRepository = koinInject()
        val cardRepository: FlashcardRepository = koinInject()
        val dataStore: DataStore = koinInject()

        val quotes: List<Quote> by quotesRepository.allQuotes().collectAsStateWithLifecycle(emptyList())

        val scope = rememberCoroutineScope()

        QuotesScreen(quotes) {
            scope.launch {
                if (dataStore.defaultDeck.isNotEmpty()) {
                    val input = CreateFlashcardInput(
                        id = it.id,
                        deckId = dataStore.defaultDeck,
                        word = it.phrase,
                        meaning = it.description,
                        translation = it.translation,
                        phonetic = it.pronunciation
                    )
                    cardRepository.create(input)
                }
            }
        }
    }
}
