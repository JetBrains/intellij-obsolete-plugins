package com.intellij.dataWrangler.jupyterPython.operations

import com.intellij.dataWrangler.annotations.CommandParameterName
import com.intellij.dataWrangler.impl.operations.CommandFactoryBase
import com.intellij.dataWrangler.jupyterPython.DataWranglerJupyterPyBundle
import com.intellij.dataWrangler.jupyterPython.engine.PythonDataWranglerContext
import com.intellij.dataWrangler.operations.CommandFactory
import com.intellij.dataWrangler.operations.CommandFactoryGroup
import org.jetbrains.annotations.Nls

internal data class JupyterPyDropRowsParameters(
  @CommandParameterName("From row ID") var from: Int = 0,
  @CommandParameterName("To row ID") var to: Int = 0,
)

internal class JupyterPyDropRowsFactory : CommandFactoryBase<JupyterPyDropRowsParameters, PythonDataWranglerContext>(
  JupyterPyDropRowsParameters::class,
  DataWranglerJupyterPyBundle.messagePointer("data.wrangler.jupyter.command.dropRows.name")
) {
  override fun createCommand(parameters: JupyterPyDropRowsParameters) =
    JupyterPyDropRowsCommand(this, parameters)

  override fun getGroupName() = CommandFactoryGroup.DROP

  override fun getDescription() = DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.dropRows.description.hint.text")
}

internal class JupyterPyDropRowsCommand(
  factory: CommandFactory<JupyterPyDropRowsParameters, PythonDataWranglerContext>, parameters: JupyterPyDropRowsParameters,
) : JupyterPyCommandBase<JupyterPyDropRowsParameters>(factory, parameters) {
  override fun getCommandLabel(): @Nls String = DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.dropRows.label", parameters.from,
                                                                                    parameters.to)

  override fun getDescription(): @Nls String = DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.dropRows.description", parameters.from,
                                                                                   parameters.to)

  override suspend fun generate(context: TableInfoData, generator: CodeGenerator) {
    val v = context.getTableName()
    val from = parameters.from
    val to = parameters.to + 1
    generator.addCommand("$v = $v.drop($v.index[$from:$to])")
  }
}