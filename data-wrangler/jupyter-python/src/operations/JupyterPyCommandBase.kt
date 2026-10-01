package com.intellij.dataWrangler.jupyterPython.operations

import com.intellij.dataWrangler.impl.operations.DataWranglerCommandBase
import com.intellij.dataWrangler.jupyterPython.engine.PandasDataFrameType
import com.intellij.dataWrangler.jupyterPython.engine.PythonDataWranglerContext
import com.intellij.dataWrangler.operations.CommandFactory
import com.intellij.scientific.tables.utils.launchIO

abstract class JupyterPyCommandBase<P : Any>(factory: CommandFactory<P, PythonDataWranglerContext>, parameters: P)
  : DataWranglerCommandBase<P, PythonDataWranglerContext>(factory, parameters) {
  interface CodeGenerator {
    fun addImport(importCode: String)
    fun addCommand(commandCode: String)
    fun setCommandWrapper(codeWrapper: String)
  }

  interface TableInfoData {
    fun getTableName(): String
    fun getColumnsType(columnName: String): PandasDataFrameType?
  }

  final override suspend fun execute(context: PythonDataWranglerContext) {
    val builder = StringBuilder()
    generate(context.asContextData(), object : CodeGenerator {
      override fun addImport(importCode: String) {
        addCommand(importCode)
        addCommand("\n")
      }

      override fun addCommand(commandCode: String) {
        builder.append(commandCode)
      }

      override fun setCommandWrapper(codeWrapper: String) {
        launchIO {
          addCommand(context.wrapPyCode(codeWrapper))
        }
      }
    })
    if (builder.isNotEmpty()) {
      context.executeCommand(builder.toString())
    }
  }

  abstract suspend fun generate(context: TableInfoData, generator: CodeGenerator)
}

fun PythonDataWranglerContext.asContextData(): JupyterPyCommandBase.TableInfoData =
  object : JupyterPyCommandBase.TableInfoData {
    override fun getTableName(): String = this@asContextData.getTableName()
    override fun getColumnsType(columnName: String): PandasDataFrameType? = this@asContextData.getColumns().firstOrNull { it.name == columnName }?.type
  }