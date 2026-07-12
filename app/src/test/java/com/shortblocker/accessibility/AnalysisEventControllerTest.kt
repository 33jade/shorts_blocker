package com.shortblocker.accessibility

import org.junit.Assert.assertEquals
import org.junit.Test

class AnalysisEventControllerTest {

    @Test
    fun windowStateChangedStartsAnalysisImmediately() {
        val environment = FakeEnvironment(nowMs = 1_000L)
        val controller = AnalysisEventController(environment, analysisIntervalMs = 300L)

        controller.request(AnalysisEventKind.WindowStateChanged)

        assertEquals(1, environment.startCount)
        assertEquals(emptyList<Long>(), environment.scheduledDelays)
    }

    @Test
    fun windowContentChangedStartsImmediatelyWhenIntervalHasElapsed() {
        val environment = FakeEnvironment(nowMs = 1_000L)
        val controller = AnalysisEventController(environment, analysisIntervalMs = 300L)

        controller.request(AnalysisEventKind.WindowContentChanged)

        assertEquals(1, environment.startCount)
        assertEquals(emptyList<Long>(), environment.scheduledDelays)
    }

    @Test
    fun windowContentChangedIsThrottledWhenIntervalHasNotElapsed() {
        val environment = FakeEnvironment(nowMs = 1_000L)
        val controller = AnalysisEventController(environment, analysisIntervalMs = 300L)
        controller.request(AnalysisEventKind.WindowContentChanged)
        controller.onAnalysisFinished()

        environment.nowMs = 1_100L
        controller.request(AnalysisEventKind.WindowContentChanged)

        assertEquals(1, environment.startCount)
        assertEquals(listOf(200L), environment.scheduledDelays)
    }

    @Test
    fun scheduledAnalysisStartsWhenDue() {
        val environment = FakeEnvironment(nowMs = 1_000L)
        val controller = AnalysisEventController(environment, analysisIntervalMs = 300L)
        controller.request(AnalysisEventKind.WindowContentChanged)
        controller.onAnalysisFinished()

        environment.nowMs = 1_100L
        controller.request(AnalysisEventKind.WindowContentChanged)
        environment.nowMs = 1_300L
        controller.onScheduledAnalysisDue()

        assertEquals(2, environment.startCount)
    }

    @Test
    fun repeatedContentEventsReuseSingleScheduledAnalysis() {
        val environment = FakeEnvironment(nowMs = 1_000L)
        val controller = AnalysisEventController(environment, analysisIntervalMs = 300L)
        controller.request(AnalysisEventKind.WindowContentChanged)
        controller.onAnalysisFinished()

        environment.nowMs = 1_050L
        controller.request(AnalysisEventKind.WindowContentChanged)
        environment.nowMs = 1_100L
        controller.request(AnalysisEventKind.WindowContentChanged)

        assertEquals(1, environment.startCount)
        assertEquals(listOf(250L), environment.scheduledDelays)
    }

    @Test
    fun windowStateChangedCancelsScheduledAnalysisAndStartsImmediately() {
        val environment = FakeEnvironment(nowMs = 1_000L)
        val controller = AnalysisEventController(environment, analysisIntervalMs = 300L)
        controller.request(AnalysisEventKind.WindowContentChanged)
        controller.onAnalysisFinished()

        environment.nowMs = 1_100L
        controller.request(AnalysisEventKind.WindowContentChanged)
        controller.request(AnalysisEventKind.WindowStateChanged)

        assertEquals(2, environment.startCount)
        assertEquals(1, environment.cancelCount)
    }

    @Test
    fun eventDuringAnalysisIsCollectedAsTrailingAnalysis() {
        val environment = FakeEnvironment(nowMs = 1_000L)
        val controller = AnalysisEventController(environment, analysisIntervalMs = 300L)
        controller.request(AnalysisEventKind.WindowContentChanged)

        environment.nowMs = 1_100L
        controller.request(AnalysisEventKind.WindowContentChanged)
        controller.onAnalysisFinished()

        assertEquals(1, environment.startCount)
        assertEquals(listOf(200L), environment.scheduledDelays)
    }

    @Test
    fun trailingAnalysisStartsImmediatelyWhenIntervalHasElapsed() {
        val environment = FakeEnvironment(nowMs = 1_000L)
        val controller = AnalysisEventController(environment, analysisIntervalMs = 300L)
        controller.request(AnalysisEventKind.WindowContentChanged)

        environment.nowMs = 1_300L
        controller.request(AnalysisEventKind.WindowContentChanged)
        controller.onAnalysisFinished()

        assertEquals(2, environment.startCount)
    }

    private class FakeEnvironment(
        var nowMs: Long,
    ) : AnalysisEventController.Environment {

        var startCount = 0
        var cancelCount = 0
        val scheduledDelays = mutableListOf<Long>()

        override fun elapsedRealtimeMs(): Long = nowMs

        override fun startAnalysis() {
            startCount += 1
        }

        override fun scheduleAnalysis(delayMs: Long) {
            scheduledDelays.add(delayMs)
        }

        override fun cancelScheduledAnalysis() {
            cancelCount += 1
        }
    }
}
