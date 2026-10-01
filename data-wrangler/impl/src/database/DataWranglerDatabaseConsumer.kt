package com.intellij.dataWrangler.impl.database

import com.intellij.database.datagrid.DataAuditor
import com.intellij.database.datagrid.DataConsumer
import com.intellij.database.datagrid.GridColumn
import com.intellij.database.datagrid.GridDataRequest
import com.intellij.database.datagrid.GridRow
import com.intellij.openapi.Disposable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flow

data class DataFrame(val columns: List<GridColumn>, val rows: List<GridRow>)

private const val rowsNumber = 100

class DataWranglerDatabaseConsumer : DataConsumer, DataAuditor, Disposable {

  private val columnsResult: MutableList<GridColumn> = ArrayList()

  private val channel = Channel<DataFrame>()

  fun getFlow(size: Int = rowsNumber): Flow<List<DataFrame>> = channel.consumeAsFlow().chunkedFlow(size)

  override fun setColumns(
    context: GridDataRequest.Context,
    subQueryIndex: Int,
    resultSetIndex: Int,
    columns: Array<out GridColumn>,
    firstRowNum: Int,
  ) {
    columnsResult.addAll(columns)
  }

  override fun addRows(context: GridDataRequest.Context, rows: List<GridRow>) {
    channel.trySend(DataFrame(columnsResult, rows))
  }

  override fun afterLastRowAdded(context: GridDataRequest.Context, total: Int) {
    channel.close()
  }

  override fun dispose() {
    channel.close()
  }
}

// TODO remove after platform change kotlin coroutines version
private fun <T> Flow<T>.chunkedFlow(size: Int): Flow<List<T>> {
  require(size >= 1) { "Expected positive chunk size, but got $size" }
  return flow {
    var result: ArrayList<T>? = null
    collect { value ->
      val acc = result ?: ArrayList<T>(size).also { result = it }
      acc.add(value)
      if (acc.size == size) {
        emit(acc)
        result = null
      }
    }
    result?.let { emit(it) }
  }
}