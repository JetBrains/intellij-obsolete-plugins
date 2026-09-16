package com.jetbrains.spark.submit.run.common

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.packaging.artifacts.ArtifactManager
import com.intellij.util.execution.ParametersListUtil
import com.jetbrains.spark.submit.model.FilePath
import com.jetbrains.spark.submit.model.FileType
import com.jetbrains.spark.submit.run.cluster.ClusterSparkJobRunConfiguration
import java.io.File

abstract class AbstractSparkCommandLineModel<C : AbstractSparkJobRunConfiguration<*>>(val project: Project, val configuration: C) {
  protected abstract val isLocal: Boolean

  protected open val commandSeparator = ";"

  protected abstract fun getSparkSubmitPath(): String

  protected open fun getRealPath(filePath: FilePath): FilePath? = when (filePath.type) {
    FileType.ARTIFACT -> {
      val artifactPath = ArtifactManager.getInstance(project).findArtifact(filePath.path)?.outputFilePath
      artifactPath?.let { FilePath(FileType.FILE, it) }
    }
    else -> filePath
  }

  protected open fun artifactPath() = transformPath(configuration.artifactPath, false)
  private fun propertyFile() = transformPath(configuration.propertiesFile, false)
  private fun keytab() = transformPath(configuration.keytab, false)
  private fun driverLibraryPath() = configuration.driverLibraryPath.joinToString(separator = ",") { transformPath(it, true) }
  private fun driverClassPath() = configuration.driverClassPath.joinToString(separator = ",") { transformPath(it, true) }
  private fun jars() = configuration.jars.joinToString(separator = ",") { transformPath(it, true) }
  private fun files() = configuration.files.joinToString(separator = ",") { transformPath(it, true) }
  private fun archives() = configuration.archives.joinToString(separator = ",") { transformPath(it, true) }
  private fun pyFiles() = configuration.pyFiles.joinToString(separator = ",") { transformPath(it, true) }

  fun createCommandLineWithEnv(isDebugMode: Boolean): GeneralCommandLine {
    val commandLine = createCommandLine(isDebugMode)

    if (!configuration.workDirectoryPath.isBlank()) {
      // it's not literally correct using File for remote, but it works for ssh also
      commandLine.workDirectory = File(configuration.workDirectoryPath.getAsSelectedPath())
    }

    val environmentVariables = getEnvs()
    return commandLine.withEnvironment(environmentVariables)
  }

  fun createCommandLine(isDebugMode: Boolean): GeneralCommandLine {
    val command = createCommandLineParams(isDebugMode)

    val beforeScript = createBeforeScript()

    val isWindows = isLocal && SystemInfo.isWindows && !ApplicationManager.getApplication().isUnitTestMode
    val commandArgKey = if (isWindows) "/c" else "-c"
    val interactiveModeKey = if (isWindows) "" else "-i"

    val shellExecutor = if (isWindows) "cmd" else configuration.shellExecutor

    return SparkSubmitCommandUtil.createCommandLine(
      shellExecutor = listOf(shellExecutor, commandArgKey).takeIf { configuration.isInteractive || beforeScript.isNotBlank() || !isWindows },
      interactiveModeKey = interactiveModeKey.takeIf { configuration.isInteractive },
      beforeScript = beforeScript.takeIf { it.isNotBlank() },
      commandSeparator = commandSeparator,
      command = command
    )
  }

  protected open fun createBeforeScript() = configuration.beforeShellScript

  fun createCommandLineParams(isDebugMode: Boolean): List<String> {
    val command: MutableList<String> = mutableListOf()

    val sparkSubmit = getSparkSubmitPath()
    command.add(sparkSubmit)

    addIfNotBlank(command, SparkSchema.DRIVER_JAVA_OPTS, configuration.driverJavaOptions)
    if (configuration.verbose)
      command.add(SparkSchema.VERBOSE)
    if (configuration.supervise)
      command.add(SparkSchema.SUPERVISE)
    addIfNotBlank(command, SparkSchema.MASTER, configuration.effectiveMaster())
    addIfNotBlank(command, SparkSchema.DEPLOY_MODE, configuration.effectiveDeployMode().valueForCommand)
    addIfNotBlank(command, SparkSchema.CLAZZ, configuration.className)
    addIfNotBlank(command, SparkSchema.NAME, configuration.name)
    addIfNotBlank(command, SparkSchema.QUEUE, configuration.queue)
    addIfNotBlank(command, SparkSchema.PROXY_USER, configuration.proxyUser)
    addIfNotBlank(command, SparkSchema.PROPERTIES_FILE, propertyFile())
    val configs = ParametersListUtil.parse(configuration.conf, false)
    configs.forEach {
      addIfNotBlank(command, SparkSchema.CONF_PARAM, it)
    }
    val configuration = configuration
    if (configuration is ClusterSparkJobRunConfiguration && configuration.debugSupported() && configuration.debugDriverEnable) {
      val driverKey = "spark.driver.extraJavaOptions"
      val driverSuspend = if (configuration.debugDriverSuspend ?: isDebugMode) "y" else "n"
      val driverPort = configuration.debugDriverPort ?: 0
      val driverValue = "$driverKey=-agentlib:jdwp=transport=dt_socket,server=y,suspend=$driverSuspend,address=$driverPort"
      addIfNotBlank(command, SparkSchema.CONF_PARAM, driverValue)
    }
    addIfNotBlank(command, SparkSchema.JARS, jars())
    addIfNotBlank(command, SparkSchema.PY_FILES, pyFiles())
    addIfNotBlank(command, SparkSchema.FILES, files())
    addIfNotBlank(command, SparkSchema.PACKAGES, configuration.packages)
    addIfNotBlank(command, SparkSchema.REPOSITORIES, configuration.repositories)
    addIfNotBlank(command, SparkSchema.EXCLUDE_PACKAGES, configuration.excludePackages)
    addIfNotBlank(command, SparkSchema.DRIVER_MEMORY, configuration.driverMemory)
    addIfNotBlank(command, SparkSchema.DRIVER_LIB_PATH, driverLibraryPath())
    addIfNotBlank(command, SparkSchema.DRIVER_CLASS_PATH, driverClassPath())
    addIfNotBlank(command, SparkSchema.DRIVER_CORES, configuration.driverCores)
    addIfNotBlank(command, SparkSchema.EXECUTOR_MEMORY, configuration.executorMemory)
    addIfNotBlank(command, SparkSchema.EXECUTOR_CORES, configuration.executorCores)
    addIfNotBlank(command, SparkSchema.NUM_EXECUTORS, configuration.numExecutors)
    addIfNotBlank(command, SparkSchema.TOTAL_EXECUTOR_CORES, configuration.totalExecutorCores)
    addIfNotBlank(command, SparkSchema.ARCHIVES, archives())
    addIfNotBlank(command, SparkSchema.PRINCIPAL, configuration.principal)
    addIfNotBlank(command, SparkSchema.KEYTAB, keytab())
    command.add(artifactPath())
    ParametersListUtil.parse(configuration.artifactArgs).forEach {
      addIfNotBlank(command, "", it)
    }
    return command
  }

  private fun transformPath(filePath: FilePath, keepFilePrefix: Boolean): String {
    if (filePath.path.isBlank()) return ""

    val resPath = getRealPath(filePath) ?: return ""

    val isUrlWithPath = FileType.ALL.any { resPath.path.startsWith(it.scheme) }

    val finalPath = if (isUrlWithPath) {
      // TODO remove branch after ensuring that scheme is never included into path
      Logger.getInstance(AbstractSparkCommandLineModel::class.java).error("Duplicated scheme: ${resPath}. Skipping topmost.")
      resPath.path
    }
    else {
      resPath.toString()
    }
    return if (keepFilePrefix)
      finalPath
    else {
      finalPath.removePrefix(FileType.FILE.scheme)
    }
  }

  private fun addIfNotBlank(command: MutableList<String>, name: String, value: String) {
    if (value.isBlank()) return

    if (name.isNotBlank())
      command.addAll(listOf(name, value))
    else
      command.add(value)
  }

  protected open fun getEnvs() = ParametersListUtil.parse(configuration.envParams).associate { param ->
    val keyValue = param.split("=").map { it.trim() }
    keyValue[0] to keyValue.getOrElse(1) { "" }
  }

  companion object {
    fun isDebug(environment: ExecutionEnvironment): Boolean {
      return environment.executor.id == DefaultDebugExecutor.EXECUTOR_ID
    }
  }

}