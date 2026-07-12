package com.shortblocker.settings

internal class ShortsAllowanceTracker(
    private val maxSingleUsageMs: Long = 5_000L,
) {
    private var lastShortsDetectedAtElapsedMs: Long? = null

    fun recordShortsDetected(nowElapsedMs: Long): Long {
        val previousDetectedAt = lastShortsDetectedAtElapsedMs
        lastShortsDetectedAtElapsedMs = nowElapsedMs
        if (previousDetectedAt == null) return 0L

        return (nowElapsedMs - previousDetectedAt)
            .coerceAtLeast(0L)
            .coerceAtMost(maxSingleUsageMs)
    }

    fun reset() {
        lastShortsDetectedAtElapsedMs = null
    }
}
