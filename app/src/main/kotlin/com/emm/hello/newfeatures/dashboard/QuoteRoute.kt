package com.emm.hello.newfeatures.dashboard

import android.annotation.SuppressLint
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.emm.domain.quote.Quote
import com.emm.domain.quote.QuoteRepository
import com.emm.domain.quote.SaveQuoteAsFlashcardCommand
import com.emm.domain.quote.SaveQuoteAsFlashcardResult
import com.emm.domain.quote.SaveQuoteAsFlashcardUseCase
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
        val saveQuoteAsFlashcardUseCase: SaveQuoteAsFlashcardUseCase = koinInject()

        val quotes: List<Quote> by quotesRepository.allQuotes().collectAsStateWithLifecycle(emptyList())

        val scope = rememberCoroutineScope()
        val context = LocalContext.current

        QuotesScreen(quotes) {
            scope.launch {
                when (saveQuoteAsFlashcardUseCase(SaveQuoteAsFlashcardCommand.fromQuote(it))) {
                    SaveQuoteAsFlashcardResult.Saved -> {
                        android.widget.Toast.makeText(
                            context,
                            context.getString(com.emm.hello.R.string.quote_saved_default_deck),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }

                    SaveQuoteAsFlashcardResult.DefaultDeckRequired -> {
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
}
