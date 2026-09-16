package com.intellij.bigdatatools.zeppelin.ztools.database

import com.intellij.bigdatatools.coreUi.serializer.BdtJson
import com.intellij.bigdatatools.coreUi.util.NotificationUtils
import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.bigdatatools.zeppelin.drivers.ZeppelinDriver
import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.bigdatatools.zeppelin.settings.ZeppelinConnectionData
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.bigdatatools.zeppelin.ztools.controller.ZtoolsCodeGenerator
import com.intellij.bigdatatools.zeppelin.ztools.controller.ZtoolsNoteController
import com.intellij.bigdatatools.zeppelin.ztools.controller.ZtoolsService
import com.intellij.bigdatatools.zeppelin.ztools.controller.model.ZtoolsDebugTask
import com.intellij.bigdatatools.zeppelin.ztools.database.models.ZtoolsSqlInfo
import com.intellij.bigdatatools.zeppelin.ztools.database.util.ZeppelinDatabaseUtil
import com.intellij.bigdatatools.zeppelin.ztools.database.util.ZtoolsConverter
import com.intellij.bigdatatools.zeppelin.ztools.settings.ZtoolsConfig
import com.intellij.bigdatatools.zeppelin.ztools.settings.ZtoolsSqlCollectStrategy
import com.intellij.bigdatatools.zeppelin.ztools.variableview.VariableViewManager
import com.intellij.database.actions.runRegularRefresh
import com.intellij.database.dataSource.LocalDataSource
import com.intellij.database.model.basicElement
import com.intellij.database.util.DbSqlUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.jetbrains.bigdatatools.common.database.BdtDatabaseUtil
import com.jetbrains.bigdatatools.common.database.BdtDbDatabase
import com.jetbrains.bigdatatools.common.rfs.driver.manager.DriverManager

class ZeppelinDatabaseIntegration : ZtoolsService {
  override fun initProject(project: Project) {
    if (!project.isOpen)
      return
    ZeppelinDatabaseUtil.clearObsoleteDataSourcesAndDriver(project)
  }

  override fun processInfo(project: Project,
                           file: NotebookVirtualFile,
                           config: ZeppelinConnectionData,
                           json: String,
                           interpreterCode: String,
                           zeppelinEditor: ZeppelinEditor) {
    @Suppress("HardCodedStringLiteral")
    val jsonValue = json.split("---ztools-sql---").getOrNull(1) ?: return
    val ztoolsSqlInfo = try {
      BdtJson.fromJsonToClass(jsonValue, ZtoolsSqlInfo::class.java)
    }
    catch (t: Throwable) {
      NotificationUtils.notifyException(t, ZepMessagesBundle.message("ztools.parse.error"), jsonValue)
      return
    }

    val isAppend = ztoolsSqlInfo.appendOutput

    val debugInfo = ZtoolsNoteController.getFor(file)?.debugInfo
    debugInfo?.collectedOutputs?.add(ztoolsSqlInfo)

    val driver = DriverManager.getDriverById(project, config.innerId) as? ZeppelinDriver ?: return

    val newDbs = ztoolsSqlInfo.tables.map { it.databaseName?.ifBlank { null } ?: "default" }.distinct().map { BdtDbDatabase(it) }
    val newDbToTables = ztoolsSqlInfo.tables
      .filter { it.columns.isNotEmpty() }
      .map { it.toDbTable() }
      .groupBy { it.databaseName?.ifBlank { null } ?: "default" }

    if (isAppend) {
      val dbToTables = driver.introspector?.cachedTables?.toMutableMap() ?: mutableMapOf()
      newDbToTables.forEach { (dbName, tables) ->
        dbToTables.putIfAbsent(dbName, emptyList())

        val newNames = tables.map { it.name }
        dbToTables[dbName] = ((dbToTables[dbName]?.filter { it.name !in newNames } ?: emptyList()) + tables).distinct()
      }
      driver.introspector?.cachedTables = dbToTables
      driver.introspector?.cachedDatabases = ((driver.introspector?.cachedDatabases ?: emptyList()) + newDbs).distinct()
    }
    else {
      driver.introspector?.cachedDatabases = newDbs
      driver.introspector?.cachedTables = newDbToTables
    }

    val viewManager = VariableViewManager.getInstance(project)

    val varCode = interpreterCode.removeSuffix(interpreterCode.takeLastWhile { it != '.' }) + "sql"

    val errors = ztoolsSqlInfo.errors

    viewManager.clearErrors(file, varCode)
    if (errors.isNotEmpty() && driver.connectionData.ztoolsConf.profiling) {
      viewManager.addErrors(file, varCode, errors)
    }

    val tables = ZtoolsConverter.convertToVariables(driver.introspector?.cachedTables ?: emptyMap())
    viewManager.setVariables(file, tables, varCode, false)
    BdtDatabaseUtil.getDataSource(project, config.innerId, null)?.let { refreshUI(project, it) }
  }

  override fun isInterpreterSupported(debugCellTask: ZtoolsDebugTask) =
    debugCellTask.interpreterGroup == "spark" && debugCellTask.subInterpreter in listOf("pyspark", "spark")

  override fun getServiceCellText(project: Project, language: String, params: UserDataHolderBase, ztoolsConfig: ZtoolsConfig): String? {
    if (!ztoolsConfig.sqlSettings.isEnabled)
      return null
    if (params.getUserData(ZtoolsService.FORCE_IGNORE_SQL) == true)
      return null

    val noteFile = params.getUserData(ZtoolsService.NOTE_FILE) ?: return null
    val group = params.getUserData(ZtoolsService.INTERPRETER_GROUP) ?: return null


    val forceUpdateRequired = params.getUserData(ZtoolsService.IS_ON_DEMAND) == true ||
                              VariableViewManager.getInstance(project).isEmptyVariables(noteFile, "$group.sql")

    val sqlSettings = ztoolsConfig.sqlSettings

    var showTableCommands: List<String>? = ZtoolsConverter.getShowTables(ztoolsConfig.sqlSettings)

    var noteTableNames = params.getUserData(ZtoolsService.DEFINED_TABLES)?.map {
      it.copy(database = ZtoolsConverter.prepareDatabaseName(it.database))
    }


    if (!forceUpdateRequired &&
        sqlSettings.collectionStrategy in setOf(ZtoolsSqlCollectStrategy.DEFINED_ON_EACH_RUN_ALL_REFRESH,
                                                ZtoolsSqlCollectStrategy.ONLY_ON_REFRESH)) {
      showTableCommands = null
    }

    if (!forceUpdateRequired && sqlSettings.collectionStrategy == ZtoolsSqlCollectStrategy.ONLY_ON_REFRESH) {
      noteTableNames = null
    }

    val appendOutput = !forceUpdateRequired && sqlSettings.collectionStrategy == ZtoolsSqlCollectStrategy.DEFINED_ON_EACH_RUN_ALL_REFRESH

    val res = when (language) {
      "scala" -> ZtoolsCodeGenerator.getCollectSqlSpark(noteTableNames = noteTableNames,
                                                        sqlTableCollectSqls = showTableCommands,
                                                        timeout = sqlSettings.timeout,
                                                        collectOnlyTempTables = sqlSettings.collectOnlyTempTables,
                                                        appendOutput = appendOutput)
      "python" -> ZtoolsCodeGenerator.getCollectSqlPyspark(noteTableNames = noteTableNames,
                                                           sqlTableCollectSqls = showTableCommands,
                                                           timeout = sqlSettings.timeout,
                                                           collectOnlyTempTables = sqlSettings.collectOnlyTempTables,
                                                           appendOutput = appendOutput)
      else -> null
    }
    return res
  }


  private fun refreshUI(project: Project, dataSource: LocalDataSource) {
    val dbDataSource = DbSqlUtil.getDbDataSource(project, dataSource) ?: return
    runRegularRefresh(project, dataSource, dataSource.model, listOfNotNull(dbDataSource.basicElement))
  }
}