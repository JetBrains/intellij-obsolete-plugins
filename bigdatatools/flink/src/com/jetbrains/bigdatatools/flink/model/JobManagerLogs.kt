package com.jetbrains.bigdatatools.flink.model

data class JobManagerLogs(
  val logs: List<LogFileInfo> = emptyList()
)