package com.intellij.dataWrangler.jupyterPython.operations

import com.intellij.dataWrangler.annotations.ColumnIntent
import com.intellij.dataWrangler.annotations.CommandParameterName
import com.intellij.dataWrangler.annotations.DWColumnIntent
import com.intellij.dataWrangler.impl.operations.CommandFactoryBase
import com.intellij.dataWrangler.jupyterPython.DataWranglerJupyterPyBundle
import com.intellij.dataWrangler.jupyterPython.engine.PythonDataWranglerContext
import com.intellij.dataWrangler.operations.CommandFactory
import com.intellij.dataWrangler.operations.CommandFactoryGroup
import org.jetbrains.annotations.Nls

internal data class JupyterPyMinMaxParameters(
  @DWTableNumericColumn @property:DWColumnIntent(ColumnIntent.CHANGE) var column: String = "",
  @CommandParameterName("Min") var min: Int = 0, // TODO: Should be double (float)? column types required
  @CommandParameterName("Max") var max: Int = 1,
)

internal class JupyterPyMinMaxFactory : CommandFactoryBase<JupyterPyMinMaxParameters, PythonDataWranglerContext>(
  JupyterPyMinMaxParameters::class,
  DataWranglerJupyterPyBundle.messagePointer("data.wrangler.jupyter.command.minMax.name")
) {
  override fun createCommand(parameters: JupyterPyMinMaxParameters) =
    JupyterPyMinMaxCommand(this, parameters)

  override fun getGroupName() = CommandFactoryGroup.NORMALIZATION_AND_SCALING

  override fun getDescription() = DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.minMax.description.hint.text")
}

internal class JupyterPyMinMaxCommand(factory: CommandFactory<JupyterPyMinMaxParameters, PythonDataWranglerContext>, parameters: JupyterPyMinMaxParameters)
  : JupyterPyCommandBase<JupyterPyMinMaxParameters>(factory, parameters) {
  override fun getCommandLabel(): @Nls String =
    DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.minMax.label", "`${parameters.column}`")

  override fun getDescription(): @Nls String =
    DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.minMax.description", "`${parameters.column}`", parameters.min, parameters.max)

  override suspend fun generate(context: TableInfoData, generator: CodeGenerator) {
    val v = context.getTableName()
    val col = parameters.column
    generator.setCommandWrapper("""
      def min_max_scale(column, min_val, max_val):
          return (column - column.min()) / (column.max() - column.min()) * (max_val - min_val) + min_val

      $v['$col'] = min_max_scale($v['$col'], min_val=${parameters.min}, max_val=${parameters.max})
    """.trimIndent())
  }

}