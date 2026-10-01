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

data class JupyterColumnParams(@DWTableColumn @property:DWColumnIntent(ColumnIntent.REFERENCE) var column: String = "")

internal class JupyterPyDropDuplicatesFactory : CommandFactoryBase<JupyterColumnParams, PythonDataWranglerContext>(
  JupyterColumnParams::class,
  DataWranglerJupyterPyBundle.messagePointer("data.wrangler.jupyter.command.duplicates.name")
) {
  override fun createCommand(parameters: JupyterColumnParams): DataWranglerCommand<PythonDataWranglerContext> {
    return JupyterPyDropDuplicatesCommand(this, parameters)
  }

  override fun getGroupName() = CommandFactoryGroup.DROP

  override fun getDescription() = DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.duplicates.description.hint.text")
}

internal class JupyterPyDropDuplicatesCommand(factory: CommandFactory<JupyterColumnParams, PythonDataWranglerContext>, parameters: JupyterColumnParams)
  : JupyterPyCommandBase<JupyterColumnParams>(factory, parameters) {
  override fun getCommandLabel(): @Nls String = DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.duplicates.label",
                                                                                    "`${parameters.column}`")

  override fun getDescription(): @Nls String = DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.duplicates.description")

  override suspend fun generate(context: TableInfoData, generator: CodeGenerator) {
    val v = context.getTableName()
    val col = parameters.column
    generator.addCommand("$v = $v.drop_duplicates(subset=['$col'])")
  }

}