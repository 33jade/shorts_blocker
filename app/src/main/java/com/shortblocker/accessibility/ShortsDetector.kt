package com.shortblocker.accessibility

import android.view.accessibility.AccessibilityNodeInfo

class ShortsDetector(
    private val maxVisitedNodes: Int = 350,
    private val maxDepth: Int = 24,
) {

    fun detect(rootNode: AccessibilityNodeInfo): DetectionResult {
        val snapshot = NodeSnapshot.from(
            facts = collectNodeFacts(rootNode, maxVisitedNodes, maxDepth),
        )
        return detect(snapshot)
    }

    internal fun detect(nodeFacts: List<NodeFacts>): DetectionResult =
        detect(NodeSnapshot.from(nodeFacts))

    private fun detect(snapshot: NodeSnapshot): DetectionResult {
        val hasReelRecycler = snapshot.hasViewId(REEL_RECYCLER_ID)
        if (!hasReelRecycler) {
            return DetectionResult(isShortsScreen = false, reason = DetectionReason.NoReelRecycler)
        }

        if (snapshot.hasTextOrDescription(SHORTS_LABEL)) {
            return DetectionResult(isShortsScreen = true, reason = DetectionReason.ReelRecyclerWithShortsLabel)
        }

        if (snapshot.hasViewIdContaining(REEL_PLAYER_OVERLAY_ID_PART) && snapshot.hasShortsActionBar()) {
            return DetectionResult(isShortsScreen = true, reason = DetectionReason.ReelRecyclerWithOverlayAndActionBar)
        }

        return DetectionResult(isShortsScreen = false, reason = DetectionReason.ReelRecyclerWithoutConfirmation)
    }

    private fun collectNodeFacts(
        rootNode: AccessibilityNodeInfo,
        maxVisitedNodes: Int,
        maxDepth: Int,
    ): List<NodeFacts> {
        val nodes = mutableListOf<NodeFacts>()
        val queue = ArrayDeque<NodeWithDepth>()
        queue.add(NodeWithDepth(rootNode, depth = 0))

        while (queue.isNotEmpty() && nodes.size < maxVisitedNodes) {
            val (node, depth) = queue.removeFirst()
            nodes.add(
                NodeFacts(
                    viewIdResourceName = node.viewIdResourceName,
                    text = node.text?.toString(),
                    contentDescription = node.contentDescription?.toString(),
                ),
            )

            if (depth >= maxDepth) continue

            val childCount = node.childCount
            for (index in 0 until childCount) {
                val child = node.getChild(index) ?: continue
                queue.add(NodeWithDepth(child, depth + 1))
            }
        }

        return nodes
    }

    private data class NodeSnapshot(
        val nodes: List<NodeFacts>,
    ) {
        fun hasViewId(viewId: String): Boolean =
            nodes.any { it.viewIdResourceName == viewId }

        fun hasViewIdContaining(value: String): Boolean =
            nodes.any { it.viewIdResourceName?.contains(value, ignoreCase = true) == true }

        fun hasTextOrDescription(value: String): Boolean =
            nodes.any {
                it.text?.contains(value, ignoreCase = true) == true ||
                    it.contentDescription?.contains(value, ignoreCase = true) == true
            }

        fun hasShortsActionBar(): Boolean =
            ACTION_BAR_ID_PARTS.any(::hasViewIdContaining)

        companion object {
            fun from(facts: List<NodeFacts>): NodeSnapshot = NodeSnapshot(facts)
        }
    }

    private data class NodeWithDepth(
        val node: AccessibilityNodeInfo,
        val depth: Int,
    )

    companion object {
        private const val REEL_RECYCLER_ID = "com.google.android.youtube:id/reel_recycler"
        private const val REEL_PLAYER_OVERLAY_ID_PART = "reel_player_overlay"
        private const val SHORTS_LABEL = "Shorts"

        private val ACTION_BAR_ID_PARTS = listOf(
            "reel_right_discovery_action_bar",
            "right_action_bar",
            "reel_like_button",
            "reel_dislike_button",
            "reel_comments_button",
            "reel_share_button",
        )
    }
}

internal data class NodeFacts(
    val viewIdResourceName: String? = null,
    val text: String? = null,
    val contentDescription: String? = null,
)

data class DetectionResult(
    val isShortsScreen: Boolean,
    val reason: DetectionReason,
)

enum class DetectionReason {
    NoReelRecycler,
    ReelRecyclerWithShortsLabel,
    ReelRecyclerWithOverlayAndActionBar,
    ReelRecyclerWithoutConfirmation,
}
