package com.intellij.bigdatatools.emr.submit

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.jetbrains.spark.submit.model.ClusterManagerType
import com.jetbrains.spark.submit.model.DeployModeType
import com.jetbrains.spark.submit.run.common.AbstractSparkJobRunConfiguration
import software.amazon.awssdk.services.emr.model.ActionOnFailure

class EmrSparkJobRunConfiguration(project: Project,
                                  factory: ConfigurationFactory,
                                  name: String) : AbstractSparkJobRunConfiguration<EmrSparkCommandLineModel>(project, factory, name) {
  var actionOnFailure: ActionOnFailure = ActionOnFailure.CONTINUE

  init {
    setName("Spark-submit")
    clusterManager = ClusterManagerType.YARN
    deployMode = DeployModeType.CLUSTER
  }

  override fun getConfigurationEditor() = throw UnsupportedOperationException()

  override fun getState() = EmrSparkCommandLineModel(project, this)

  override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? = null
}