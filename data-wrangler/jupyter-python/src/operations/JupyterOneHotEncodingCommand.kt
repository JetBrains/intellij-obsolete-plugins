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

data class JupyterOneHotEncoding(
  @DWTableColumn @property:DWColumnIntent(ColumnIntent.CHANGE) var column: String = "",
)

internal class JupyterOneHotEncodingCommandFactory : CommandFactoryBase<JupyterOneHotEncoding, PythonDataWranglerContext>(
  JupyterOneHotEncoding::class,
  DataWranglerJupyterPyBundle.messagePointer("data.wrangler.jupyter.command.encoding.name")
) {
  override fun createCommand(parameters: JupyterOneHotEncoding): DataWranglerCommand<PythonDataWranglerContext> {
    return JupyterOneHotEncodingCommand(this, parameters)
  }

  override fun getGroupName() = CommandFactoryGroup.ADD

  override fun getDescription() = DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.encoding.description.hint.text")
}

internal class JupyterOneHotEncodingCommand(factory: CommandFactory<JupyterOneHotEncoding, PythonDataWranglerContext>, parameters: JupyterOneHotEncoding)
  : JupyterPyCommandBase<JupyterOneHotEncoding>(factory, parameters) {
  override fun getCommandLabel(): @Nls String =
    DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.encoding.label", "`${parameters.column}`")

  override fun getDescription(): @Nls String =
    DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.encoding.description", "`${parameters.column}`")

  override suspend fun generate(context: TableInfoData, generator: CodeGenerator) {
    val v = context.getTableName()
    val col = parameters.column
    generator.addCommand("$v = pd.get_dummies($v, columns=['$col'])")
  }

}