package com.jetbrains.spark.submit.run.local

import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.jetbrains.bigdatatools.common.integration.python.BdtPythonIntegration
import com.jetbrains.spark.submit.model.FilePath
import com.jetbrains.spark.submit.model.FileType
import com.jetbrains.spark.submit.run.common.AbstractSparkCommandLineModel

class LocalSparkCommandLineModel(
  project: Project,
  configuration: LocalSparkJobRunConfiguration
) : AbstractSparkCommandLineModel<LocalSparkJobRunConfiguration>(project, configuration) {
  override val isLocal: Boolean = true

  override fun getRealPath(filePath: FilePath): FilePath? {
    val superPath = super.getRealPath(filePath) ?: return null
    return if (superPath.type == FileType.FILE && SystemInfo.isWindows && !ApplicationManager.getApplication().isUnitTestMode) {
      FilePath(path = superPath.path.replace('/', '\\'))
    }
    else
      superPath
  }

  override fun getSparkSubmitPath() = configuration.sparkHome + if (SystemInfo.isWindows && !ApplicationManager.getApplication().isUnitTestMode)
    "/bin/spark-submit.cmd"
  else
    "/bin/spark-submit"

  override val commandSeparator = if (SystemInfo.isWindows && !ApplicationManager.getApplication().isUnitTestMode)
    "&"
  else
    super.commandSeparator

  override fun artifactPath(): String {
    val artifactPath = super.artifactPath()
    return if (SystemInfo.isMac)
      artifactPath.removePrefix("file://")
    else
      artifactPath
  }

  override fun getEnvs(): Map<String, String> {
    val envParams = super.getEnvs()

    return if (BdtPythonIntegration.isEnabled() && configuration.pythonSdkPath.isNotBlank())
      envParams + mapOf("PYSPARK_PYTHON" to configuration.pythonSdkPath)
    else
      envParams
  }
}

class LocalSparkRunConfigurationProfileState(
  val myProfileState: LocalSparkCommandLineModel,
  environment: ExecutionEnvironment
) : CommandLineState(environment) {
  override fun startProcess(): ProcessHandler {
    return KillableColoredProcessHandler.Silent(myProfileState.createCommandLineWithEnv(isDebugMode = false)).also { handler ->
      ProcessTerminatedListener.attach(handler)
    }
  }
}