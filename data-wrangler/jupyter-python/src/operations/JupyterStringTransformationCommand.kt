package com.intellij.dataWrangler.jupyterPython.operations

import com.intellij.dataWrangler.annotations.ColumnIntent
import com.intellij.dataWrangler.annotations.CommandParameterName
import com.intellij.dataWrangler.annotations.DWColumnIntent
import com.intellij.dataWrangler.annotations.DWTableColumn
import com.intellij.dataWrangler.annotations.DisplayName
import com.intellij.dataWrangler.annotations.ValuesProvider
import com.intellij.dataWrangler.impl.operations.CommandFactoryBase
import com.intellij.dataWrangler.jupyterPython.DataWranglerJupyterPyBundle
import com.intellij.dataWrangler.jupyterPython.engine.PythonDataWranglerContext
import com.intellij.dataWrangler.operations.CommandFactory
import com.intellij.dataWrangler.operations.CommandFactoryGroup
import com.intellij.dataWrangler.operations.DataWranglerCommand
import org.jetbrains.annotations.Nls
import java.util.function.Supplier

enum class JupyterStringTransformationCondition(val pyFun: String, override val displayNamePtr: Supplier<@Nls String>) : DisplayName.Impl {
  UPPER_CASE("upper", DataWranglerJupyterPyBundle.messagePointer("JupyterStringTransformationCondition.convert.to.upper.case")),
  LOWER_CASE("lower", DataWranglerJupyterPyBundle.messagePointer("JupyterStringTransformationCondition.convert.to.lower.case")),
  CAPITALIZE("capitalize", DataWranglerJupyterPyBundle.messagePointer("JupyterStringTransformationCondition.capitalize.first.character"))
}

data class JupyterStringTransformation(
  @DWTableColumn @DWColumnIntent(ColumnIntent.CHANGE) @CommandParameterName("Column") @ValuesProvider(StringTableColumnsProvider::class) var column: String = "",
  @CommandParameterName("Transformation type") var condition: JupyterStringTransformationCondition = JupyterStringTransformationCondition.CAPITALIZE,
  @CommandParameterName("New column name") var newColumnName: String = "",
)

internal class JupyterStringTransformationCommandFactory : CommandFactoryBase<JupyterStringTransformation, PythonDataWranglerContext>(
  JupyterStringTransformation::class,
  DataWranglerJupyterPyBundle.messagePointer("data.wrangler.jupyter.command.string.transform.name")
) {
  override fun createCommand(parameters: JupyterStringTransformation): DataWranglerCommand<PythonDataWranglerContext> {
    return JupyterStringTransformationCommand(this, parameters)
  }

  override fun getGroupName() = CommandFactoryGroup.ADD

  override fun getDescription() = DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.string.transform.description.hint.text")
}

internal class JupyterStringTransformationCommand(factory: CommandFactory<JupyterStringTransformation, PythonDataWranglerContext>, parameters: JupyterStringTransformation)
  : JupyterPyCommandBase<JupyterStringTransformation>(factory, parameters) {
  override fun getCommandLabel(): @Nls String = DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.string.transform.label", "`${parameters.column}`")

  override fun getDescription(): @Nls String = DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.string.transform.description",
                                                                                   parameters.condition.displayNamePtr.get(),
                                                                                   "`${parameters.column}`",
                                                                                   "`${parameters.newColumnName}`")

  override suspend fun generate(context: TableInfoData, generator: CodeGenerator) {
    val v = context.getTableName()
    val col = parameters.column
    val pyFun = parameters.condition.pyFun
    val newCol = parameters.newColumnName
    generator.addCommand("$v['$newCol'] = $v['$col'].str.$pyFun()")
  }
}