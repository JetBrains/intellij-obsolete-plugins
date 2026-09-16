package com.jetbrains.bigdatatools.hivemetastore.rfs

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
import com.jetbrains.bigdatatools.hivemetastore.client.HiveClient
import com.jetbrains.bigdatatools.hivemetastore.client.HiveDataManager
import com.jetbrains.bigdatatools.hivemetastore.introspector.HiveBdtDbIntrospector
import com.jetbrains.bigdatatools.hivemetastore.monitoring.HiveMonitoringToolWindowController
import com.jetbrains.bigdatatools.hivemetastore.rfs.metainfo.HiveMetaInfoProvider
import com.jetbrains.bigdatatools.hivemetastore.settings.HiveMetastoreConnectionData
import com.jetbrains.bigdatatools.hivemetastore.settings.HiveToolWindowSettings
import com.jetbrains.bigdatatools.hivemetastore.utils.HiveMetastoreUtils.catalog
import com.jetbrains.bigdatatools.hivemetastore.utils.HiveMetastoreUtils.database
import com.jetbrains.bigdatatools.hivemetastore.utils.HiveMetastoreUtils.table
import javax.swing.Icon
import kotlin.time.Duration


class HiveMetastoreDriver(override val connectionData: HiveMetastoreConnectionData,
                          project: Project?,
                          testConnection: Boolean) : MonitoringDriver(project, testConnection), BdtDbIntrospectable {
  override val timeout: Duration = super.timeout * 2
  override val treeNodeBuilder: RfsDriverTreeNodeBuilder = HiveMetastoreTreeNodeBuilder()
  override val presentableName: String = connectionData.name
  override val icon: Icon = connectionData.getIcon()
  override val isFileStorage: Boolean = false

  internal val client = HiveClient(project, connectionData, testConnection)

  override val isRfsViewEditorAvailable = true

  override val dataManager = HiveDataManager(project, client, connectionData, HiveToolWindowSettings.getInstance())

  override val introspector = if (!testConnection && BDTPluginUtil.isDatabaseEnabled()) {
    HiveBdtDbIntrospector(this).also {
      Disposer.register(this, it)
    }
  }
  else {
    null
  }

  init {
    Disposer.register(this, dataManager)
    Disposer.register(this, client)
  }

  override fun dispose() {}

  override fun innerRefreshConnection(calledByUser: Boolean): ReadyConnectionStatus {
    val status = super.innerRefreshConnection(calledByUser)

    if (status == ConnectedConnectionStatus && BDTPluginUtil.isDatabaseEnabled()) {
      BdtDatabaseUtil.refreshUIForConnection(project, connectionData.innerId)
    }

    return status
  }

  override fun getController(project: Project) = HiveMonitoringToolWindowController.getInstance(project)

  override fun getMetaInfoProvider(): DriverFileMetaInfoProvider = HiveMetaInfoProvider(this)

  override fun doLoadChildren(rfsPath: RfsPath): List<FileInfo>? = when {
    rfsPath.isRoot -> listCatalogs(rfsPath)
    rfsPath.size == 1 -> listDatabases(rfsPath)
    rfsPath.size == 2 -> listTables(rfsPath)
    rfsPath.size == 3 -> listSchema(rfsPath)
    else -> null
  }

  override fun doLoadFileInfo(rfsPath: RfsPath): FileInfo? {
    listStatus(rfsPath, force = false).resultOrThrow().fileInfos ?: return null
    return HiveFileInfo(this, rfsPath)
  }


  private fun listSchema(path: RfsPath): List<HiveFileInfo> {
    val tableSchema = client.getSchema(path.catalog, path.database, path.table) ?: emptyList()

    return tableSchema.map { fieldSchema ->
      val schemaName = "${fieldSchema.name}: ${fieldSchema.type}${if (fieldSchema.isSetComment) " (${fieldSchema.comment})" else ""}"
      HiveFileInfo(this, path.child(schemaName, false), fieldSchema)
    }
  }

  private fun listCatalogs(path: RfsPath): List<HiveFileInfo> = synchronized(this) {
    client.getCatalogs().map { HiveFileInfo(this, path.child(it, true)) }
  }


  private fun listDatabases(path: RfsPath): List<HiveFileInfo> = synchronized(this) {
    client.getDatabases(path.catalog, connectionData.databasePattern)
  }.map {
    HiveFileInfo(this, path.child(it, true))
  }

  private fun listTables(path: RfsPath): List<HiveFileInfo> {
    val tables = synchronized(this) {
      client.getTables(path.catalog, path.database ?: error("Not found"), connectionData.tablePattern)
    }
    return tables.map {
      HiveFileInfo(this, path.child(it, true))
    }
  }

  override fun getExternalId(): String = connectionData.innerId
  override fun validatePath(path: RfsPath): String? = null

  companion object {
    //Test evf to set config paths from hive lib
    const val TEST_ENV_WORKAROUND = "metastore.testing.env.workaround.dont.ever.set.this."
    internal const val THRIFT_PREFIX = "thrift://"
  }
}