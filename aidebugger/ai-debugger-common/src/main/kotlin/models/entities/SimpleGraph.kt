package com.intellij.aidebugger.common.models.entities

data class SimpleGraph(
    val nodes: List<SimpleGraphNode>,
    val edges: List<SimpleGraphEdge>
) {
    companion object {
        val Empty: SimpleGraph = SimpleGraph(listOf(), listOf())
    }
}

data class SimpleGraphNode(
    val id: String,
    val type: String?,
    val data: Any?
)

data class SimpleGraphEdge(
    val source: String,
    val target: String
)