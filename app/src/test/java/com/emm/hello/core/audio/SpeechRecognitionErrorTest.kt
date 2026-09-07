package com.emm.hello.core.audio

import android.speech.SpeechRecognizer
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SpeechRecognitionErrorTest {

    @Test
    fun `no match and speech timeout both read as not heard`() {
        assertThat(SpeechRecognitionError.fromCode(SpeechRecognizer.ERROR_NO_MATCH))
            .isEqualTo(SpeechRecognitionError.NotHeard)
        assertThat(SpeechRecognitionError.fromCode(SpeechRecognizer.ERROR_SPEECH_TIMEOUT))
            .isEqualTo(SpeechRecognitionError.NotHeard)
    }

    @Test
    fun `network failures read as no connection`() {
        assertThat(SpeechRecognitionError.fromCode(SpeechRecognizer.ERROR_NETWORK))
            .isEqualTo(SpeechRecognitionError.NoConnection)
        assertThat(SpeechRecognitionError.fromCode(SpeechRecognizer.ERROR_NETWORK_TIMEOUT))
            .isEqualTo(SpeechRecognitionError.NoConnection)
    }

    @Test
    fun `audio failure reads as microphone unavailable`() {
        assertThat(SpeechRecognitionError.fromCode(SpeechRecognizer.ERROR_AUDIO))
            .isEqualTo(SpeechRecognitionError.MicrophoneUnavailable)
    }

    @Test
    fun `insufficient permissions reads as permission missing`() {
        assertThat(SpeechRecognitionError.fromCode(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS))
            .isEqualTo(SpeechRecognitionError.PermissionMissing)
    }

    @Test
    fun `recognizer busy keeps its own meaning`() {
        assertThat(SpeechRecognitionError.fromCode(SpeechRecognizer.ERROR_RECOGNIZER_BUSY))
            .isEqualTo(SpeechRecognitionError.RecognizerBusy)
    }

    @Test
    fun `an unknown code falls back to unavailable`() {
        assertThat(SpeechRecognitionError.fromCode(Int.MIN_VALUE))
            .isEqualTo(SpeechRecognitionError.Unavailable)
    }
}
