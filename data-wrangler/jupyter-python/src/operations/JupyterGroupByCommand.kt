package com.intellij.dataWrangler.jupyterPython.operations

import com.intellij.dataWrangler.annotations.DWCollectionItemType
import com.intellij.dataWrangler.annotations.DWTableColumn
import com.intellij.dataWrangler.impl.operations.CommandFactoryBase
import com.intellij.dataWrangler.jupyterPython.DataWranglerJupyterPyBundle
import com.intellij.dataWrangler.jupyterPython.engine.PythonDataWranglerContext
import com.intellij.dataWrangler.operations.CommandFactory
import com.intellij.dataWrangler.operations.CommandFactoryGroup
import com.intellij.dataWrangler.operations.DataWranglerCommand
import org.jetbrains.annotations.Nls

enum class JupyterGroupByAggregate(val pyFun: String) {
  MEAN("mean"),
  SUM("sum"),
  MIN("min"),
  MAX("max"),
  COUNT("count")
}

data class JupyterGroupByParameters(
  @DWCollectionItemType(DWTableColumn::class)
  var columns: Set<String> = mutableSetOf(),
  var aggregate: JupyterGroupByAggregate = JupyterGroupByAggregate.MEAN
)

internal class JupyterGroupByCommandFactory : CommandFactoryBase<JupyterGroupByParameters, PythonDataWranglerContext>(
  JupyterGroupByParameters::class,
  DataWranglerJupyterPyBundle.messagePointer("data.wrangler.jupyter.command.group.name")
) {
  override fun createCommand(parameters: JupyterGroupByParameters): DataWranglerCommand<PythonDataWranglerContext> {
    return JupyterGroupByCommand(this, parameters)
  }

  override fun getGroupName() = CommandFactoryGroup.OTHERS

  override fun getDescription() = DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.group.description.hint.text")
}

internal class JupyterGroupByCommand(factory: CommandFactory<JupyterGroupByParameters, PythonDataWranglerContext>, parameters: JupyterGroupByParameters)
  : JupyterPyCommandBase<JupyterGroupByParameters>(factory, parameters) {
  override fun getCommandLabel(): @Nls String =
    DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.group.label", parameters.columns.joinToString(), parameters.aggregate.pyFun)

  override fun getDescription(): @Nls String =
    DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.group.description", parameters.columns.joinToString(), parameters.aggregate.pyFun)

  override suspend fun generate(context: TableInfoData, generator: CodeGenerator) {
    val v = context.getTableName()
    val cols = parameters.columns
    val agg = parameters.aggregate.pyFun
    generator.addCommand("$v = $v.groupby([${cols.joinToString { it.pyStr }}], as_index = False).${agg}()")
  }

}