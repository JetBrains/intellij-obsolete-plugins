package com.jetbrains.spark.submit.run.local

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.jetbrains.spark.submit.run.common.AbstractSparkJobRunConfiguration
import com.jetbrains.spark.submit.run.local.ui.LocalSparkSubmitConfigurationEditor
import com.jetbrains.spark.submit.util.SparkMessagesBundle
import java.io.File

class LocalSparkJobRunConfiguration(
  project: Project,
  val factory: LocalSparkSubmitConfigurationFactory,
  name: String
) : AbstractSparkJobRunConfiguration<LocalSparkCommandLineModel>(project, factory, name) {
  var pythonSdkPath: String = ""

  override fun getConfigurationEditor() = LocalSparkSubmitConfigurationEditor(project, factory)

  override fun getState(executor: Executor, environment: ExecutionEnvironment) =
    LocalSparkRunConfigurationProfileState(LocalSparkCommandLineModel(project, this), environment)

  override fun getState() = LocalSparkCommandLineModel(project, this)

  override fun checkConfiguration() {
    if (sparkHome.isBlank() || !File(sparkHome).exists())
      throw RuntimeConfigurationError(SparkMessagesBundle.message("dialog.message.spark.home.should.be.set.to.correct.folder"))

    super.checkConfiguration()
  }
}