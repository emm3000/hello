package com.emm.hello.core.audio

object TtsUtterancePolicy {

    fun resolveOnFinish(currentUtterance: String?, finishedUtterance: String?): String? {
        return if (currentUtterance == finishedUtterance) null else currentUtterance
    }
}
