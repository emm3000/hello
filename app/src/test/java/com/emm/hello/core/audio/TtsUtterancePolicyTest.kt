package com.emm.hello.core.audio

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TtsUtterancePolicyTest {

    @Test
    fun `resolveOnFinish clears the state when the finished utterance is the current one`() {
        assertThat(TtsUtterancePolicy.resolveOnFinish("hello", "hello")).isNull()
    }

    @Test
    fun `resolveOnFinish keeps the current utterance when a stale utterance finishes`() {
        assertThat(TtsUtterancePolicy.resolveOnFinish("hello", "goodbye")).isEqualTo("hello")
    }

    @Test
    fun `resolveOnFinish returns null when nothing is speaking and nothing finished`() {
        assertThat(TtsUtterancePolicy.resolveOnFinish(null, null)).isNull()
    }

    @Test
    fun `resolveOnFinish keeps the current utterance when a stale null utterance finishes`() {
        assertThat(TtsUtterancePolicy.resolveOnFinish("hello", null)).isEqualTo("hello")
    }
}
