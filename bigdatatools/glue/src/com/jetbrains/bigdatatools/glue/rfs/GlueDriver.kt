package com.jetbrains.bigdatatools.glue.rfs

import com.intellij.bigdatatools.awsBase.ui.AwsUiUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.jetbrains.bigdatatools.common.database.BdtDatabaseUtil
import com.jetbrains.bigdatatools.common.database.BdtDbIntrospectable
import com.jetbrains.bigdatatools.common.monitoring.rfs.MonitoringDriver
import com.jetbrains.bigdatatools.common.rfs.driver.ConnectedConnectionStatus
import com.jetbrains.bigdatatools.common.rfs.driver.FileInfo
import com.jetbrains.bigdatatools.common.rfs.driver.ReadyConnectionStatus
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.driver.metainfo.DriverFileMetaInfoProvider
import com.jetbrains.bigdatatools.common.rfs.tree.node.RfsDriverTreeNodeBuilder
import com.jetbrains.bigdatatools.common.updater.BDTPluginUtil
import com.jetbrains.bigdatatools.glue.client.BdtGlueClient
import com.jetbrains.bigdatatools.glue.client.GlueDataManager
import com.jetbrains.bigdatatools.glue.introspector.GlueBdtDbIntrospector
import com.jetbrains.bigdatatools.glue.monitoring.GlueMonitoringToolWindowController
import com.jetbrains.bigdatatools.glue.rfs.metainfo.GlueMetaInfoProvider
import com.jetbrains.bigdatatools.glue.settings.GlueConnectionData
import com.jetbrains.bigdatatools.glue.settings.GlueToolWindowSettings
import com.jetbrains.bigdatatools.glue.utils.GlueUtils.database
import javax.swing.Icon
import kotlin.time.Duration

class GlueDriver(override val connectionData: GlueConnectionData,
                 project: Project?,
                 isTest: Boolean) : MonitoringDriver(project, isTest), BdtDbIntrospectable {
  override val timeout: Duration = super.timeout * 2
  override val treeNodeBuilder: RfsDriverTreeNodeBuilder = GlueTreeNodeBuilder()
  override val presentableName: String = "${connectionData.name} [${AwsUiUtil.getAuthName(connectionData)}] [${connectionData.region}]"
  override val icon: Icon = connectionData.getIcon()
  override val isFileStorage: Boolean = false

  internal val client = BdtGlueClient(project, connectionData)

  override val dataManager = GlueDataManager(project, client, connectionData, GlueToolWindowSettings.getInstance())

  override val introspector = if (!isTest)
    GlueBdtDbIntrospector(this).also {
      Disposer.register(this, it)
    }
  else
    null

  init {
    Disposer.register(this, dataManager)
  }

  override fun dispose() {
  }

  override fun innerRefreshConnection(calledByUser: Boolean): ReadyConnectionStatus {
    val status = super.innerRefreshConnection(calledByUser)

    if (status == ConnectedConnectionStatus && BDTPluginUtil.isDatabaseEnabled()) {
      BdtDatabaseUtil.refreshUIForConnection(project, connectionData.innerId)
    }

    return status
  }

  override fun getController(project: Project) = GlueMonitoringToolWindowController.getInstance(project)

  override fun getMetaInfoProvider(): DriverFileMetaInfoProvider = GlueMetaInfoProvider(this)

  override fun doLoadChildren(rfsPath: RfsPath): List<FileInfo>? = when {
    rfsPath.isRoot -> listDatabases(rfsPath)
    rfsPath.size == 1 -> listTables(rfsPath)
    rfsPath.size == 2 -> listSchema(rfsPath)
    else -> null
  }

  override fun doLoadFileInfo(rfsPath: RfsPath): FileInfo? {
    if (rfsPath.isRoot) {
      listStatus(rfsPath, force = false).resultOrThrow().fileInfos ?: return null
      return GlueFileInfo(this, rfsPath, null, null, null)
    }
    return listStatus(rfsPath.parent!!).resultOrThrow().fileInfos?.firstOrNull { it.path == rfsPath }
  }

  private fun listSchema(path: RfsPath): List<GlueFileInfo>? {
    val tableFileInfo = getFileStatus(path).resultOrThrow() as? GlueFileInfo ?: return null
    return tableFileInfo.table?.storageDescriptor()?.columns()?.map { GlueFileInfo(this, path.child(it.name(), false), null, null, it) }
  }

  private fun listDatabases(path: RfsPath): List<GlueFileInfo> = synchronized(this) {
    client.getDatabases()
  }.map {
    GlueFileInfo(this, path.child(it.name(), true), it, null, null)
  }

  private fun listTables(path: RfsPath): List<GlueFileInfo> {
    val tables = synchronized(this) {
      client.getTables(null, path.database!!, null, null)
    }
    return tables.map {
      GlueFileInfo(this, path.child(it.name(), true), null, it, null)
    }
  }

  override fun getExternalId(): String = connectionData.innerId
  override fun validatePath(path: RfsPath): String? = null
}