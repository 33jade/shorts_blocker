package com.youtubeshortblocker.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortsDetectorTest {

    private val detector = ShortsDetector()

    @Test
    fun reelRecyclerWithShortsTextIsShortsScreen() {
        val result = detector.detect(
            listOf(
                node(viewId = REEL_RECYCLER_ID),
                node(text = "Shorts"),
            ),
        )

        assertTrue(result.isShortsScreen)
        assertEquals(DetectionReason.ReelRecyclerWithShortsLabel, result.reason)
    }

    @Test
    fun reelRecyclerWithShortsContentDescriptionIsShortsScreen() {
        val result = detector.detect(
            listOf(
                node(viewId = REEL_RECYCLER_ID),
                node(contentDescription = "Shorts player"),
            ),
        )

        assertTrue(result.isShortsScreen)
        assertEquals(DetectionReason.ReelRecyclerWithShortsLabel, result.reason)
    }

    @Test
    fun reelRecyclerWithOverlayAndActionBarIsShortsScreen() {
        val result = detector.detect(
            listOf(
                node(viewId = REEL_RECYCLER_ID),
                node(viewId = "com.google.android.youtube:id/reel_player_overlay"),
                node(viewId = "com.google.android.youtube:id/reel_like_button"),
            ),
        )

        assertTrue(result.isShortsScreen)
        assertEquals(DetectionReason.ReelRecyclerWithOverlayAndActionBar, result.reason)
    }

    @Test
    fun shortsLabelWithoutReelRecyclerIsNotShortsScreen() {
        val result = detector.detect(
            listOf(
                node(text = "Shorts"),
                node(contentDescription = "Shorts"),
            ),
        )

        assertFalse(result.isShortsScreen)
        assertEquals(DetectionReason.NoReelRecycler, result.reason)
    }

    @Test
    fun reelRecyclerWithoutConfirmationIsNotShortsScreen() {
        val result = detector.detect(
            listOf(
                node(viewId = REEL_RECYCLER_ID),
                node(text = "Home"),
            ),
        )

        assertFalse(result.isShortsScreen)
        assertEquals(DetectionReason.ReelRecyclerWithoutConfirmation, result.reason)
    }

    @Test
    fun overlayWithoutActionBarIsNotShortsScreen() {
        val result = detector.detect(
            listOf(
                node(viewId = REEL_RECYCLER_ID),
                node(viewId = "com.google.android.youtube:id/reel_player_overlay"),
            ),
        )

        assertFalse(result.isShortsScreen)
        assertEquals(DetectionReason.ReelRecyclerWithoutConfirmation, result.reason)
    }

    private fun node(
        viewId: String? = null,
        text: String? = null,
        contentDescription: String? = null,
    ): NodeFacts =
        NodeFacts(
            viewIdResourceName = viewId,
            text = text,
            contentDescription = contentDescription,
        )

    private companion object {
        const val REEL_RECYCLER_ID = "com.google.android.youtube:id/reel_recycler"
    }
}
