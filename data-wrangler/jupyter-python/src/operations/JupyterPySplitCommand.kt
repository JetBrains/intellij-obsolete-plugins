package com.intellij.dataWrangler.jupyterPython.operations

import com.intellij.dataWrangler.annotations.ColumnIntent
import com.intellij.dataWrangler.annotations.CommandParameterName
import com.intellij.dataWrangler.annotations.DWColumnIntent
import com.intellij.dataWrangler.impl.operations.CommandFactoryBase
import com.intellij.dataWrangler.jupyterPython.DataWranglerJupyterPyBundle
import com.intellij.dataWrangler.jupyterPython.engine.PythonDataWranglerContext
import com.intellij.dataWrangler.operations.CommandFactory
import org.jetbrains.annotations.Nls

internal data class JupyterPySplitParameters(
  @DWTableStringColumn @property:DWColumnIntent(ColumnIntent.CHANGE) var column: String = "",
  @CommandParameterName("Delimiter") var delimiter: String = ",",
  @CommandParameterName("Maximum splits") var maxSplits: Int = 2,
)

internal class JupyterPySplitFactory : CommandFactoryBase<JupyterPySplitParameters, PythonDataWranglerContext>(
  JupyterPySplitParameters::class,
  DataWranglerJupyterPyBundle.messagePointer("data.wrangler.jupyter.command.split.name")
) {
  override fun createCommand(parameters: JupyterPySplitParameters) =
    JupyterPySplitCommand(this, parameters)

  override fun getDescription() = DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.split.description.hint.text")
}

internal class JupyterPySplitCommand(factory: CommandFactory<JupyterPySplitParameters, PythonDataWranglerContext>, parameters: JupyterPySplitParameters)
  : JupyterPyCommandBase<JupyterPySplitParameters>(factory, parameters) {
  override fun getCommandLabel(): @Nls String =
    DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.split.label", "`${parameters.column}`")

  override fun getDescription(): @Nls String =
    DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.split.description", "`${parameters.column}`", "`${parameters.delimiter}`", parameters.maxSplits)

  override suspend fun generate(context: TableInfoData, generator: CodeGenerator) {
    val v = context.getTableName()
    val delimiter = parameters.delimiter
    val col = parameters.column
    val maxSplits = parameters.maxSplits
    generator.addCommand("""
      num_splits = int(min($v['$col'].str.count('$delimiter').max() + 1, $maxSplits))
      if num_splits <= 1:
          raise Exception("num_splits is not sufficient")

      new_columns = [f'${col}_{i + 1}' for i in range(num_splits)]
      split_cols = $v['$col'].str.split('$delimiter', n=num_splits - 1, expand=True)

      col_index = $v.columns.get_loc('$col')
      $v = $v.drop('$col', axis=1)
      for i, col in enumerate(new_columns):
          $v.insert(col_index + i, col, split_cols[i])
      """.trimIndent())
  }

}