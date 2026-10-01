package com.intellij.dataWrangler.jupyterPython.operations

import com.intellij.dataWrangler.annotations.CommandParameterName
import com.intellij.dataWrangler.impl.operations.CommandFactoryBase
import com.intellij.dataWrangler.jupyterPython.DataWranglerJupyterPyBundle
import com.intellij.dataWrangler.jupyterPython.engine.PythonDataWranglerContext
import com.intellij.dataWrangler.operations.CommandFactory
import com.intellij.dataWrangler.operations.CommandFactoryGroup
import com.intellij.dataWrangler.operations.DataWranglerCommand
import org.jetbrains.annotations.Nls

data class JupyterPyRemoveEmptyParams(@CommandParameterName("At least one empty value in fields") var emptyValueFlag: Boolean = false)

internal class JupyterPyRemoveEmptyCommandFactory : CommandFactoryBase<JupyterPyRemoveEmptyParams, PythonDataWranglerContext>(
  JupyterPyRemoveEmptyParams::class,
  DataWranglerJupyterPyBundle.messagePointer("data.wrangler.jupyter.command.remove.name")
) {
  override fun createCommand(parameters: JupyterPyRemoveEmptyParams): DataWranglerCommand<PythonDataWranglerContext> {
    return JupyterPyRemoveEmptyCommand(this, parameters)
  }

  override fun getGroupName() = CommandFactoryGroup.DROP

  override fun getDescription() = DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.remove.description.hint.text")
}

internal class JupyterPyRemoveEmptyCommand(factory: CommandFactory<JupyterPyRemoveEmptyParams, PythonDataWranglerContext>, parameters: JupyterPyRemoveEmptyParams)
  : JupyterPyCommandBase<JupyterPyRemoveEmptyParams>(factory, parameters) {
  override fun getCommandLabel(): @Nls String =
    DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.remove.label")

  override fun getDescription(): @Nls String = DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.remove.description")

  // It could potentially remove the column with all flags. Description and label will have to change if we allow 'all' flag.
  override suspend fun generate(context: TableInfoData, generator: CodeGenerator) {
    val v = context.getTableName()
    val mode = if (parameters.emptyValueFlag) "any" else "all"
    generator.addCommand("$v.dropna(how=\"${mode}\", inplace=True)")
  }

}