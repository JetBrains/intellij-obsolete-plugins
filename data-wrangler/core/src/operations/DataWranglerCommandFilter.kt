package com.intellij.dataWrangler.core.operations

import com.intellij.dataWrangler.annotations.ColumnIntent
import com.intellij.dataWrangler.annotations.CommandParameterName
import com.intellij.dataWrangler.annotations.DWColumnIntent
import com.intellij.dataWrangler.annotations.DWTableColumn
import com.intellij.dataWrangler.annotations.DisplayName
import com.intellij.dataWrangler.core.CoreDataWranglerBundle
import com.intellij.dataWrangler.core.engine.DataWranglerCoreContext
import com.intellij.dataWrangler.impl.operations.CommandFactoryBase
import com.intellij.dataWrangler.operations.CommandFactory
import com.intellij.dataWrangler.operations.CommandFactoryGroup
import com.intellij.database.datagrid.GridColumn
import com.intellij.database.datagrid.GridDataHookUp
import com.intellij.database.datagrid.GridRow
import org.jetbrains.annotations.Nls
import java.util.function.Supplier

internal enum class FilterCondition(override val displayNamePtr: Supplier<@Nls String>) : DisplayName.Impl {
  LESS(CoreDataWranglerBundle.messagePointer("label.transformations.filters.less")),
  GREATER(CoreDataWranglerBundle.messagePointer("label.transformations.filters.greater")),
  EQUAL(CoreDataWranglerBundle.messagePointer("label.transformations.filters.equal"))
}

internal class FilterParameters(
  @DWTableColumn @property:DWColumnIntent(ColumnIntent.CHANGE)
  var colName: String = "",
  @CommandParameterName("Value")
  var value: String = "",
  @CommandParameterName("Condition")
  var condition: FilterCondition = FilterCondition.EQUAL
)

internal class FilterCommandFactory : CommandFactoryBase<FilterParameters, DataWranglerCoreContext>(
  FilterParameters::class,
  CoreDataWranglerBundle.messagePointer("label.transformations.commandName.filter")
) {
  override fun initDefaultParameters(context: DataWranglerCoreContext, defaultParams: FilterParameters) {
    defaultParams.value = context.getText(0, 0)
    defaultParams.condition = FilterCondition.EQUAL
  }

  override fun createCommand(parameters: FilterParameters) = DataWranglerCommandFilter(this, parameters)

  override fun getGroupName() = CommandFactoryGroup.SORT_AND_FILTER
}

internal class DataWranglerCommandFilter(factory: CommandFactory<FilterParameters, DataWranglerCoreContext>, parameters: FilterParameters)
  : DataWranglerCoreCommandBase<FilterParameters>(factory, parameters) {

  override fun getCommandLabel(): @Nls String =
    CoreDataWranglerBundle.message("label.transformations.commandName.filter.label", CoreDataWranglerBundle.message("label.transformations.commandName.filter"), parameters.colName)

  override fun getDescription(): @Nls String =
    CoreDataWranglerBundle.message("label.transformations.commandName.description", parameters.condition.getDisplayName(), parameters.value)

  override suspend fun execute(context: DataWranglerCoreContext) {
    if (!filterTable(parameters, context.getGridDataHookUp() as GridDataHookUp<GridRow, GridColumn>)) {
      throw RuntimeException("No column with such name")
    }
  }

  companion object {
    fun filterTable(parameters: FilterParameters, hookUp: GridDataHookUp<GridRow, GridColumn>): Boolean {
      val columnId = hookUp.dataModel.columns.find { it.name == parameters.colName }?.columnNumber ?: return false
      val rowsToDelete = mutableListOf<Int>()
      for (rowId in 0..<hookUp.dataModel.rowCount) {
        val value = DataWranglerCoreContext.getText(rowId, columnId, hookUp)

        when (parameters.condition) {
          FilterCondition.LESS -> if (parameters.value <= value) {
            rowsToDelete.add(rowId)
          }
          FilterCondition.GREATER -> if (parameters.value >= value) {
            rowsToDelete.add(rowId)
          }
          FilterCondition.EQUAL -> if (parameters.value != value) {
            rowsToDelete.add(rowId)
          }
        }
      }

      DataWranglerCoreContext.deleteRows(rowsToDelete, hookUp)
      return true
    }
  }
}