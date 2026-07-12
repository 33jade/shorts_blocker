package com.youtubeshortblocker.accessibility

internal class AnalysisEventController(
    private val environment: Environment,
    private val analysisIntervalMs: Long,
) {

    private var isAnalysisRunning = false
    private var scheduledAnalysisPending = false
    private var trailingAnalysisRequested = false
    private var lastAnalysisStartedAtMs = 0L

    fun request(eventKind: AnalysisEventKind) {
        if (isAnalysisRunning) {
            trailingAnalysisRequested = true
            return
        }

        if (eventKind == AnalysisEventKind.WindowStateChanged) {
            environment.cancelScheduledAnalysis()
            scheduledAnalysisPending = false
            startAnalysis()
            return
        }

        val elapsedSinceLastAnalysis = environment.elapsedRealtimeMs() - lastAnalysisStartedAtMs
        val delayMs = analysisIntervalMs - elapsedSinceLastAnalysis
        if (delayMs <= 0L) {
            if (scheduledAnalysisPending) {
                environment.cancelScheduledAnalysis()
            }
            scheduledAnalysisPending = false
            startAnalysis()
        } else if (!scheduledAnalysisPending) {
            scheduledAnalysisPending = true
            environment.scheduleAnalysis(delayMs)
        }
    }

    fun onScheduledAnalysisDue() {
        scheduledAnalysisPending = false
        if (isAnalysisRunning) {
            trailingAnalysisRequested = true
            return
        }

        startAnalysis()
    }

    fun onAnalysisFinished() {
        isAnalysisRunning = false
        val shouldRunTrailingAnalysis = trailingAnalysisRequested
        trailingAnalysisRequested = false

        if (shouldRunTrailingAnalysis) {
            request(AnalysisEventKind.WindowContentChanged)
        }
    }

    private fun startAnalysis() {
        isAnalysisRunning = true
        lastAnalysisStartedAtMs = environment.elapsedRealtimeMs()
        environment.startAnalysis()
    }

    interface Environment {
        fun elapsedRealtimeMs(): Long
        fun startAnalysis()
        fun scheduleAnalysis(delayMs: Long)
        fun cancelScheduledAnalysis()
    }
}

internal enum class AnalysisEventKind {
    WindowStateChanged,
    WindowContentChanged,
}
