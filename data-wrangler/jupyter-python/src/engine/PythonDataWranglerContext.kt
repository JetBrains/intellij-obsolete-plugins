package com.intellij.dataWrangler.jupyterPython.engine

import com.intellij.dataWrangler.executor.DataWranglerContext
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.scientific.tables.api.DSDataFrameInfo

enum class PandasDataFrameType(val type: String) {
  FLOAT("float64"),
  INT("int64"),
  STRING("object"),
  DATE("datetime64[ns]")
}

data class ColumnInfo(val name: String, val type: PandasDataFrameType?)

const val DW_VARIABLE_NAME: String = "df_dw"

interface PythonDataWranglerContext : DataWranglerContext {

  fun getVariableCodePreviewName(): String = DW_VARIABLE_NAME

  fun getInitializationCode(): String = ""

  suspend fun executeCommand(commandCode: String)

  override fun getColumnNames(): List<@NlsSafe String> = getDSDataFrameInfo().columnNames.drop(1)

  /**
   * Returns a list of column types in [PandasDataFrameType]. Should be return the same size as [getColumnNames], but can't guarantee.
   */
  fun getColumnTypes(): List<PandasDataFrameType?> = getDSDataFrameInfo().columnTypes.drop(1).map { type -> PandasDataFrameType.entries.firstOrNull { it.type == type } }

  fun getColumns(): List<ColumnInfo> = getColumnNames().zip(getColumnTypes()).map { ColumnInfo(it.first, it.second) }

  override fun getFile(): VirtualFile? = null

  fun getDSDataFrameInfo(): DSDataFrameInfo

  suspend fun wrapPyCode(commandCode: String): String {
    val execFun = "dw_exec_${System.currentTimeMillis()}"

    val resultCode = "def $execFun(df):\n" +
                     "  ${commandCode.replace("\n", "\n  ")}\n" +
                     "  return df\n" +
                     "${getTableName()} = $execFun(${getTableName()})"

    return resultCode
  }

  suspend fun getCodeDeletePyVariable(): String {
    return """
      if '${getTableName()}' in globals():
        del ${getTableName()}
    """.trimIndent()
  }
}