package com.intellij.dataWrangler.jupyterPython.operations

import com.intellij.dataWrangler.annotations.ColumnIntent
import com.intellij.dataWrangler.annotations.CommandParameterName
import com.intellij.dataWrangler.annotations.DWColumnIntent
import com.intellij.dataWrangler.annotations.DisplayName
import com.intellij.dataWrangler.impl.operations.CommandFactoryBase
import com.intellij.dataWrangler.jupyterPython.DataWranglerJupyterPyBundle
import com.intellij.dataWrangler.jupyterPython.engine.PythonDataWranglerContext
import com.intellij.dataWrangler.operations.CommandFactory
import org.jetbrains.annotations.Nls
import java.util.function.Supplier

internal enum class RoundType(override val displayNamePtr: Supplier<@Nls String>) : DisplayName.Impl {
  ROUND(DataWranglerJupyterPyBundle.messagePointer("RoundType.round")),
  CEIL(DataWranglerJupyterPyBundle.messagePointer("RoundType.ceil")),
  FLOOR(DataWranglerJupyterPyBundle.messagePointer("RoundType.floor"))
}

internal data class JupyterPyRoundParameters(
  @DWTableNumericColumn @property:DWColumnIntent(ColumnIntent.CHANGE) var column: String = "",
  @CommandParameterName("Decimals") var decimals: Int = 0, // TODO: should handle too big/negative numbers?
  @CommandParameterName("Round type") var roundType: RoundType = RoundType.ROUND,
)

internal class JupyterPyRoundFactory : CommandFactoryBase<JupyterPyRoundParameters, PythonDataWranglerContext>(
  JupyterPyRoundParameters::class,
  DataWranglerJupyterPyBundle.messagePointer("data.wrangler.jupyter.command.round.name")
) {
  override fun createCommand(parameters: JupyterPyRoundParameters) =
    JupyterPyRoundCommand(this, parameters)

  override fun getDescription() = DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.round.description.hint.text")
}

internal class JupyterPyRoundCommand(factory: CommandFactory<JupyterPyRoundParameters, PythonDataWranglerContext>, parameters: JupyterPyRoundParameters)
  : JupyterPyCommandBase<JupyterPyRoundParameters>(factory, parameters) {
  override fun getCommandLabel(): @Nls String =
    DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.round.label", "`${parameters.column}`")

  override fun getDescription(): @Nls String =
    DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.round.description", parameters.decimals, "`${parameters.column}`")

  override suspend fun generate(context: TableInfoData, generator: CodeGenerator) {
    generator.addImport("import numpy as np")
    val v = context.getTableName()
    val col = parameters.column
    val d = parameters.decimals
    val commandCode = when (parameters.roundType) {
      RoundType.ROUND -> "$v = $v.round({'$col': $d})"
      RoundType.CEIL -> "factor = 10 ** $d\n$v['$col'] = np.ceil($v['$col'] * factor) / factor"
      RoundType.FLOOR -> "factor = 10 ** $d\n$v['$col'] = np.floor($v['$col'] * factor) / factor"
    }
    generator.addCommand(commandCode)
  }
}