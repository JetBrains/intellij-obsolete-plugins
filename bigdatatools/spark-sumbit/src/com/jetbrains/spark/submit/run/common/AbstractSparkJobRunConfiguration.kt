package com.jetbrains.spark.submit.run.common

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.configurations.RuntimeConfigurationException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.packaging.artifacts.ArtifactManager
import com.intellij.util.xmlb.XmlSerializer
import com.jetbrains.spark.submit.model.ClusterManagerType
import com.jetbrains.spark.submit.model.DeployModeType
import com.jetbrains.spark.submit.model.FilePath
import com.jetbrains.spark.submit.model.FileType
import com.jetbrains.spark.submit.run.SparkSubmitConfigurationType
import com.jetbrains.spark.submit.run.cluster.ClusterSparkJobRunConfiguration
import com.jetbrains.spark.submit.settings.RunConfigurationBlockType
import com.jetbrains.spark.submit.util.SparkMessagesBundle
import com.jetbrains.spark.submit.util.SparkSubmitSupportUtils
import org.jdom.Element
import java.io.File
import java.util.EnumMap

abstract class AbstractSparkJobRunConfiguration<T>(project: Project, factory: ConfigurationFactory, name: String) :
  LocatableConfigurationBase<T>(project, factory, name) {
  //Common settings
  var sparkHome = ""

  var artifactPath: FilePath = FilePath()

  var className = ""
  var artifactArgs = ""

  var visibleBlocks: Map<RunConfigurationBlockType, Boolean> = EnumMap(RunConfigurationBlockType::class.java)

  //Dependencies
  var jars: List<FilePath> = emptyList()
  var files: List<FilePath> = emptyList()
  var pyFiles: List<FilePath> = emptyList()

  //Maven settings
  var packages = ""
  var excludePackages = ""
  var repositories = ""

  //Driver settings
  var driverMemory = ""
  var driverJavaOptions = ""
  var driverLibraryPath: List<FilePath> = emptyList()
  var driverClassPath: List<FilePath> = emptyList()
  var driverCores = ""

  //Executor settings
  var executorMemory = ""
  var totalExecutorCores = ""
  var numExecutors = ""
  var executorCores = ""
  var archives: List<FilePath> = emptyList()
  var proxyUser = ""

  //Kerberos
  var principal = ""
  var keytab: FilePath = FilePath()

  //Spark Conf
  var clusterManagerDeployModeDefault: Boolean = true
  var master = "local"
  var clusterManager = ClusterManagerType.LOCAL
  var deployMode: DeployModeType = DeployModeType.CLIENT
  var conf = ""
  var propertiesFile: FilePath = FilePath()

  //Other settings
  var supervise = false
  var verbose = false
  var queue = ""
  var isInteractive = false
  var shellExecutor = "/bin/bash"
  var envParams = ""
  var beforeShellScript = ""
  var workDirectoryPath: FilePath = FilePath()

  //Integration
  var sparkMonitoringDriverId: String = ""

  override fun writeExternal(element: Element) {
    super.writeExternal(element)
    XmlSerializer.serializeInto(this, element)
  }

  override fun readExternal(element: Element) {
    super.readExternal(element)

    try {
      XmlSerializer.deserializeInto(this, element)
    }
    catch (e: RuntimeException) {
      logger.warn(e)
    }
  }

  override fun checkConfiguration() {
    if (artifactPath.path.isEmpty())
      throw RuntimeConfigurationError(SparkMessagesBundle.message("dialog.message.specify.application"))

    validatePath(artifactPath, "Artifact Path")
    validatePath(propertiesFile, "Properties file")
    validatePath(keytab, "Keytab")
    validatePaths(files)
    validatePaths(jars)
    validatePaths(pyFiles)
    validatePath(workDirectoryPath, "Working directory")

    super.checkConfiguration()
  }

  abstract override fun getState(): T

  fun getAllPaths(): List<FilePath> {
    val allPaths = mutableListOf<FilePath>()
    allPaths += artifactPath
    allPaths += files
    allPaths += jars
    allPaths += pyFiles
    if (!keytab.isBlank()) {
      allPaths += keytab
    }
    if (!propertiesFile.isBlank()) {
      allPaths += propertiesFile
    }
    return allPaths
  }

  private fun validatePath(path: FilePath, name: String) {
    if (path.isBlank()) return

    when (path.type) {
      in setOf(FileType.FILE, FileType.UPLOAD) -> {
        val selectedFilePath = (this as? ClusterSparkJobRunConfiguration)?.selectedArtifactInfo
        if (selectedFilePath == null || selectedFilePath.filePath != path || !selectedFilePath.allowMissing) {
          if (!File(path.path).exists()) {
            throw RuntimeConfigurationException(SparkMessagesBundle.message("dialog.message.not.found", name))
          }
        }
      }
      FileType.ARTIFACT -> {
        if (ArtifactManager.getInstance(project).findArtifact(path.path) == null) {
          throw RuntimeConfigurationException(SparkMessagesBundle.message("dialog.message.not.found", name))
        }
      }
      else -> {}
    }
  }

  private fun validatePaths(paths: List<FilePath>) {
    if (paths.isEmpty()) return

    paths.forEach {
      try {
        validatePath(it, "")
      }
      catch (e: Exception) {
        throw RuntimeConfigurationException(SparkMessagesBundle.message("dialog.message.not.found", it.path))
      }
    }
  }

  private fun getAllArtifactPaths() = if (SparkSubmitSupportUtils.isArtifactSupported())
    getAllPaths().filter { it.type == FileType.ARTIFACT }
  else
    emptyList()

  fun effectiveMaster(): String {
    return clusterManager.masterFixed ?: master
  }

  fun effectiveDeployMode(): DeployModeType =
    if (this.clusterManager == ClusterManagerType.LOCAL)
      DeployModeType.CLIENT
    else
      this.deployMode

  fun debugSupported(): Boolean {
    return effectiveDeployMode() == DeployModeType.CLIENT && (type as? SparkSubmitConfigurationType)?.isPySpark == false
  }

  companion object {
    private val logger = Logger.getInstance(this::class.java)
  }
}