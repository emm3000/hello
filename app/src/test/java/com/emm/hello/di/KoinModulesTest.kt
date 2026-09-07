package com.emm.hello.di

import android.content.Context
import com.emm.hello.newfeatures.deck.DeckFormMode
import com.google.firebase.ai.GenerativeModel
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify

class KoinModulesTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `every definition in the app modules resolves`() {
        module { includes(repositoryModule, newModule) }.verify(
            extraTypes = listOf(
                Context::class,
                GenerativeModel::class,
                Function1::class,
                DoubleArray::class,
                DeckFormMode::class,
            ),
        )
    }
}
