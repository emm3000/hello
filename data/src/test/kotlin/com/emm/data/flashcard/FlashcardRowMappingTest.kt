package com.emm.data.flashcard

import com.emm.domain.generation.GeneratedNoteQualityCode
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class FlashcardRowMappingTest {

    private val json = Json

    @Test
    fun `stored quality check with a retired code is dropped`() {
        val raw = """
            [
              {"code": "SingleMeaning", "passed": true, "message": "ok"},
              {"code": "RequiredFieldsPresent", "passed": true, "message": "ok"}
            ]
        """.trimIndent()

        val checks = decodeQualityChecks(raw, json)

        assertEquals(1, checks.size)
        assertEquals(GeneratedNoteQualityCode.SingleMeaning, checks.first().code)
    }
}
