package com.intellij.dataWrangler.jupyterPython.operations

import com.intellij.dataWrangler.annotations.ColumnIntent
import com.intellij.dataWrangler.annotations.CommandParameterName
import com.intellij.dataWrangler.annotations.DWColumnIntent
import com.intellij.dataWrangler.annotations.DWTableColumn
import com.intellij.dataWrangler.impl.operations.CommandFactoryBase
import com.intellij.dataWrangler.jupyterPython.DataWranglerJupyterPyBundle
import com.intellij.dataWrangler.jupyterPython.engine.PandasDataFrameType
import com.intellij.dataWrangler.jupyterPython.engine.PythonDataWranglerContext
import com.intellij.dataWrangler.operations.CommandFactory
import com.intellij.dataWrangler.operations.CommandFactoryGroup
import com.intellij.dataWrangler.operations.DataWranglerCommand
import org.jetbrains.annotations.Nls

data class JupyterPyReplaceParams(
  @DWTableColumn @property:DWColumnIntent(ColumnIntent.CHANGE) var column: String = "",
  @CommandParameterName("Old value") var oldValue: String = "",
  @CommandParameterName("New value") var newValue: String = "",
  @CommandParameterName("Match case") var matchCase: Boolean = false,
  @CommandParameterName("Match full string") var fullMatch: Boolean = false,
  @CommandParameterName("Use regular expression") var isRegex: Boolean = false,
)

internal class JupyterPyReplaceFactory : CommandFactoryBase<JupyterPyReplaceParams, PythonDataWranglerContext>(
  JupyterPyReplaceParams::class,
  DataWranglerJupyterPyBundle.messagePointer("data.wrangler.jupyter.command.replace.name")
) {
  override fun createCommand(parameters: JupyterPyReplaceParams): DataWranglerCommand<PythonDataWranglerContext> {
    return JupyterPyReplaceCommand(this, parameters)
  }

  override fun getGroupName() = CommandFactoryGroup.FIND_AND_REPLACE

  override fun getDescription() = DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.replace.description.hint.text")
}

internal class JupyterPyReplaceCommand(factory: CommandFactory<JupyterPyReplaceParams, PythonDataWranglerContext>, parameters: JupyterPyReplaceParams)
  : JupyterPyCommandBase<JupyterPyReplaceParams>(factory, parameters) {
  override fun getCommandLabel(): @Nls String = DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.replace.label", factory.commandName, "`${parameters.column}`")

  override fun getDescription(): @Nls String = DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.replace.description", parameters.oldValue, parameters.newValue)

  override suspend fun generate(context: TableInfoData, generator: CodeGenerator) {
    // TODO: support different column types / rewrite
    val pythonVariableName = context.getTableName()
    val columnName = parameters.column
    val type = context.getColumnsType(columnName)
    val columnAccess = "${pythonVariableName}['${columnName}']"
    val commandCode = when (type) {
      PandasDataFrameType.FLOAT, PandasDataFrameType.INT -> {
        "$columnAccess = $columnAccess.replace(${parameters.oldValue}, ${parameters.newValue})"
      }
      PandasDataFrameType.STRING, PandasDataFrameType.DATE, null -> {
        when {
          parameters.fullMatch && parameters.isRegex && parameters.matchCase -> "$columnAccess = $columnAccess.str.replace(\"^${parameters.oldValue}\$\", \"${parameters.newValue}\", regex=True)"
          parameters.fullMatch && parameters.isRegex -> "$columnAccess = $columnAccess.str.replace(\"^${parameters.oldValue}\$\", \"${parameters.newValue}\", regex=True, case=False)"
          parameters.fullMatch && parameters.matchCase -> "${pythonVariableName}.loc[${pythonVariableName}['${columnName}'] == \"${parameters.oldValue}\", '${columnName}'] = \"${parameters.newValue}\""
          parameters.matchCase && parameters.isRegex -> "$columnAccess = $columnAccess.str.replace(\"${parameters.oldValue}\", \"${parameters.newValue}\", regex=True)"
          parameters.fullMatch -> "${pythonVariableName}.loc[${pythonVariableName}['${columnName}'].str.lower() == \"${parameters.oldValue}\".lower(), '${columnName}'] = \"${parameters.newValue}\""
          parameters.isRegex -> "$columnAccess = $columnAccess.str.replace(\"${parameters.oldValue}\", \"${parameters.newValue}\", case=False, regex=True)"
          parameters.matchCase -> "$columnAccess = $columnAccess.str.replace(\"${parameters.oldValue}\", \"${parameters.newValue}\", regex=False)"
          else -> "$columnAccess = $columnAccess.str.replace(\"${parameters.oldValue}\", \"${parameters.newValue}\", case=False, regex=False)"
        }
      }
    }
    generator.addCommand(commandCode)
  }

}