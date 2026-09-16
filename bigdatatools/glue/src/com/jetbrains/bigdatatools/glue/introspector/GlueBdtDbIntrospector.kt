package com.jetbrains.bigdatatools.glue.introspector

import com.intellij.bigdatatools.glue.icons.BigdatatoolsGlueIcons
import com.jetbrains.bigdatatools.common.database.BdtDbColumn
import com.jetbrains.bigdatatools.common.database.BdtDbDatabase
import com.jetbrains.bigdatatools.common.database.BdtDbTable
import com.jetbrains.bigdatatools.common.database.introspector.BdtDbIntrospectorBase
import com.jetbrains.bigdatatools.glue.rfs.GlueDriver
import com.jetbrains.bigdatatools.glue.utils.GlueMessagesBundle
import javax.swing.Icon

class GlueBdtDbIntrospector(val driver: GlueDriver) : BdtDbIntrospectorBase() {
  override val connectionData = driver.connectionData
  override val dbDriverName: String = "BDT AWS Glue Connection"
  override val dbDriverComment: String = GlueMessagesBundle.message("database.integration.comment")
  override val dbDriverIcon: Icon = BigdatatoolsGlueIcons.AwsGlue

  init {
    initDataSource(project = driver.project)
  }

  override fun dispose() {
  }

  override fun getDatabases() = driver.client.getDatabases().map { BdtDbDatabase(it.name()) }

  override fun getTables(databaseName: String) = driver.client.getTables(null, databaseName, null, null).map { table ->
    val columns = table.storageDescriptor().columns()?.map { BdtDbColumn(it.name(), it.type(), it.comment()) } ?: emptyList()
    BdtDbTable(name = table.name(),
               tableType = table.tableType(),
               databaseName = table.databaseName(),
               columns = columns,
               isTemporary = null,
               comment = table.description())
  }
}