package com.jetbrains.bigdatatools.glue.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializerUtil
import com.jetbrains.bigdatatools.common.connection.updater.IntervalUpdateSettings
import com.jetbrains.bigdatatools.common.settings.ColumnVisibilitySettings
import com.jetbrains.bigdatatools.glue.monitoring.models.GlueColumnInfo
import com.jetbrains.bigdatatools.glue.monitoring.models.GlueConfig
import com.jetbrains.bigdatatools.glue.monitoring.models.GlueDatabaseInfo
import com.jetbrains.bigdatatools.glue.monitoring.models.GluePartitionInfo
import com.jetbrains.bigdatatools.glue.monitoring.models.GlueTableInfo

@State(name = "GlueSettings", storages = [Storage("glue.xml")])
class GlueToolWindowSettings : PersistentStateComponent<GlueToolWindowSettings>, IntervalUpdateSettings {
  override var selectedConnectionId: String? = null

  private val databaseColumns = mutableListOf(
    GlueDatabaseInfo::name.name,
    GlueDatabaseInfo::catalog.name,
    GlueDatabaseInfo::description.name,
  )
  val databaseSettings = ColumnVisibilitySettings(databaseColumns)

  private val tableColumns = mutableListOf(
    GlueTableInfo::name.name,
    GlueTableInfo::createTime.name,
    GlueTableInfo::location.name,
    GlueTableInfo::tableType.name,
    GlueTableInfo::description.name,
    GlueTableInfo::owner.name,
    GlueTableInfo::lastAccessTime.name,
    GlueTableInfo::lastAnalyzedTime.name,
  )
  val tableSettings = ColumnVisibilitySettings(tableColumns)

  private val schemaColumns = mutableListOf(
    GlueColumnInfo::name.name,
    GlueColumnInfo::dataType.name,
    GlueColumnInfo::comment.name,
  )
  val schemaSettings = ColumnVisibilitySettings(schemaColumns)

  private val partitionColumns = mutableListOf(
    GluePartitionInfo::name.name,
    GluePartitionInfo::location.name,
  )
  val partitionSettings = ColumnVisibilitySettings(partitionColumns)

  override val configs: MutableMap<String, GlueConfig> = mutableMapOf()

  override var dataUpdateIntervalMillis: Int = 30000

  fun getOrCreateConfig(connectionId: String): GlueConfig {
    var config = configs[connectionId]
    if (config == null) {
      config = GlueConfig()
      configs[connectionId] = config
    }
    return config
  }

  override fun getState(): GlueToolWindowSettings = this

  override fun loadState(state: GlueToolWindowSettings) {
    XmlSerializerUtil.copyBean(state, this)
  }

  companion object {
    fun getInstance(): GlueToolWindowSettings = service()
  }
}