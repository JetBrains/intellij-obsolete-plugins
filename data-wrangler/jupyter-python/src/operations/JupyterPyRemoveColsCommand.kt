package com.intellij.dataWrangler.jupyterPython.operations

import com.intellij.dataWrangler.annotations.ColumnIntent
import com.intellij.dataWrangler.annotations.DWColumnIntent
import com.intellij.dataWrangler.annotations.DWTableColumn
import com.intellij.dataWrangler.impl.operations.CommandFactoryBase
import com.intellij.dataWrangler.jupyterPython.DataWranglerJupyterPyBundle
import com.intellij.dataWrangler.jupyterPython.engine.PythonDataWranglerContext
import com.intellij.dataWrangler.operations.CommandFactory
import org.jetbrains.annotations.Nls

internal data class JupyterPyRemoveColsParameters(
  @DWTableColumn @property:DWColumnIntent(ColumnIntent.REMOVE) var columns: Set<String> = emptySet(),
)

internal class JupyterPyRemoveColsFactory : CommandFactoryBase<JupyterPyRemoveColsParameters, PythonDataWranglerContext>(
  JupyterPyRemoveColsParameters::class,
  DataWranglerJupyterPyBundle.messagePointer("data.wrangler.jupyter.command.removeCols.name")
) {
  override fun createCommand(parameters: JupyterPyRemoveColsParameters) =
    JupyterPyRemoveColsCommand(this, parameters)

  override fun initDefaultParameters(context: PythonDataWranglerContext, defaultParams: JupyterPyRemoveColsParameters) {
    defaultParams.columns = context.getColumnNames().asSequence().take(1).toSet()
  }

  override fun getDescription() = DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.removeCols.description.hint.text")
}

internal class JupyterPyRemoveColsCommand(factory: CommandFactory<JupyterPyRemoveColsParameters, PythonDataWranglerContext>, parameters: JupyterPyRemoveColsParameters)
  : JupyterPyCommandBase<JupyterPyRemoveColsParameters>(factory, parameters) {
  override fun getCommandLabel(): @Nls String =
    DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.removeCols.label")

  override fun getDescription(): @Nls String =
    DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.removeCols.description", parameters.columns.joinToString(", "))

  override suspend fun generate(context: TableInfoData, generator: CodeGenerator) { // TODO when table is empty command is still created
    val v = context.getTableName()
    generator.addCommand(parameters.columns.joinToString<String>("\n") {
      "$v.pop('$it')"
    })
  }

}