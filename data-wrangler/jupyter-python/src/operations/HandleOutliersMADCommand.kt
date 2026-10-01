package com.intellij.dataWrangler.jupyterPython.operations

import com.intellij.dataWrangler.annotations.ColumnIntent
import com.intellij.dataWrangler.annotations.CommandParameterName
import com.intellij.dataWrangler.annotations.DWColumnIntent
import com.intellij.dataWrangler.annotations.DisplayName
import com.intellij.dataWrangler.impl.operations.CommandFactoryBase
import com.intellij.dataWrangler.jupyterPython.DataWranglerJupyterPyBundle
import com.intellij.dataWrangler.jupyterPython.engine.PythonDataWranglerContext
import com.intellij.dataWrangler.operations.CommandFactory
import com.intellij.dataWrangler.operations.CommandFactoryGroup
import com.intellij.openapi.util.NlsSafe
import org.jetbrains.annotations.Nls

internal enum class MADThreshold(val value: Float) : DisplayName {
  TWO_AND_HALF(2.5f),
  THREE(3.0f),
  THREE_AND_HALF(3.5f),
  FOUR(4.0f),
  FIVE(5.0f),
  ;

  override fun getDisplayName(): @NlsSafe String =
    String.format("%.1f", value)
}

internal data class HandleOutliersMADParameters(
  @DWTableNumericColumn @property:DWColumnIntent(ColumnIntent.REFERENCE)
  var column: String = "",
  @CommandParameterName("Threshold")
  var threshold: MADThreshold = MADThreshold.THREE, // TODO: Should be double (float)? column types required
)

internal class HandleOutliersMADFactory : CommandFactoryBase<HandleOutliersMADParameters, PythonDataWranglerContext>(
  HandleOutliersMADParameters::class,
  DataWranglerJupyterPyBundle.messagePointer("data.wrangler.jupyter.command.handleOutliersWithMAD.name")
) {
  override fun createCommand(parameters: HandleOutliersMADParameters) =
    HandleOutliersMADCommand(this, parameters)

  override fun getGroupName() = CommandFactoryGroup.HANDLING_OUTLIERS_AND_SKEWED

  override fun getDescription() = DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.handleOutliersWithMAD.description.hint.text")
}

internal class HandleOutliersMADCommand(factory: CommandFactory<HandleOutliersMADParameters, PythonDataWranglerContext>, parameters: HandleOutliersMADParameters)
  : JupyterPyCommandBase<HandleOutliersMADParameters>(factory, parameters) {
  override fun getCommandLabel(): @Nls String =
    DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.handleOutliersWithMAD.label", "`${parameters.column}`")

  override fun getDescription(): @Nls String =
    DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.handleOutliersWithMAD.description", parameters.threshold.getDisplayName(), "`${parameters.column}`")

  override suspend fun generate(context: TableInfoData, generator: CodeGenerator) {
    val v = context.getTableName()
    val col = parameters.column
    generator.addImport("import numpy as np")
    generator.setCommandWrapper("""
      median_$col = $v['$col'].median()
      mad_$col = np.median(np.abs($v['$col'] - median_$col))

      threshold = ${parameters.threshold.value}

      mad_score_$col = 0.6745 * ($v['$col'] - median_$col) / mad_$col
      $v = $v[mad_score_$col.abs() < threshold]
    """.trimIndent())
  }

}