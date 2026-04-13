package com.emm.hello.newfeatures.card

import com.emm.domain.flashcard.CreateFlashcardUseCase
import com.emm.domain.flashcard.GenerateLearningNotePreviewUseCase
import com.emm.domain.flashcard.RegenerateLearningNoteClozeUseCase
import com.emm.domain.flashcard.RegenerateLearningNoteExampleUseCase
import com.emm.domain.flashcard.RegenerateLearningNoteFieldUseCase
import com.emm.domain.flashcard.RegenerateStudyCardUseCase
import com.emm.domain.flashcard.ValidateFlashcardGenerationInputUseCase
import com.emm.domain.flashcard.ValidateGeneratedLearningNoteUseCase

data class NewCardGenerationDependencies(
    val createFlashcardUseCase: CreateFlashcardUseCase,
    val generateLearningNotePreviewUseCase: GenerateLearningNotePreviewUseCase,
    val regenerateLearningNoteExampleUseCase: RegenerateLearningNoteExampleUseCase,
    val regenerateLearningNoteClozeUseCase: RegenerateLearningNoteClozeUseCase,
    val regenerateLearningNoteFieldUseCase: RegenerateLearningNoteFieldUseCase,
    val regenerateStudyCardUseCase: RegenerateStudyCardUseCase,
    val validateInputUseCase: ValidateFlashcardGenerationInputUseCase,
    val validateGeneratedLearningNoteUseCase: ValidateGeneratedLearningNoteUseCase,
)
