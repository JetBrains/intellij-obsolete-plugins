package com.jetbrains.bigdatatools.flink.model

data class Jars(
  val address: String,
  val files: List<JarInfo> = emptyList()
)