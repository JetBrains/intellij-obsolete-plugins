package com.jetbrains.bigdatatools.hivemetastore.client

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.jetbrains.bigdatatools.common.monitoring.data.MonitoringDataManager
import com.jetbrains.bigdatatools.common.monitoring.data.model.ObjectDataModel
import com.jetbrains.bigdatatools.common.monitoring.data.storage.FieldGroupsDataModelStorage
import com.jetbrains.bigdatatools.common.monitoring.data.storage.ObjectDataModelStorage
import com.jetbrains.bigdatatools.common.monitoring.data.storage.RootDataModelStorage
import com.jetbrains.bigdatatools.common.rfs.driver.manager.DriverManager
import com.jetbrains.bigdatatools.common.util.TimeUtils
import com.jetbrains.bigdatatools.hivemetastore.monitoring.models.DatabaseId
import com.jetbrains.bigdatatools.hivemetastore.monitoring.models.HiveDatabaseInfo
import com.jetbrains.bigdatatools.hivemetastore.monitoring.models.HivePartitionInfo
import com.jetbrains.bigdatatools.hivemetastore.monitoring.models.HiveSchemaInfo
import com.jetbrains.bigdatatools.hivemetastore.monitoring.models.HiveTableInfo
import com.jetbrains.bigdatatools.hivemetastore.monitoring.models.TableId
import com.jetbrains.bigdatatools.hivemetastore.rfs.HiveMetastoreDriver
import com.jetbrains.bigdatatools.hivemetastore.settings.HiveMetastoreConnectionData
import com.jetbrains.bigdatatools.hivemetastore.settings.HiveToolWindowSettings
import com.jetbrains.bigdatatools.hivemetastore.utils.HiveTransforms
import org.apache.hadoop.hive.metastore.TableType
import org.apache.hadoop.hive.metastore.Warehouse
import org.apache.hadoop.hive.metastore.api.Table

class HiveDataManager(project: Project?,
                      override val client: HiveClient,
                      override val connectionData: HiveMetastoreConnectionData,
                      override val settings: HiveToolWindowSettings) : MonitoringDataManager(project, settings) {
  val connectionId = client.connectionData.innerId

  val rootModel = createDatabaseDataModel().also { Disposer.register(this, it) }
  private val databaseModels = createDatabases().also { Disposer.register(this, it) }
  private val schemaModels = createSchemas().also { Disposer.register(this, it) }
  private val partitionsModels = createPartitions().also { Disposer.register(this, it) }
  private val tableSummaryModels = createTableSummaries().also { Disposer.register(this, it) }

  init {
    init()

    RootDataModelStorage(updater, listOf(rootModel)).also { Disposer.register(this, it) }
  }

  fun getPartitionsModel(id: TableId) = partitionsModels.get(id)
  fun getSchemaModel(id: TableId) = schemaModels.get(id)
  fun getDatabaseModel(id: DatabaseId) = databaseModels.get(id)
  fun getTableSummary(tableId: TableId) = tableSummaryModels[tableId]

  fun getTable(catalog: String, database: String, table: String): Table? {
    val model = getDatabaseModel(DatabaseId(catalog, database))
    return model.data?.firstOrNull { it.name == table && it.database == database && it.catalog == catalog }?.origin
  }

  private fun createDatabases() = ObjectDataModelStorage<DatabaseId, HiveTableInfo>(updater, HiveTableInfo::name) { id ->
    val config = settings.getOrCreateConfig(connectionId)

    val realTableTypes = TableType.entries.map { it.name }

    var tableTypes: List<TableType>? = settings.filterTableTypes.filter { it in realTableTypes }.map { TableType.valueOf(it) }.ifEmpty {
      return@ObjectDataModelStorage emptyList()
    }
    if (tableTypes?.size == realTableTypes.size)
      tableTypes = null

    val tableNames = tableTypes?.flatMap { client.getTables(id.catalog, id.database, config.tablePattern, it) }
                     ?: client.getTables(id.catalog, id.database, config.tablePattern, null)

    val tables = tableNames.mapNotNull { tableName ->
      val table = client.getTable(id.catalog, id.database, tableName) ?: return@mapNotNull null
      val location = table.sd?.location ?: ""
      val tableType = table.tableType ?: ""
      val description = table.sd?.serdeInfo?.description ?: ""
      val createTime = table.createTime.let { TimeUtils.unixTimeToString(it.toLong() * 1000) }
      HiveTableInfo(origin = table,
                    catalog = table.catName ?: Warehouse.DEFAULT_CATALOG_NAME,
                    database = table.dbName,
                    name = table.tableName ?: "",
                    location = location,
                    tableType = tableType,
                    createTime = createTime,
                    description = description)
    }
    tables

  }

  private fun createSchemas() = ObjectDataModelStorage<TableId, HiveSchemaInfo>(updater, HiveSchemaInfo::name) { tableId ->
    val schemaParts = client.getSchema(tableId.catalog, tableId.database, tableId.table) ?: emptyList()
    val schemaInfos = schemaParts.map {
      HiveSchemaInfo(it.name ?: "", it.type ?: "", it.comment ?: "")
    }
    schemaInfos
  }

  private fun createPartitions() = ObjectDataModelStorage<TableId, HivePartitionInfo>(updater, HivePartitionInfo::name) { id ->
    val config = settings.getOrCreateConfig(connectionId)

    val partitionsNames = client.getPartitionsNames(id.catalog, id.database, id.table, config.partitionLimit)
    val partitions = client.getPartitions(id.catalog, id.database, id.table, config.partitionLimit)

    val partitionInfos = partitionsNames.zip(partitions).map {
      HivePartitionInfo(it.first, it.second.sd.location, it.second)
    }
    partitionInfos
  }

  private fun createTableSummaries() = FieldGroupsDataModelStorage<TableId, HiveTableInfo>(updater) { tableId ->
    val sourceModel = getDatabaseModel(DatabaseId(tableId.catalog, tableId.database))
    val source = sourceModel.data ?: emptyList()
    val tableInfo = source.firstOrNull {
      it.name == tableId.table &&
      it.database == tableId.database &&
      it.catalog == tableId.catalog
    } ?: error("Cannot find table")
    HiveTransforms.getTableLocalizedFields(tableInfo)
  }

  private fun createDatabaseDataModel(): ObjectDataModel<HiveDatabaseInfo> {
    val dataModel = ObjectDataModel(HiveDatabaseInfo::name) {
      val settings = HiveToolWindowSettings.getInstance()
      val config = settings.getOrCreateConfig(connectionId)

      val catalogs = client.getCatalogs()
      val databases = catalogs.flatMap {
        client.getDatabases(it, config.databasePattern)
      }

      val databaseInfos = databases.mapNotNull {
        val database = client.getDatabaseInfo(it) ?: return@mapNotNull null
        HiveDatabaseInfo(database)
      }
      databaseInfos to false
    }

    Disposer.register(this, dataModel)

    return dataModel
  }


  companion object {
    fun getInstance(connectionId: String, project: Project) =
      (DriverManager.getDriverById(project, connectionId) as? HiveMetastoreDriver)?.dataManager

    const val DELIMITER = "$!@#"
  }
}