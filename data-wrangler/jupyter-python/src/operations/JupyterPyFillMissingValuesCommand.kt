package com.intellij.dataWrangler.jupyterPython.operations

import com.intellij.dataWrangler.annotations.ColumnIntent
import com.intellij.dataWrangler.annotations.CommandParameterName
import com.intellij.dataWrangler.annotations.DWColumnIntent
import com.intellij.dataWrangler.annotations.DWTableColumn
import com.intellij.dataWrangler.impl.operations.CommandFactoryBase
import com.intellij.dataWrangler.jupyterPython.DataWranglerJupyterPyBundle
import com.intellij.dataWrangler.jupyterPython.engine.PythonDataWranglerContext
import com.intellij.dataWrangler.operations.CommandFactory
import org.jetbrains.annotations.Nls

internal data class JupyterPyFillMissingValuesParameters(
  @DWTableColumn @property:DWColumnIntent(ColumnIntent.CHANGE) var column: String = "",
  @CommandParameterName("Value") var missingValueReplacement: String = "",
  @CommandParameterName("Empty string is a missing value") var emptyStringIsMissingValue: Boolean = false,
)

internal class JupyterPyFillMissingValuesFactory : CommandFactoryBase<JupyterPyFillMissingValuesParameters, PythonDataWranglerContext>(
  JupyterPyFillMissingValuesParameters::class,
  DataWranglerJupyterPyBundle.messagePointer("data.wrangler.jupyter.command.fillMissing.name")
) {
  override fun createCommand(parameters: JupyterPyFillMissingValuesParameters) = JupyterPyFillMissingValuesCommand(this, parameters)

  override fun getDescription() = DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.fillMissing.description.hint.text")
}

internal class JupyterPyFillMissingValuesCommand(factory: CommandFactory<JupyterPyFillMissingValuesParameters, PythonDataWranglerContext>, parameters: JupyterPyFillMissingValuesParameters)
  : JupyterPyCommandBase<JupyterPyFillMissingValuesParameters>(factory, parameters) {
  override fun getCommandLabel(): @Nls String =
    DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.fillMissing.label", "`${parameters.column}`")

  override fun getDescription(): @Nls String =
    DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.fillMissing.description", "`${parameters.column}`", "`${parameters.missingValueReplacement}`")

  override suspend fun generate(context: TableInfoData, generator: CodeGenerator) { // TODO handle empty missingValueReplacement?
    val v = context.getTableName()
    val col = parameters.column
    val prefix = if (parameters.emptyStringIsMissingValue)
      "${v}['$col'].replace('', None, inplace=True)\n"
    else ""
    val replacement = parameters.missingValueReplacement
    generator.addCommand("$prefix${v} = $v.fillna({'$col': '$replacement'})")
  }
}