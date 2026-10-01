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

internal enum class IQMultiplier(val value: Float) : DisplayName {
  ONE_WITH_HALF(1.5f),
  ONE(1f),
  TWO(2f),
  THREE(3f),
  ;

  override fun getDisplayName(): @NlsSafe String =
    String.format("%.1f", value)
}

internal data class JupyterPyHandleOutliersWithIQRParameters(
  @DWTableNumericColumn @property:DWColumnIntent(ColumnIntent.REFERENCE) var column: String = "",
  @CommandParameterName("Multiplier") var multiplier: IQMultiplier = IQMultiplier.ONE_WITH_HALF,
)

internal class JupyterPyHandleOutliersWithIQRFactory : CommandFactoryBase<JupyterPyHandleOutliersWithIQRParameters, PythonDataWranglerContext>(
  JupyterPyHandleOutliersWithIQRParameters::class,
  DataWranglerJupyterPyBundle.messagePointer("data.wrangler.jupyter.command.handleOutliersWithIQR.name")
) {
  override fun createCommand(parameters: JupyterPyHandleOutliersWithIQRParameters) =
    JupyterPyHandleOutliersWithIQRCommand(this, parameters)

  override fun getGroupName() = CommandFactoryGroup.HANDLING_OUTLIERS_AND_SKEWED

  override fun getDescription() = DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.handleOutliersWithIQR.description.hint.text")
}

internal class JupyterPyHandleOutliersWithIQRCommand(factory: CommandFactory<JupyterPyHandleOutliersWithIQRParameters, PythonDataWranglerContext>, parameters: JupyterPyHandleOutliersWithIQRParameters)
  : JupyterPyCommandBase<JupyterPyHandleOutliersWithIQRParameters>(factory, parameters) {

  override fun getCommandLabel(): @Nls String =
    DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.handleOutliersWithIQR.label",
                                        "`${parameters.column}`")

  override fun getDescription(): @Nls String =
    DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.handleOutliersWithIQR.description", parameters.multiplier.getDisplayName(),
                                        "`${parameters.column}`")

  override suspend fun generate(context: TableInfoData, generator: CodeGenerator) {
    val v = context.getTableName()
    val col = parameters.column
    val multiplier = parameters.multiplier.value
    generator.setCommandWrapper("""
      Q1 = $v['$col'].quantile(0.25)
      Q3 = $v['$col'].quantile(0.75)
      IQR = Q3 - Q1
      lower_bound = Q1 - $multiplier * IQR
      upper_bound = Q3 + $multiplier * IQR
      $v = $v[($v['$col'] >= lower_bound) & ($v['$col'] <= upper_bound)]
    """.trimIndent())
  }

}