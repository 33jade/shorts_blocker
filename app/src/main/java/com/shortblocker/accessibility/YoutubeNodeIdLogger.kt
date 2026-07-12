package com.shortblocker.accessibility

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

internal class YoutubeNodeIdLogger(
    private val tag: String,
    private val maxVisitedNodes: Int = 350,
    private val maxDepth: Int = 24,
) {

    fun log(rootNode: AccessibilityNodeInfo, reason: DetectionReason) {
        val viewIds = collectViewIds(rootNode)
        if (viewIds.isEmpty()) {
            Log.d(tag, "YouTube node IDs for $reason: none")
            return
        }

        viewIds
            .chunked(LOG_IDS_PER_LINE)
            .forEachIndexed { index, chunk ->
                Log.d(
                    tag,
                    "YouTube node IDs for $reason [${index + 1}]: ${chunk.joinToString()}",
                )
            }
    }

    private fun collectViewIds(rootNode: AccessibilityNodeInfo): List<String> {
        val viewIds = linkedSetOf<String>()
        val queue = ArrayDeque<NodeWithDepth>()
        queue.add(NodeWithDepth(rootNode, depth = 0))
        var visitedNodes = 0

        while (queue.isNotEmpty() && visitedNodes < maxVisitedNodes) {
            val (node, depth) = queue.removeFirst()
            visitedNodes += 1

            node.viewIdResourceName
                ?.takeIf { it.startsWith(YOUTUBE_ID_PREFIX) }
                ?.let(viewIds::add)

            if (depth >= maxDepth) continue

            for (index in 0 until node.childCount) {
                val child = node.getChild(index) ?: continue
                queue.add(NodeWithDepth(child, depth + 1))
            }
        }

        return viewIds.toList()
    }

    private data class NodeWithDepth(
        val node: AccessibilityNodeInfo,
        val depth: Int,
    )

    private companion object {
        const val YOUTUBE_ID_PREFIX = "com.google.android.youtube:id/"
        const val LOG_IDS_PER_LINE = 12
    }
}
