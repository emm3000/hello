package com.emm.hello.newfeatures.dashboard

import android.annotation.SuppressLint
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

@SuppressLint("LocalContextGetResourceValueCall")
fun NavGraphBuilder.quote() {
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
        val context = LocalContext.current

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
                    android.widget.Toast.makeText(
                        context,
                        context.getString(com.emm.hello.R.string.quote_saved_default_deck),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                } else {
                    android.widget.Toast.makeText(
                        context,
                        context.getString(com.emm.hello.R.string.quote_default_deck_required),
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
