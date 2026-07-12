package com.youtubeshortblocker.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class ShortsAllowanceTrackerTest {

    @Test
    fun firstDetectionDoesNotConsumeTime() {
        val tracker = ShortsAllowanceTracker()

        assertEquals(0L, tracker.recordShortsDetected(nowElapsedMs = 1_000L))
    }

    @Test
    fun secondDetectionConsumesElapsedTime() {
        val tracker = ShortsAllowanceTracker()
        tracker.recordShortsDetected(nowElapsedMs = 1_000L)

        assertEquals(750L, tracker.recordShortsDetected(nowElapsedMs = 1_750L))
    }

    @Test
    fun singleUsageIsCapped() {
        val tracker = ShortsAllowanceTracker(maxSingleUsageMs = 5_000L)
        tracker.recordShortsDetected(nowElapsedMs = 1_000L)

        assertEquals(5_000L, tracker.recordShortsDetected(nowElapsedMs = 30_000L))
    }

    @Test
    fun resetClearsPreviousDetection() {
        val tracker = ShortsAllowanceTracker()
        tracker.recordShortsDetected(nowElapsedMs = 1_000L)
        tracker.reset()

        assertEquals(0L, tracker.recordShortsDetected(nowElapsedMs = 2_000L))
    }
}
