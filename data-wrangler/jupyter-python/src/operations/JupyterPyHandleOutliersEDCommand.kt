package com.intellij.dataWrangler.jupyterPython.operations

import com.intellij.dataWrangler.annotations.ColumnIntent
import com.intellij.dataWrangler.annotations.CommandParameterName
import com.intellij.dataWrangler.annotations.DWColumnIntent
import com.intellij.dataWrangler.impl.operations.CommandFactoryBase
import com.intellij.dataWrangler.jupyterPython.DataWranglerJupyterPyBundle
import com.intellij.dataWrangler.jupyterPython.engine.PythonDataWranglerContext
import com.intellij.dataWrangler.operations.CommandFactory
import com.intellij.dataWrangler.operations.CommandFactoryGroup
import com.intellij.dataWrangler.operations.DataWranglerCommand
import org.jetbrains.annotations.Nls

internal data class JupyterPyHandleOutliersEDParams(
  @DWTableNumericColumn @property:DWColumnIntent(ColumnIntent.REFERENCE) var column: String = "",
  @CommandParameterName("Threshold") var threshold: Float = 1.5f,
)

internal class JupyterPyHandleOutliersEDFactory : CommandFactoryBase<JupyterPyHandleOutliersEDParams, PythonDataWranglerContext>(
  JupyterPyHandleOutliersEDParams::class,
  DataWranglerJupyterPyBundle.messagePointer("data.wrangler.jupyter.command.handleOutliersWithED.name")
) {
  override fun createCommand(parameters: JupyterPyHandleOutliersEDParams): DataWranglerCommand<PythonDataWranglerContext> =
    JupyterPyHandleOutliersEDCommand(this, parameters)

  override fun getGroupName() = CommandFactoryGroup.HANDLING_OUTLIERS_AND_SKEWED

  override fun getDescription() = DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.handleOutliersWithED.description.hint.text")
}

internal class JupyterPyHandleOutliersEDCommand(factory: CommandFactory<JupyterPyHandleOutliersEDParams, PythonDataWranglerContext>, parameters: JupyterPyHandleOutliersEDParams)
  : JupyterPyCommandBase<JupyterPyHandleOutliersEDParams>(factory, parameters) {
  override fun getCommandLabel(): @Nls String =
    DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.handleOutliersWithED.label", "`${parameters.column}`")

  override fun getDescription(): @Nls String =
    DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.handleOutliersWithED.description", parameters.threshold, "`${parameters.column}`")

  override suspend fun generate(context: TableInfoData, generator: CodeGenerator) {
    val v = context.getTableName()
    val col = parameters.column
    generator.addImport("import numpy as np")
    generator.setCommandWrapper("""
      center = $v['$col'].mean()
      deviations = np.abs($v['$col'] - center)
      mad = np.median(deviations)
      threshold = ${parameters.threshold} * mad
      $v = $v[deviations < threshold]
    """.trimIndent())
  }

}