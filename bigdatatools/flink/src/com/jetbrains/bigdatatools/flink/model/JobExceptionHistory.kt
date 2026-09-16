package com.jetbrains.bigdatatools.flink.model

data class JobExceptionHistory(
  val entries: List<JobExceptionEntry> = emptyList(),
  val truncated: Boolean?
)