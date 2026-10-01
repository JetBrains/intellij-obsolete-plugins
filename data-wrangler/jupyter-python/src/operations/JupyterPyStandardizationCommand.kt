package com.intellij.dataWrangler.jupyterPython.operations

import com.intellij.dataWrangler.impl.operations.CommandFactoryBase
import com.intellij.dataWrangler.jupyterPython.DataWranglerJupyterPyBundle
import com.intellij.dataWrangler.jupyterPython.engine.PythonDataWranglerContext
import com.intellij.dataWrangler.operations.CommandFactory
import com.intellij.dataWrangler.operations.CommandFactoryGroup
import org.jetbrains.annotations.Nls

internal class JupyterPyStandardizationFactory : CommandFactoryBase<JupyterColumnParams, PythonDataWranglerContext>(
  JupyterColumnParams::class,
  DataWranglerJupyterPyBundle.messagePointer("data.wrangler.jupyter.command.standardization.name")
) {
  override fun createCommand(parameters: JupyterColumnParams) = JupyterPyStandardizationCommand(this, parameters)

  override fun getGroupName() = CommandFactoryGroup.NORMALIZATION_AND_SCALING

  override fun getDescription() = DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.standardization.description.hint.text")
}

internal class JupyterPyStandardizationCommand(factory: CommandFactory<JupyterColumnParams, PythonDataWranglerContext>, parameters: JupyterColumnParams)
  : JupyterPyCommandBase<JupyterColumnParams>(factory, parameters) {
  override fun getCommandLabel(): @Nls String =
    DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.standardization.label", "`${parameters.column}`")

  override fun getDescription(): @Nls String =
    DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.standardization.description", "`${parameters.column}`")

  override suspend fun generate(context: TableInfoData, generator: CodeGenerator) {
    val v = context.getTableName()
    val col = parameters.column
    generator.addCommand("""
      def z_score_normalize(column):
        return (column - column.mean()) / column.std()

      $v['$col'] = z_score_normalize($v['$col'])""".trimIndent())
  }
}