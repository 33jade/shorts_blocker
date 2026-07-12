package com.youtubeshortblocker.accessibility

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class EvacuationSequenceRunnerTest {

    @Test
    fun exitsAfterFirstBackWhenShortsIsGone() = runBlocking {
        val environment = FakeEnvironment(screenResults = mutableListOf(false))

        EvacuationSequenceRunner(environment).run()

        assertEquals(listOf(EvacuationAction.Back), environment.actions)
        assertEquals(0, environment.homeSuccessCount)
    }

    @Test
    fun runsSecondBackWhenShortsStillContinuesAfterFirstBack() = runBlocking {
        val environment = FakeEnvironment(screenResults = mutableListOf(true, false))

        EvacuationSequenceRunner(environment).run()

        assertEquals(listOf(EvacuationAction.Back, EvacuationAction.Back), environment.actions)
        assertEquals(0, environment.homeSuccessCount)
    }

    @Test
    fun fallsBackHomeWhenShortsStillContinuesAfterTwoBacks() = runBlocking {
        val environment = FakeEnvironment(screenResults = mutableListOf(true, true))

        EvacuationSequenceRunner(environment).run()

        assertEquals(
            listOf(EvacuationAction.Back, EvacuationAction.Back, EvacuationAction.Home),
            environment.actions,
        )
        assertEquals(1, environment.homeSuccessCount)
    }

    @Test
    fun doesNotShowHomeSuccessWhenHomeActionFails() = runBlocking {
        val environment = FakeEnvironment(
            screenResults = mutableListOf(true, true),
            failedActions = setOf(EvacuationAction.Home),
        )

        EvacuationSequenceRunner(environment).run()

        assertEquals(
            listOf(EvacuationAction.Back, EvacuationAction.Back, EvacuationAction.Home),
            environment.actions,
        )
        assertEquals(0, environment.homeSuccessCount)
    }

    @Test
    fun retriesRootOnceAndStopsWhenRootIsStillUnavailable() = runBlocking {
        val environment = FakeEnvironment(screenResults = mutableListOf(null, null))

        EvacuationSequenceRunner(environment).run()

        assertEquals(listOf(EvacuationAction.Back), environment.actions)
        assertEquals(1, environment.rootRetryDelayCount)
        assertEquals(0, environment.homeSuccessCount)
    }

    @Test
    fun retriesRootOnceAndContinuesWhenRetryGetsShortsScreen() = runBlocking {
        val environment = FakeEnvironment(screenResults = mutableListOf(null, true, false))

        EvacuationSequenceRunner(environment).run()

        assertEquals(listOf(EvacuationAction.Back, EvacuationAction.Back), environment.actions)
        assertEquals(1, environment.rootRetryDelayCount)
    }

    @Test
    fun stopsBeforeSecondBackWhenRuntimeBecomesDisabled() = runBlocking {
        val environment = FakeEnvironment(
            screenResults = mutableListOf(true),
            runtimeAllowedResults = mutableListOf(true, false),
        )

        assertThrows(CancellationException::class.java) {
            runBlocking {
                EvacuationSequenceRunner(environment).run()
            }
        }
        assertEquals(listOf(EvacuationAction.Back), environment.actions)
    }

    private class FakeEnvironment(
        private val screenResults: MutableList<Boolean?>,
        private val runtimeAllowedResults: MutableList<Boolean> = mutableListOf(true),
        private val failedActions: Set<EvacuationAction> = emptySet(),
    ) : EvacuationSequenceRunner.Environment {

        val actions = mutableListOf<EvacuationAction>()
        var homeSuccessCount = 0
        var rootRetryDelayCount = 0

        override fun isRuntimeAllowed(): Boolean =
            if (runtimeAllowedResults.size > 1) {
                runtimeAllowedResults.removeAt(0)
            } else {
                runtimeAllowedResults.first()
            }

        override fun performGlobalAction(action: EvacuationAction): Boolean {
            actions.add(action)
            return action !in failedActions
        }

        override fun isCurrentScreenShorts(): Boolean? =
            if (screenResults.isEmpty()) {
                false
            } else {
                screenResults.removeAt(0)
            }

        override fun onHomeSucceeded() {
            homeSuccessCount += 1
        }

        override suspend fun delayAfterBack() = Unit

        override suspend fun delayBeforeRootRetry() {
            rootRetryDelayCount += 1
        }
    }
}
