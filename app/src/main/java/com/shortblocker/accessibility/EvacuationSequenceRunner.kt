package com.shortblocker.accessibility

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal class EvacuationSequenceRunner(
    private val environment: Environment,
) {

    suspend fun run() {
        ensureBlockingActive()
        environment.performGlobalAction(EvacuationAction.Back)

        environment.delayAfterBack()
        val firstResult = environment.isCurrentScreenShortsWithRetry() ?: return
        if (!firstResult) return

        ensureBlockingActive()
        environment.performGlobalAction(EvacuationAction.Back)

        environment.delayAfterBack()
        val secondResult = environment.isCurrentScreenShortsWithRetry() ?: return
        if (!secondResult) return

        ensureBlockingActive()
        if (environment.performGlobalAction(EvacuationAction.Home)) {
            environment.onHomeSucceeded()
        }
    }

    private suspend fun ensureBlockingActive() {
        currentCoroutineContext().ensureActive()
        if (!environment.isRuntimeAllowed()) {
            throw CancellationException("Blocking is not currently allowed.")
        }
    }

    private suspend fun Environment.isCurrentScreenShortsWithRetry(): Boolean? {
        isCurrentScreenShorts()?.let { return it }
        delayBeforeRootRetry()
        return isCurrentScreenShorts()
    }

    interface Environment {
        fun isRuntimeAllowed(): Boolean
        fun performGlobalAction(action: EvacuationAction): Boolean
        fun isCurrentScreenShorts(): Boolean?
        fun onHomeSucceeded()
        suspend fun delayAfterBack()
        suspend fun delayBeforeRootRetry()
    }
}

internal enum class EvacuationAction {
    Back,
    Home,
}
