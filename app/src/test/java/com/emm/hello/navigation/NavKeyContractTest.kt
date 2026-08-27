package com.emm.hello.navigation

import androidx.navigation3.runtime.NavKey
import com.emm.hello.newfeatures.card.CardDetailRoute
import com.emm.hello.newfeatures.card.EditFlashcardRoute
import com.emm.hello.newfeatures.card.NewCardRoute
import com.emm.hello.newfeatures.hoy.HoyRoute
import com.emm.hello.newfeatures.deck.DecksRoute
import com.emm.hello.newfeatures.deck.NewDeckRoute
import com.emm.hello.newfeatures.settings.SettingsRoute
import com.emm.hello.newfeatures.study.StudyRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavKeyContractTest {

    @Test
    fun `all routes implement NavKey`() {
        val routes: List<Any> = listOf(
            HoyRoute,
            StudyRoute("test"),
            NewCardRoute,
            NewDeckRoute(),
            DecksRoute,
            CardDetailRoute("test", "deck-test"),
            EditFlashcardRoute("test", "deck-test"),
            SettingsRoute,
        )

        routes.forEach { route ->
            assertTrue(
                "${route::class.simpleName} should implement NavKey",
                route is NavKey
            )
        }
    }

    @Test
    fun `parameterized routes preserve args`() {
        val study = StudyRoute("deck-123")
        val deck = DecksRoute
        val card = CardDetailRoute("card-789", "deck-789")
        val editFlashcard = EditFlashcardRoute("card-789", "deck-789")

        assertTrue(study is NavKey)
        assertTrue(deck is NavKey)
        assertTrue(card is NavKey)
        assertTrue(editFlashcard is NavKey)
        assertEquals("deck-123", study.deckId)
    }

    @Test
    fun `StudyRoute defaults to the all-decks target`() {
        val allDecks = StudyRoute()

        assertTrue(allDecks is NavKey)
        assertNull(allDecks.deckId)
    }
}
