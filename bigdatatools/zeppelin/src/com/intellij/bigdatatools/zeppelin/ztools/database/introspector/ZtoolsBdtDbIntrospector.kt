package com.intellij.bigdatatools.zeppelin.ztools.database.introspector

import com.intellij.bigdatatools.zeppelin.constants.ZeppelinIcons
import com.intellij.bigdatatools.zeppelin.drivers.ZeppelinDriver
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.jetbrains.bigdatatools.common.database.BdtDbDatabase
import com.jetbrains.bigdatatools.common.database.BdtDbTable
import com.jetbrains.bigdatatools.common.database.introspector.BdtDbIntrospectorBase
import javax.swing.Icon

class ZtoolsBdtDbIntrospector(val driver: ZeppelinDriver) : BdtDbIntrospectorBase() {
  override val connectionData = driver.connectionData
  override val dbDriverName: String = "BDT Zeppelin Connection"
  override val dbDriverComment: String = ZepMessagesBundle.message("database.integration.comment")
  override val dbDriverIcon: Icon = ZeppelinIcons.ZEPPELIN


  var cachedDatabases: List<BdtDbDatabase> = emptyList()
  var cachedTables: Map<String, List<BdtDbTable>> = emptyMap()


  init {
    initDataSource(project = driver.project)
  }

  override fun dispose() {

  }

  override fun getDatabases() = cachedDatabases
  override fun getTables(databaseName: String) = cachedTables[databaseName] ?: emptyList()
}