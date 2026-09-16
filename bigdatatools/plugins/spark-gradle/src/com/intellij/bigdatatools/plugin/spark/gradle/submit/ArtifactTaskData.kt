package com.intellij.bigdatatools.plugin.spark.gradle.submit

import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.ProjectKeys

class ArtifactTaskData(val artifact: String?) {
  companion object {
    @JvmField
    val KEY = Key.create(ArtifactTaskData::class.java, ProjectKeys.TASK.processingWeight + 1)
  }
}