package com.jetbrains.bigdatatools.flink.model

data class Plan(
  val jid: String,
  val name: String,
  val type: String,
  val nodes: List<Node> = emptyList()
)