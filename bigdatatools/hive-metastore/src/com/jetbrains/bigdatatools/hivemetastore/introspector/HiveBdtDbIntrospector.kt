package com.jetbrains.bigdatatools.hivemetastore.introspector

import com.intellij.bigdatatools.hiveMetastore.icons.BigdatatoolsHiveMetastoreIcons
import com.jetbrains.bigdatatools.common.database.BdtDbColumn
import com.jetbrains.bigdatatools.common.database.BdtDbDatabase
import com.jetbrains.bigdatatools.common.database.BdtDbTable
import com.jetbrains.bigdatatools.common.database.introspector.BdtDbIntrospectorBase
import com.jetbrains.bigdatatools.hivemetastore.rfs.HiveMetastoreDriver
import com.jetbrains.bigdatatools.hivemetastore.utils.HiveMessagesBundle
import javax.swing.Icon

class HiveBdtDbIntrospector(val driver: HiveMetastoreDriver) : BdtDbIntrospectorBase() {
  override val connectionData = driver.connectionData
  override val dbDriverName: String = "BDT Hive Metastore Connection"
  override val dbDriverComment: String = HiveMessagesBundle.message("database.integration.comment")
  override val dbDriverIcon: Icon = BigdatatoolsHiveMetastoreIcons.Apache_hive

  init {
    initDataSource(project = driver.project)
  }

  override fun dispose() {

  }

  override fun getDatabases() = driver.client.getDatabases(null, null).map { BdtDbDatabase(it) }

  override fun getTables(databaseName: String) = driver.client.getTables(null, databaseName, null, null).mapNotNull { tableName ->
    val table = driver.client.getTable(null, databaseName, tableName) ?: return@mapNotNull null
    val columns = driver.client.getSchema(null, databaseName, tableName) ?: emptyList()
    val bdtColumns = columns.map { BdtDbColumn(it.name, it.type, it.comment) }
    BdtDbTable(name = table.tableName, tableType = table.tableType, databaseName = table.dbName, bdtColumns, table.isTemporary, null)
  }
}