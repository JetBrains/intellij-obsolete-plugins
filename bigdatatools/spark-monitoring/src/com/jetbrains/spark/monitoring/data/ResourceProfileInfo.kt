package com.jetbrains.spark.monitoring.data

data class ResourceProfileInfo(
  val id: Int = -1,
  val executorResources: Map<String, ExecutorResourceRequest> = emptyMap(),
  val taskResources: Map<String, TaskResourceRequest> = emptyMap()
)