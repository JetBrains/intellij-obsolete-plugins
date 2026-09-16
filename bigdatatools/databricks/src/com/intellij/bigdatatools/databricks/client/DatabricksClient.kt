package com.intellij.bigdatatools.databricks.client

import com.databricks.sdk.WorkspaceClient
import com.databricks.sdk.core.AzureCliCredentialsProvider
import com.databricks.sdk.core.DatabricksCliCredentialsProvider
import com.databricks.sdk.core.DatabricksConfig
import com.databricks.sdk.core.DatabricksException
import com.databricks.sdk.core.UserAgent
import com.databricks.sdk.service.compute.CancelCommand
import com.databricks.sdk.service.compute.Command
import com.databricks.sdk.service.compute.CommandStatusRequest
import com.databricks.sdk.service.compute.CommandStatusResponse
import com.databricks.sdk.service.compute.CreateContext
import com.databricks.sdk.service.compute.DestroyContext
import com.databricks.sdk.service.compute.Language
import com.databricks.sdk.service.compute.ListClustersRequest
import com.databricks.sdk.service.compute.RestartCluster
import com.databricks.sdk.service.files.Create
import com.databricks.sdk.service.files.Move
import com.databricks.sdk.service.iam.ObjectPermissions
import com.databricks.sdk.service.jobs.ExportRunOutput
import com.databricks.sdk.service.jobs.Run
import com.databricks.sdk.service.jobs.RunOutput
import com.databricks.sdk.service.jobs.SubmitRun
import com.databricks.sdk.service.jobs.SubmitRunResponse
import com.databricks.sdk.service.workspace.Delete
import com.databricks.sdk.service.workspace.ExportFormat
import com.databricks.sdk.service.workspace.ExportRequest
import com.databricks.sdk.service.workspace.Import
import com.databricks.sdk.service.workspace.ObjectInfo
import com.intellij.bigdatatools.databricks.auth.IntellijAzureCliCredentialsProvider
import com.intellij.bigdatatools.databricks.cli.DatabricksCliManager
import com.intellij.bigdatatools.databricks.model.ClusterInfoPresentable
import com.intellij.bigdatatools.databricks.model.DbfsFile
import com.intellij.bigdatatools.databricks.rfs.DatabricksConnectionData
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.runBlockingMaybeCancellable
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.monitoring.connection.MonitoringClient
import com.jetbrains.bigdatatools.common.updater.BDTPluginUtil
import org.jetbrains.annotations.ApiStatus
import java.io.InputStream
import java.io.OutputStream
import java.time.Duration
import java.util.Base64
import kotlin.io.path.absolutePathString

class DatabricksClient(
  project: Project?,
  val connectionData: DatabricksConnectionData,
) : MonitoringClient(project) {
  private var innerClient: WorkspaceClient? = null

  private val client: WorkspaceClient
    get() = this.innerClient ?: error("Client is not inited")

  override fun checkConnectionInner() {
    this.client.currentUser().me()
  }

  override fun dispose() {}

  override fun getRealUri(): String = connectionData.getRealUri()

  override fun connectInner(calledByUser: Boolean) {
    val config = resolveConfig(createConfig(), calledByUser)

    innerClient = WorkspaceClient(config)
    getClusters()
  }

  @ApiStatus.Internal
  fun createConfig(): DatabricksConfig = DatabricksConfig().also {
    DatabricksProxySupport.configureHttpClient(it, connectionData.getRealUri())
  }

  private fun resolveConfig(databricksConfig: DatabricksConfig, calledByUser: Boolean): DatabricksConfig {
    val config = when (connectionData.connType) {
      DatabricksConnType.AZURE -> {
        val config = databricksConfig.resolve()
        setupAzureConfig(config)
        config
      }
      DatabricksConnType.PROFILE -> {
        val config = databricksConfig.apply {
          profile = connectionData.profile
          resolve()
        }

        when (config.authType) {
          DatabricksCliCredentialsProvider.DATABRICKS_CLI -> setupDatabricksConfig(config, calledByUser)
          AzureCliCredentialsProvider.AZURE_CLI -> setupAzureConfig(config)
        }
        config
      }
      DatabricksConnType.DATABRICKS -> {
        val config = databricksConfig.resolve()
        setupDatabricksConfig(config, calledByUser)
        config
      }
    }
    return config
  }


  fun getConfig(): DatabricksConfig? = innerClient?.config()

  private fun setupAzureConfig(config: DatabricksConfig) {
    config.host = connectionData.uri
    config.authType = AzureCliCredentialsProvider.AZURE_CLI
    config.credentialsProvider = IntellijAzureCliCredentialsProvider()
  }

  private fun setupDatabricksConfig(config: DatabricksConfig, calledByUser: Boolean) {
    val cliPath = runBlockingMaybeCancellable {
      DatabricksCliManager.getOrDownloadDatabricksCli(project, calledByUser)
    }
    config.host = connectionData.uri
    config.authType = DatabricksCliCredentialsProvider.DATABRICKS_CLI
    config.databricksCliPath = cliPath.absolutePathString()
    config.credentialsProvider = IntellijDatabricksAuthProvider(calledByUser)
  }

  fun getClusterPermission(clusterId: String): ObjectPermissions {
    return client.permissions().get("clusters", clusterId)
  }

  fun getClusters(): List<ClusterInfoPresentable> {
    val request = ListClustersRequest()
    val response = this.client.clusters().list(request).toList()
    return response.map { ClusterInfoPresentable.createFrom(it) }
  }

  fun getCluster(clusterId: String): ClusterInfoPresentable? {
    if (clusterId.isBlank())
      return null
    return try {
      val details = this.innerClient?.clusters()?.get(clusterId) ?: return null
      ClusterInfoPresentable.createFrom(details)
    }
    catch (e: DatabricksException) {
      null
    }
  }

  fun startCluster(clusterId: String) {
    this.client.clusters().start(clusterId)
  }

  fun restartCluster(clusterId: String) {
    val request = RestartCluster()
    request.clusterId = clusterId
    this.client.clusters().restart(request)
  }

  fun stopCluster(clusterId: String) {
    this.client.clusters().delete(clusterId).get(Duration.ofMinutes(2))
  }


  fun getJobRunOutput(runId: Long): RunOutput {
    return this.client.jobs().getRunOutput(runId)
  }

  fun getJobRun(runId: Long): Run {
    return this.client.jobs().getRun(runId)
  }

  fun exportJobRunOutput(runId: Long): ExportRunOutput? {
    return this.client.jobs().exportRun(runId)
  }


  fun createExecutionContext(language: String, clusterId: String): String {
    val request = CreateContext()
    request.language = Language.valueOf(language.uppercase())
    request.clusterId = clusterId

    return this.client.commandExecution().create(request).get().id
  }

  fun getExecutionContextStatus(clusterId: String, contextId: String, commandId: String): CommandStatusResponse {
    val request = CommandStatusRequest()
    request.clusterId = clusterId
    request.contextId = contextId
    request.commandId = commandId

    return this.client.commandExecution().commandStatus(request)
  }

  fun destroyExecutionContextStatus(contextId: String, clusterId: String) {
    val request = DestroyContext()
    request.contextId = contextId
    request.clusterId = clusterId
    this.client.commandExecution().destroy(request)
  }

  fun executeCommand(clusterId: String, contextId: String, language: String, command: String): String {
    val request = Command()
    request.contextId = contextId
    request.clusterId = clusterId
    request.language = Language.valueOf(language.uppercase())
    request.command = command
    return this.client.commandExecution().execute(request).response.id
  }

  fun cancelCommand(clusterId: String, contextId: String, commandId: String) {
    val request = CancelCommand()
    request.contextId = contextId
    request.clusterId = clusterId
    request.commandId = commandId
    this.client.commandExecution().cancel(request)
  }


  fun submitJob(runParameters: SubmitRun): SubmitRunResponse {
    return this.client.jobs().submit(runParameters).response
  }

  fun workspaceList(path: String): List<ObjectInfo>? {
    return this.client.workspace().list(path)?.toList()
  }

  fun workspaceFileInfo(path: String): ObjectInfo? {
    return this.client.workspace().getStatus(path)
  }

  fun workspaceImport(import: Import) {
    return this.client.workspace().importContent(import)
  }

  fun workspaceExport(stringPath: String, dbExportFormat: ExportFormat): String {
    val request = ExportRequest().setPath(stringPath).setFormat(dbExportFormat)
    val export = this.client.workspace().export(request)
    val base64 = export.content
    return Base64.getDecoder().decode(base64).decodeToString()
  }

  fun workspaceDelete(path: String, recursive: Boolean = true) {
    val request = Delete().setPath(path).setRecursive(recursive)
    this.client.workspace().delete(request)
  }

  fun workspaceMakeDirs(path: String) {
    this.client.workspace().mkdirs(path)
  }

  fun dbfsList(path: String): List<DbfsFile> {
    return this.client.dbfs().list(path).map {
      DbfsFile(path = it.path, isDir = it.isDir, fileSize = it.fileSize, modificationTime = it.modificationTime)
    }
  }

  fun dbfsGetStatus(path: String): DbfsFile {
    val fileInfo = this.client.dbfs().getStatus(path)
    return DbfsFile(path = fileInfo.path,
                    isDir = fileInfo.isDir,
                    fileSize = fileInfo.fileSize,
                    modificationTime = fileInfo.modificationTime)


  }

  fun dbfsDelete(path: String, recursive: Boolean) {
    val request = com.databricks.sdk.service.files.Delete().setPath(path).setRecursive(recursive)
    this.client.dbfs().delete(request)
  }

  fun dbfsMakeDirs(path: String) {
    this.client.dbfs().mkdirs(path)
  }

  fun dbfsMove(sourcePath: String, destPath: String) {
    val request = Move()
    request.sourcePath = sourcePath
    request.destinationPath = destPath
    this.client.dbfs().move(request)
  }

  fun dbfsInputStream(path: String): InputStream {
    return this.client.dbfs().open(path)
  }


  fun dbfsOutputStream(path: String, create: Boolean): OutputStream {
    if (create) {
      val request = Create()
      request.path = path
      this.client.dbfs().create(request)
    }
    return this.client.dbfs().getOutputStream(path)
  }

  fun getUser(): String? {
    ApplicationManager.getApplication().isUnitTestMode
    val me = this.client.currentUser().me()
    return me.emails.firstOrNull()?.value
  }

  companion object {
    init {
      UserAgent.withProduct("databricks-intellij", BDTPluginUtil.getDatabricksVersion())
    }
  }
}
