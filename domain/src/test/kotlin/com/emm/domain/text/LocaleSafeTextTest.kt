package com.emm.domain.text

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class LocaleSafeTextTest {

    @Test
    fun `lowercaseRoot is stable under Turkish locale`() {
        val previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("tr-TR"))

        try {
            assertEquals("iğdir", "IĞDIR".lowercaseRoot())
        } finally {
            Locale.setDefault(previousLocale)
        }
    }
}
