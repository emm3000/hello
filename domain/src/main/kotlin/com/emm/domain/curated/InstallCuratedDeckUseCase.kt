package com.emm.domain.curated

import com.emm.domain.authoring.CreateFlashcardUseCase
import com.emm.domain.deck.CreateDeckInput
import com.emm.domain.deck.DeckRepository
import com.emm.domain.generation.GeneratedLearningNote
import com.emm.domain.ids.DeckId
import com.emm.domain.validation.DomainValidationException
import com.emm.domain.validation.IssueCode
import kotlinx.coroutines.flow.first

class InstallCuratedDeckUseCase(
    private val catalog: CuratedDeckCatalog,
    private val deckRepository: DeckRepository,
    private val createFlashcardUseCase: CreateFlashcardUseCase,
) {

    suspend operator fun invoke(curatedDeckId: String): DeckId {
        val curated: CuratedDeck = catalog.decks().firstOrNull { it.id == curatedDeckId }
            ?: throw UnknownCuratedDeckException(curatedDeckId)

        val deckId: DeckId = curated.installedDeckId

        createDeckUnlessPresent(curated, deckId)

        curated.notes.forEach { note: GeneratedLearningNote ->
            installNoteUnlessPresent(deckId, note)
        }

        return deckId
    }

    private suspend fun createDeckUnlessPresent(curated: CuratedDeck, deckId: DeckId) {
        if (deckRepository.fetchById(deckId).first() != null) return

        deckRepository.create(
            CreateDeckInput(
                name = curated.name,
                description = curated.description,
                tags = curated.tags,
                id = deckId,
            ),
        )
    }

    private suspend fun installNoteUnlessPresent(deckId: DeckId, note: GeneratedLearningNote) {
        try {
            createFlashcardUseCase(deckId = deckId, learningNote = note)
        } catch (validationFailure: DomainValidationException) {
            if (validationFailure.isNotDuplicateCard()) throw validationFailure
        }
    }

    private fun DomainValidationException.isNotDuplicateCard(): Boolean {
        return issues.none { it.code == IssueCode.DuplicateExactCardInDeck }
    }
}
