package com.emm.hello.core.audio

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AudioStateTest {

    @Test
    fun `isSpeaking is true when the utterance id matches`() {
        val state = AudioState(speakingUtteranceId = "card_word", isTtsReady = true)

        assertThat(state.isSpeaking("card_word")).isTrue()
    }

    @Test
    fun `isSpeaking is false for a different id while another utterance speaks`() {
        val state = AudioState(speakingUtteranceId = "study_word", isTtsReady = true)

        assertThat(state.isSpeaking("card_word")).isFalse()
    }

    @Test
    fun `isSpeaking is false when nothing is speaking`() {
        val state = AudioState(speakingUtteranceId = null, isTtsReady = true)

        assertThat(state.isSpeaking("card_word")).isFalse()
    }

    @Test
    fun `isSpeaking does not conflate two buttons whose spoken text is identical`() {
        val state = AudioState(speakingUtteranceId = "card_word", isTtsReady = true)

        assertThat(state.isSpeaking("card_example")).isFalse()
    }
}
