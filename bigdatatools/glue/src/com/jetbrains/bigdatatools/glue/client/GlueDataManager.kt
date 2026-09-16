package com.jetbrains.bigdatatools.glue.client

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.jetbrains.bigdatatools.common.monitoring.data.MonitoringDataManager
import com.jetbrains.bigdatatools.common.monitoring.data.model.FieldGroupsData
import com.jetbrains.bigdatatools.common.monitoring.data.model.FieldsGroupModel
import com.jetbrains.bigdatatools.common.monitoring.data.model.ObjectDataModel
import com.jetbrains.bigdatatools.common.monitoring.data.storage.FieldGroupsDataModelStorage
import com.jetbrains.bigdatatools.common.monitoring.data.storage.ObjectDataModelStorage
import com.jetbrains.bigdatatools.common.monitoring.data.storage.RootDataModelStorage
import com.jetbrains.bigdatatools.common.rfs.driver.manager.DriverManager
import com.jetbrains.bigdatatools.glue.monitoring.models.DatabaseId
import com.jetbrains.bigdatatools.glue.monitoring.models.GlueColumnInfo
import com.jetbrains.bigdatatools.glue.monitoring.models.GlueDatabaseInfo
import com.jetbrains.bigdatatools.glue.monitoring.models.GluePartitionInfo
import com.jetbrains.bigdatatools.glue.monitoring.models.GlueResourceShareType
import com.jetbrains.bigdatatools.glue.monitoring.models.GlueTableInfo
import com.jetbrains.bigdatatools.glue.monitoring.models.TableId
import com.jetbrains.bigdatatools.glue.rfs.GlueDriver
import com.jetbrains.bigdatatools.glue.settings.GlueConnectionData
import com.jetbrains.bigdatatools.glue.settings.GlueToolWindowSettings
import com.jetbrains.bigdatatools.glue.utils.GlueTransforms
import software.amazon.awssdk.services.glue.model.Table

class GlueDataManager(project: Project?,
                      override val client: BdtGlueClient,
                      override val connectionData: GlueConnectionData,
                      settings: GlueToolWindowSettings) : MonitoringDataManager(project, settings) {
  val connectionId = client.connData.innerId

  val rootModel = createDatabaseDataModel().also { Disposer.register(this, it) }
  private var databaseModels = createDatabaseStorage(settings).also { Disposer.register(this, it) }
  private var schemaModels = createSchemaStorage().also { Disposer.register(this, it) }
  private var partitionsModels = createPartitionStorage(settings).also { Disposer.register(this, it) }
  private var tableSummaryModels = createTableSummaries().also { Disposer.register(this, it) }

  init {
    init()

    RootDataModelStorage(updater, listOf(rootModel)).also { Disposer.register(this, it) }
  }

  private fun createDatabaseDataModel(): ObjectDataModel<GlueDatabaseInfo> {
    val dataModel = ObjectDataModel(GlueDatabaseInfo::name) {
      val settings = GlueToolWindowSettings.getInstance()
      val config = settings.getOrCreateConfig(connectionId)

      val databases = client.getDatabases(resourceShareType = GlueResourceShareType.fromId(config.databaseResourceShareType))

      databases.map {
        GlueDatabaseInfo(it)
      } to false
    }

    Disposer.register(this, dataModel)
    return dataModel
  }

  fun getPartitionsModel(id: TableId) = partitionsModels.get(id)
  fun getSchemaModel(tableId: TableId) = schemaModels.get(tableId)
  fun getTablesModel(id: DatabaseId) = databaseModels.get(id)

  fun getTableSummary(tableId: TableId): FieldsGroupModel<Table> = tableSummaryModels[tableId]

  fun getTable(catalog: String, database: String, table: String): Table? {
    val model = getTablesModel(DatabaseId(catalog, database))
    return model.data?.firstOrNull { it.name == table && it.database == database && it.catalog == catalog }?.table
  }

  private fun createDatabaseStorage(settings: GlueToolWindowSettings) = ObjectDataModelStorage<DatabaseId, GlueTableInfo>(
    updater,
    GlueTableInfo::name) { id ->
    val config = settings.getOrCreateConfig(connectionId)


    val awsTables = client.getTables(id.catalog, id.database, config.tablePattern?.ifBlank { null }, config.tableLimit)

    awsTables.map { table ->
      GlueTableInfo(table)
    }
  }

  private fun createSchemaStorage() = ObjectDataModelStorage<TableId, GlueColumnInfo>(updater, GlueColumnInfo::name) { id ->
    val sourceModel = getTablesModel(DatabaseId(id.catalog, id.database))
    val tables = sourceModel.entries
    tables.firstOrNull()?.table?.storageDescriptor()?.columns()?.map { GlueColumnInfo(it) }
    ?: emptyList()
  }

  private fun createPartitionStorage(settings: GlueToolWindowSettings) =
    ObjectDataModelStorage<TableId, GluePartitionInfo>(updater, GluePartitionInfo::name) { id ->
      val config = settings.getOrCreateConfig(connectionId)

      val table = getTable(id.catalog, id.database, id.table) ?: let {
        return@ObjectDataModelStorage listOf()
      }

      if (table.partitionKeys().isEmpty()) {
        return@ObjectDataModelStorage listOf()
      }

      val partitions = client.getSchemaPartitions(id.catalog, id.database, id.table, config.partitionLimit)


      val partitionInfos = partitions.map {
        GluePartitionInfo(it, table.partitionKeys() ?: emptyList())
      }
      partitionInfos
    }

  private fun createTableSummaries() = FieldGroupsDataModelStorage<TableId, Table>(updater) { tableId ->
    val sourceModel = getTablesModel(DatabaseId(tableId.catalog, tableId.database))
    val source = sourceModel.data ?: emptyList()
    val tableInfo = source.firstOrNull { it.name == tableId.table && it.database == tableId.database && it.catalog == tableId.catalog }
                    ?: return@FieldGroupsDataModelStorage FieldGroupsData.empty<Table>()
    GlueTransforms.getTableLocalizedFields(tableInfo.table)
  }

  companion object {
    fun getInstance(connectionId: String, project: Project) =
      (DriverManager.getDriverById(project, connectionId) as? GlueDriver)?.dataManager

    const val DELIMITER = "$!@#"
  }
}

