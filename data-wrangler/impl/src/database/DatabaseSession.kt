package com.intellij.dataWrangler.impl.database

import com.intellij.database.Dbms
import com.intellij.database.console.session.DatabaseSessionManager
import com.intellij.database.csv.CsvFormats
import com.intellij.database.csv.CsvFormatter
import com.intellij.database.dataSource.DatabaseConnectionPoint
import com.intellij.database.dataSource.connection.DGDepartment
import com.intellij.database.datagrid.DataRequest
import com.intellij.database.extractors.BaseExtractorConfig
import com.intellij.database.extractors.DbObjectFormatter
import com.intellij.database.extractors.FormatBasedExtractor
import com.intellij.database.extractors.GridExtractorsUtilCore
import com.intellij.database.model.DasObject
import com.intellij.database.script.generator.dml.allColumns
import com.intellij.database.script.generator.dml.dmlGenerator
import com.intellij.database.util.DbImplUtil
import com.intellij.database.util.DbImplUtilCore.createBuilderForExec
import com.intellij.database.util.Out
import com.intellij.openapi.diagnostic.fileLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.io.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.Nls

private const val dbTableName: String = "__df_sql__"

data class DataWranglerLocalDatabase(val localDataSource: DatabaseConnectionPoint, val sqlSource: String, val tableName: String = dbTableName)

@Nls
private val dataBaseSessionName = "DataWrangler"
private val LOG = fileLogger()

suspend fun createSession(project: Project, localDatabase: DataWranglerLocalDatabase, processRows: suspend (data: IndexedValue<List<DataFrame>>) -> Unit, afterDataProceeded: suspend () -> Unit) {
  val facade = DatabaseSessionManager.getFacade(
    project, localDatabase.localDataSource, null, null, false, null,
    DGDepartment.RunConfigurationRunner(dataBaseSessionName)
  )

  val sqlDataConsumer = DataWranglerDatabaseConsumer()
  val flow = sqlDataConsumer.getFlow().withIndex()
  val databaseSession = DatabaseSessionManager.getSession(project, localDatabase.localDataSource, dataBaseSessionName)

  withContext(Dispatchers.Default) {
    launch {
      flow.collect { dataFrames ->
        processRows(dataFrames)
      }
      afterDataProceeded()
    }
    val client = facade.client(databaseSession)
    val sqlRequest = DataRequest.newRequest(client, localDatabase.sqlSource, 0, 0, -1, 0, 0)
    sqlRequest.promise.onProcessed {
      Disposer.dispose(client)
      Disposer.dispose(sqlDataConsumer)
    }
    client.messageBus.addConsumer(sqlDataConsumer)
    client.messageBus.addAuditor(sqlDataConsumer)
    try {
      client.messageBus.dataProducer.processRequest(sqlRequest)
      sqlRequest.promise.await()
    }
    catch (e: RuntimeException) {
      LOG.error(e)
      Disposer.dispose(client)
      Disposer.dispose(sqlDataConsumer)
    }
  }
}

fun getFormattedByteArrayWithTableData(dataFrames: List<DataFrame>, project: Project, localDatabase: DataWranglerLocalDatabase): ByteArray {
  val extractorConfig = BaseExtractorConfig(DbObjectFormatter(localDatabase.localDataSource.dataSource.dbms), project)
  val format = CsvFormatter.setFirstRowIsHeader(CsvFormats.CSV_FORMAT.getValue(), true)
  val extractor = FormatBasedExtractor(format, extractorConfig.objectFormatter)
  val rows = dataFrames.flatMap { it.rows }
  val columns = dataFrames[0].columns

  val out = Out.Readable()
  GridExtractorsUtilCore.extract(out, columns, extractor, rows)
  return out.toBytes()
}

fun generateDataWranglerQueryForRequest(dbms: Dbms, table: DasObject): String {
  val queryBuilder = allColumns(table).build(createBuilderForExec(DbImplUtil.getDatabaseDialect(dbms)))
  val helper = dmlGenerator(dbms)
  return helper.generate(queryBuilder).getStatement()
}