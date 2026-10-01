package com.intellij.dataWrangler.jupyterPython.operations

import com.intellij.dataWrangler.impl.operations.CommandFactoryBase
import com.intellij.dataWrangler.jupyterPython.DataWranglerJupyterPyBundle
import com.intellij.dataWrangler.jupyterPython.engine.PythonDataWranglerContext
import com.intellij.dataWrangler.operations.CommandFactory
import com.intellij.dataWrangler.operations.CommandFactoryGroup
import com.intellij.dataWrangler.operations.DataWranglerCommand
import org.jetbrains.annotations.Nls

internal class JupyterPyDropMissFactory : CommandFactoryBase<JupyterColumnParams, PythonDataWranglerContext>(
  JupyterColumnParams::class,
  DataWranglerJupyterPyBundle.messagePointer("data.wrangler.jupyter.command.miss.name")
) {
  override fun createCommand(parameters: JupyterColumnParams): DataWranglerCommand<PythonDataWranglerContext> {
    return JupyterPyDropMissCommand(this, parameters)
  }

  override fun getDescription() = DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.miss.description.hint.text")

  override fun getGroupName() = CommandFactoryGroup.DROP
}

internal class JupyterPyDropMissCommand(factory: CommandFactory<JupyterColumnParams, PythonDataWranglerContext>, parameters: JupyterColumnParams)
  : JupyterPyCommandBase<JupyterColumnParams>(factory, parameters) {
  override fun getCommandLabel(): @Nls String = DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.miss.label", "`${parameters.column}`")

  override fun getDescription(): @Nls String = DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.miss.description", "`${parameters.column}`")

  override suspend fun generate(context: TableInfoData, generator: CodeGenerator) {
    val v = context.getTableName()
    val col = parameters.column
    generator.addCommand("$v = $v.dropna(subset=['$col'])")
  }

}