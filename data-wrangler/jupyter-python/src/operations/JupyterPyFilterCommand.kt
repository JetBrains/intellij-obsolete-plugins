package com.intellij.dataWrangler.jupyterPython.operations

import com.intellij.dataWrangler.annotations.ColumnIntent
import com.intellij.dataWrangler.annotations.CommandParameterName
import com.intellij.dataWrangler.annotations.DWColumnIntent
import com.intellij.dataWrangler.annotations.DWVariantType
import com.intellij.dataWrangler.annotations.DWVariantTypeItem
import com.intellij.dataWrangler.annotations.DisplayName
import com.intellij.dataWrangler.impl.operations.CommandFactoryBase
import com.intellij.dataWrangler.jupyterPython.DataWranglerJupyterPyBundle
import com.intellij.dataWrangler.jupyterPython.engine.PythonDataWranglerContext
import com.intellij.dataWrangler.jupyterPython.operations.JupyterPyFilterCondition.Constructed
import com.intellij.dataWrangler.jupyterPython.operations.JupyterPyFilterCondition.Expression
import com.intellij.dataWrangler.operations.CommandFactory
import com.intellij.dataWrangler.operations.CommandFactoryGroup
import com.intellij.dataWrangler.operations.DataWranglerCommand
import org.jetbrains.annotations.Nls
import java.util.function.Supplier

enum class JupyterPyFilterPredicate(val pyOperator: String, override val displayNamePtr: Supplier<@Nls String>) : DisplayName.Impl {
  LESS("<", DataWranglerJupyterPyBundle.messagePointer("data.wrangler.jupyter.command.filter.condition.less")),
  EQUAL("==", DataWranglerJupyterPyBundle.messagePointer("data.wrangler.jupyter.command.filter.condition.equal")),
  GREATER(">", DataWranglerJupyterPyBundle.messagePointer("data.wrangler.jupyter.command.filter.condition.greater")),
  NOT_EQUAL("!=", DataWranglerJupyterPyBundle.messagePointer("data.wrangler.jupyter.command.filter.condition.notEqual")),
  GREATER_OR_EQUAL(">=", DataWranglerJupyterPyBundle.messagePointer("data.wrangler.jupyter.command.filter.condition.greaterOrEqual")),
  LESS_OR_EQUAL("<=", DataWranglerJupyterPyBundle.messagePointer("data.wrangler.jupyter.command.filter.condition.lessOrEqual")),
}

data class JupyterPyFilterParams(
  @DWVariantType(
    DWVariantTypeItem(Constructed::class, "Condition"),
    DWVariantTypeItem(Expression::class, "Expression")
  )
  var condition: JupyterPyFilterCondition = Constructed()
)

sealed interface JupyterPyFilterCondition {
  data class Expression(
    @CommandParameterName("Expression") var expression: String = "",
  ): JupyterPyFilterCondition
  data class Constructed(
  @DWTableNumericColumn @property:DWColumnIntent(ColumnIntent.REFERENCE) var column: String = "",
  @CommandParameterName("Value") var value: Double = 0.0,
  @CommandParameterName("Condition") var condition: JupyterPyFilterPredicate = JupyterPyFilterPredicate.EQUAL,
  ): JupyterPyFilterCondition
}

internal class JupyterPyFilterFactory : CommandFactoryBase<JupyterPyFilterParams, PythonDataWranglerContext>(
  JupyterPyFilterParams::class,
  DataWranglerJupyterPyBundle.messagePointer("data.wrangler.jupyter.command.filter.name")
) {
  override fun createCommand(parameters: JupyterPyFilterParams): DataWranglerCommand<PythonDataWranglerContext> {
    return JupyterPyFilterCommand(this, parameters)
  }

  override fun getGroupName() = CommandFactoryGroup.SORT_AND_FILTER

  override fun getDescription() = DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.filter.description.hint.text")
}

internal class JupyterPyFilterCommand(factory: CommandFactory<JupyterPyFilterParams, PythonDataWranglerContext>, parameters: JupyterPyFilterParams)
  : JupyterPyCommandBase<JupyterPyFilterParams>(factory, parameters) {
  override fun getCommandLabel(): @Nls String =
    DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.filter.label", factory.commandName, "`${
      when (val c = parameters.condition) {
        is Constructed -> c.column
        is Expression -> c.expression
      }
    }`")

  override fun getDescription(): @Nls String =
    DataWranglerJupyterPyBundle.message(
      "data.wrangler.jupyter.command.filter.description",
      when (val c = parameters.condition) {
        is Constructed -> c.condition.getDisplayName() + " " + c.value
        is Expression -> c.expression
      })

  override suspend fun generate(context: TableInfoData, generator: CodeGenerator) {
    //TODO: Wrap values in Jupyter wrap. (Required: need to know column data type)
    val v = context.getTableName()
    when (val c = parameters.condition) {
      is Constructed -> {
        val col = c.column
        val commandCondition = c.condition
        val value = c.value
        val op = commandCondition.pyOperator
        generator.addCommand("$v = $v[$v[${col.pyStr}] $op $value]")
      }
      is Expression -> {
        generator.addCommand("$v = $v.query(${c.expression.pyStr})")
      }
    }

  }
}