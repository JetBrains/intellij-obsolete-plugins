package com.jetbrains.bigdatatools.hivemetastore.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializerUtil
import com.jetbrains.bigdatatools.common.connection.updater.IntervalUpdateSettings
import com.jetbrains.bigdatatools.common.settings.ColumnVisibilitySettings
import com.jetbrains.bigdatatools.hivemetastore.monitoring.models.HiveDatabaseInfo
import com.jetbrains.bigdatatools.hivemetastore.monitoring.models.HiveMetastoreConfig
import com.jetbrains.bigdatatools.hivemetastore.monitoring.models.HivePartitionInfo
import com.jetbrains.bigdatatools.hivemetastore.monitoring.models.HiveSchemaInfo
import com.jetbrains.bigdatatools.hivemetastore.monitoring.models.HiveTableInfo
import org.apache.hadoop.hive.metastore.TableType

@State(name = "HiveSettings", storages = [Storage("hive.xml")])
class HiveToolWindowSettings : PersistentStateComponent<HiveToolWindowSettings>, IntervalUpdateSettings {
  override var selectedConnectionId: String? = null

  var filterTableTypes = TableType.entries.map { it.name }.toMutableSet()

  private val databaseColumns = mutableListOf(
    HiveDatabaseInfo::name.name,
    HiveDatabaseInfo::catalog.name,
    HiveDatabaseInfo::description.name,
  )
  val databaseSettings = ColumnVisibilitySettings(databaseColumns)

  private val tableColumns = mutableListOf(
    HiveTableInfo::name.name,
    HiveTableInfo::createTime.name,
    HiveTableInfo::location.name,
    HiveTableInfo::tableType.name,
    HiveTableInfo::description.name,
  )
  val tableSettings = ColumnVisibilitySettings(tableColumns)

  private val schemaColumns = mutableListOf(
    HiveSchemaInfo::name.name,
    HiveSchemaInfo::dataType.name,
    HiveSchemaInfo::comment.name,
  )
  val schemaSettings = ColumnVisibilitySettings(schemaColumns)

  private val partitionColumns = mutableListOf(
    HivePartitionInfo::name.name,
    HivePartitionInfo::location.name,
  )
  val partitionSettings = ColumnVisibilitySettings(partitionColumns)

  override val configs: MutableMap<String, HiveMetastoreConfig> = mutableMapOf()

  override var dataUpdateIntervalMillis: Int = 30000

  fun getOrCreateConfig(connectionId: String): HiveMetastoreConfig {
    var config = configs[connectionId]
    if (config == null) {
      config = HiveMetastoreConfig()
      configs[connectionId] = config
    }
    return config
  }

  override fun getState(): HiveToolWindowSettings = this

  override fun loadState(state: HiveToolWindowSettings) {
    XmlSerializerUtil.copyBean(state, this)
  }

  companion object {
    fun getInstance(): HiveToolWindowSettings = service()
  }
}