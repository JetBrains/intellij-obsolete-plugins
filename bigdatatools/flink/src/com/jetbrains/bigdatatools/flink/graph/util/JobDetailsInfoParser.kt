package com.jetbrains.bigdatatools.flink.graph.util

import com.jetbrains.bigdatatools.flink.graph.FlinkDiagramProvider
import com.jetbrains.bigdatatools.flink.graph.model.FlinkEdge
import com.jetbrains.bigdatatools.flink.graph.model.FlinkNode
import com.jetbrains.bigdatatools.flink.model.Plan

class JobDetailsInfoParser {
  val nodesWithId = mutableMapOf<String, FlinkNode>()
  val edges = mutableListOf<FlinkEdge>()

  init {
    // init nodes
    plan?.nodes?.forEach { nodesWithId[it.id] = FlinkNode(it, FlinkDiagramProvider) }

    // init edges
    nodesWithId.values.forEach { to ->
      to.identifyingElement.inputs.forEach { from ->
        edges.add(FlinkEdge(getNode(from.id), to, from.shipStrategy))
      }
    }
  }

  private fun getNode(nodeId: String): FlinkNode {
    return nodesWithId[nodeId] ?: throw Exception("Graph initialization exception:\nThere is no $nodeId in graph")
  }

  companion object {
    var plan: Plan? = null
  }
}