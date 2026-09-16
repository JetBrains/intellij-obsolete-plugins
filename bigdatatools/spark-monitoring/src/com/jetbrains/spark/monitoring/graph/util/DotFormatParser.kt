package com.jetbrains.spark.monitoring.graph.util

import com.jetbrains.spark.monitoring.graph.DotDiagramProvider
import com.jetbrains.spark.monitoring.graph.model.DotEdge
import com.jetbrains.spark.monitoring.graph.model.DotItem
import com.jetbrains.spark.monitoring.graph.model.DotNode
import com.jetbrains.spark.monitoring.graph.view.DotNodeGroup

class DotFormatParser {
  val nodesWithId = mutableMapOf<String, DotNode>()
  val edges = mutableListOf<DotEdge>()

  init {
    graphs.forEach { initNodesAndEdges(it) }
    externalEdges.forEach { addEdge(it, ",") }
  }

  private fun initNodesAndEdges(dot: String) {
    // split a string using space and when not surrounded by quotes
    val tokens = dot.split(" (?=(?:[^\"]*\"[^\"]*\")*[^\"]*\$)".toRegex())

    val groups = ArrayDeque<DotNodeGroup>()
    var isLeaf = false
    for (i in tokens.indices) {
      if (tokens[i] == "subgraph") {
        if (isLeaf) {
          groups.removeLast()
          isLeaf = false
        }

        val name = tokens[i + 3].textOfNodeLabel()
        val parentGroup = if (groups.isEmpty()) null else groups.last()
        val newGroup = DotNodeGroup(name, parentGroup)
        groups.addLast(newGroup)
      }
      else if (tokens[i].all { it.isDigit() }) {
        if (!isLeaf && isNotParentGroup(groups.last())) {
          isLeaf = true
        }
        val group = groups.last()
        addNode(tokens[i], tokens[i + 2].textOfNodeLabel(), group)
      }
      else if (tokens[i].contains("->")) {
        // nodeIdFrom->nodeIdTo;
        addEdge(tokens[i].trim(';'), "->")
      }
    }
  }

  private fun isNotParentGroup(group: DotNodeGroup) = group.parentGroup != null

  private fun addNode(nodeId: String, name: String, group: DotNodeGroup): DotNode {
    if (nodesWithId[nodeId] != null) {
      throw Exception("Graph initialization exception:\nThere is already $nodeId in graph")
    }
    val node = DotNode(DotItem(nodeId, name, group), DotDiagramProvider)
    nodesWithId[nodeId] = node
    return node
  }

  private fun getNode(nodeId: String): DotNode {
    return nodesWithId[nodeId] ?: throw Exception("Graph initialization exception:\nThere is no $nodeId in graph")
  }

  private fun addEdge(edge: String, splitter: String) {
    val (fromId, toId) = edge.split(splitter)

    val from = getNode(fromId)
    val to = getNode(toId)
    edges.add(DotEdge(from, to))
  }

  private fun String.textOfNodeLabel(): String =
    "<html>${this.trim { it != '"' }.removeSurrounding("\"")}</html>"

  companion object {
    var graphs: List<String> = listOf()
    var externalEdges: List<String> = listOf()
  }
}