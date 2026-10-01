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
import org.jetbrains.annotations.Nls
import java.util.function.Supplier

internal enum class SkewedTransformationType(val pyTr: String, override val displayNamePtr: Supplier<@Nls String>) : DisplayName.Impl {
  LOG("log1p", DataWranglerJupyterPyBundle.messagePointer("SkewedTransformationType.logarithmic")),
  SQRT("sqrt", DataWranglerJupyterPyBundle.messagePointer("SkewedTransformationType.square.root"))
}

internal data class JupyterPyHandleSkewedParameters(
  @DWTableNumericColumn @property:DWColumnIntent(ColumnIntent.CHANGE) var column: String = "",
  @CommandParameterName("Transformation") var transformation: SkewedTransformationType = SkewedTransformationType.LOG,
)

internal class JupyterPyHandleSkewedFactory : CommandFactoryBase<JupyterPyHandleSkewedParameters, PythonDataWranglerContext>(
  JupyterPyHandleSkewedParameters::class,
  DataWranglerJupyterPyBundle.messagePointer("data.wrangler.jupyter.command.handleSkewed.name")
) {
  override fun createCommand(parameters: JupyterPyHandleSkewedParameters) =
    JupyterPyHandleSkewedCommand(this, parameters)

  override fun getGroupName() = CommandFactoryGroup.HANDLING_OUTLIERS_AND_SKEWED

  override fun getDescription() = DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.handleSkewed.description.hint.text")
}

internal class JupyterPyHandleSkewedCommand(factory: CommandFactory<JupyterPyHandleSkewedParameters, PythonDataWranglerContext>, parameters: JupyterPyHandleSkewedParameters)
  : JupyterPyCommandBase<JupyterPyHandleSkewedParameters>(factory, parameters) {
  override fun getCommandLabel(): @Nls String =
    DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.handleSkewed.label", "`${parameters.column}`")

  override fun getDescription(): @Nls String =
    DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.handleSkewed.description", parameters.transformation.getDisplayName(), "`${parameters.column}`")

  override suspend fun generate(context: TableInfoData, generator: CodeGenerator) {
    val v = context.getTableName()
    val col = parameters.column
    val pyTr = parameters.transformation.pyTr
    generator.addImport("import numpy as np")
    generator.addCommand("$v['$col'] = $v['$col'].apply(np.$pyTr)")
  }

}