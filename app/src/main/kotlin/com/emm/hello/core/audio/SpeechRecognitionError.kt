package com.emm.hello.core.audio

import android.speech.SpeechRecognizer

enum class SpeechRecognitionError {
    NotHeard,
    NoConnection,
    MicrophoneUnavailable,
    PermissionMissing,
    RecognizerBusy,
    Unavailable;

    companion object {
        fun fromCode(errorCode: Int): SpeechRecognitionError {
            return when (errorCode) {
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> NotHeard
                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> NoConnection
                SpeechRecognizer.ERROR_AUDIO -> MicrophoneUnavailable
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> PermissionMissing
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> RecognizerBusy
                else -> Unavailable
            }
        }
    }
}
