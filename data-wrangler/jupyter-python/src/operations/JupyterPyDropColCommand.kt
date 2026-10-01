package com.intellij.dataWrangler.jupyterPython.operations

import com.intellij.dataWrangler.annotations.ColumnIntent
import com.intellij.dataWrangler.annotations.DWColumnIntent
import com.intellij.dataWrangler.annotations.DWTableColumn
import com.intellij.dataWrangler.impl.operations.CommandFactoryBase
import com.intellij.dataWrangler.jupyterPython.DataWranglerJupyterPyBundle
import com.intellij.dataWrangler.jupyterPython.engine.PythonDataWranglerContext
import com.intellij.dataWrangler.operations.CommandFactory
import com.intellij.dataWrangler.operations.CommandFactoryGroup
import com.intellij.dataWrangler.operations.DataWranglerCommand
import org.jetbrains.annotations.Nls

data class JupyterPyDropColParams(@DWTableColumn @property:DWColumnIntent(ColumnIntent.REMOVE) var column: String = "")

internal class JupyterPyDropColFactory : CommandFactoryBase<JupyterPyDropColParams, PythonDataWranglerContext>(
  JupyterPyDropColParams::class,
  DataWranglerJupyterPyBundle.messagePointer("data.wrangler.jupyter.command.drop.name")
) {
  override fun createCommand(parameters: JupyterPyDropColParams): DataWranglerCommand<PythonDataWranglerContext> {
    return JupyterPyDropColCommand(this, parameters)
  }

  override fun getGroupName() = CommandFactoryGroup.DROP

  override fun getDescription() = DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.drop.description.hint.text")
}

internal class JupyterPyDropColCommand(factory: CommandFactory<JupyterPyDropColParams, PythonDataWranglerContext>, parameters: JupyterPyDropColParams) : JupyterPyCommandBase<JupyterPyDropColParams>(factory, parameters) {
  override fun getCommandLabel(): @Nls String =
    DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.drop.label", "`${parameters.column}`")

  override fun getDescription(): @Nls String = DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.drop.description", "`${parameters.column}`")

  override suspend fun generate(context: TableInfoData, generator: CodeGenerator) {
    val v = context.getTableName()
    val col = parameters.column
    generator.addCommand("$v = $v.drop(columns=['$col'])")
  }

}