package com.intellij.dataWrangler.jupyterPython.operations

import com.intellij.dataWrangler.annotations.ColumnIntent
import com.intellij.dataWrangler.annotations.CommandParameterName
import com.intellij.dataWrangler.annotations.DWColumnIntent
import com.intellij.dataWrangler.annotations.DWTableColumn
import com.intellij.dataWrangler.impl.operations.CommandFactoryBase
import com.intellij.dataWrangler.jupyterPython.DataWranglerJupyterPyBundle
import com.intellij.dataWrangler.jupyterPython.engine.PandasDataFrameType
import com.intellij.dataWrangler.jupyterPython.engine.PythonDataWranglerContext
import com.intellij.dataWrangler.operations.CommandFactory
import org.jetbrains.annotations.Nls

internal data class JupyterPyChangeColumnTypeParams(
  @DWTableColumn @property:DWColumnIntent(ColumnIntent.CHANGE) var column: String = "",
  @CommandParameterName("New type") var newType: PandasDataFrameType = PandasDataFrameType.STRING,
)

internal class JupyterPyChangeColumnTypeFactory : CommandFactoryBase<JupyterPyChangeColumnTypeParams, PythonDataWranglerContext>(
  JupyterPyChangeColumnTypeParams::class,
  DataWranglerJupyterPyBundle.messagePointer("data.wrangler.jupyter.command.changeType.name")
) {
  override fun createCommand(parameters: JupyterPyChangeColumnTypeParams) =
    JupyterPyChangeColumnTypeCommand(this, parameters)

  override fun getDescription() = DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.changeType.description.hint.text")
}

// TODO string->int does not work (should?)
internal class JupyterPyChangeColumnTypeCommand(factory: CommandFactory<JupyterPyChangeColumnTypeParams, PythonDataWranglerContext>, parameters: JupyterPyChangeColumnTypeParams)
  : JupyterPyCommandBase<JupyterPyChangeColumnTypeParams>(factory, parameters) {
  override fun getCommandLabel(): @Nls String =
    DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.changeType.label", "`${parameters.column}`")

  override fun getDescription(): @Nls String =
    DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.changeType.description", "`${parameters.column}`", parameters.newType.toString())

  override suspend fun generate(context: TableInfoData, generator: CodeGenerator) {
    val v = context.getTableName()
    val col = parameters.column
    val type = parameters.newType.type
    generator.addCommand("""
      $v['$col'] = $v['$col'].astype("$type")
    """.trimIndent())
  }

}
