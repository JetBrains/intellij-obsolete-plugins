package com.intellij.bigdatatools.emr.submit

import com.intellij.openapi.project.Project
import com.jetbrains.spark.submit.run.common.AbstractSparkCommandLineModel

class EmrSparkCommandLineModel(
  project: Project,
  configuration: EmrSparkJobRunConfiguration
) : AbstractSparkCommandLineModel<EmrSparkJobRunConfiguration>(project, configuration) {

  override val isLocal: Boolean = false

  override fun getSparkSubmitPath() = "spark-submit"
}