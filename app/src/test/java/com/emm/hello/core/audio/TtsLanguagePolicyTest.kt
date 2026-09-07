package com.emm.hello.core.audio

import android.speech.tts.TextToSpeech
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TtsLanguagePolicyTest {

    @Test
    fun `isUsable is false when the language data is missing`() {
        assertThat(TtsLanguagePolicy.isUsable(TextToSpeech.LANG_MISSING_DATA)).isFalse()
    }

    @Test
    fun `isUsable is false when the language is not supported`() {
        assertThat(TtsLanguagePolicy.isUsable(TextToSpeech.LANG_NOT_SUPPORTED)).isFalse()
    }

    @Test
    fun `isUsable is true when the language is available`() {
        assertThat(TtsLanguagePolicy.isUsable(TextToSpeech.LANG_AVAILABLE)).isTrue()
    }

    @Test
    fun `isUsable is true when country and variant match`() {
        assertThat(TtsLanguagePolicy.isUsable(TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE)).isTrue()
    }
}
