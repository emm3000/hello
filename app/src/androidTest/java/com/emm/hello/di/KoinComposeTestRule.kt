package com.emm.hello.di

import androidx.test.core.app.ApplicationProvider
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module

/**
 * JUnit TestRule that isolates Koin between tests.
 *
 * Stops any existing global Koin context, starts a fresh one with the
 * supplied modules, evaluates the test, then stops Koin again.
 *
 * Must be ordered **before** the Compose rule so Koin is available
 * when `setContent` runs:
 *
 * ```
 * @get:Rule(order = 0)
 * val koinRule = KoinComposeTestRule(newModule, repositoryModule, testModule)
 *
 * @get:Rule(order = 1)
 * val composeRule = createAndroidComposeRule<ComponentActivity>()
 * ```
 */
class KoinComposeTestRule(vararg modules: Module) : TestRule {

    private val modulesArray: Array<out Module> = modules

    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                stopKoin()
                startKoin {
                    androidContext(ApplicationProvider.getApplicationContext())
                    modules(*modulesArray)
                }
                try {
                    base.evaluate()
                } finally {
                    stopKoin()
                }
            }
        }
}
