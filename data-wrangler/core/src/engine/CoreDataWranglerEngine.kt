package com.intellij.dataWrangler.core.engine

import com.intellij.dataWrangler.core.operations.FilterCommandFactory
import com.intellij.dataWrangler.executor.DataWranglerEngine
import com.intellij.dataWrangler.operations.CommandFactory
import com.intellij.database.datagrid.CsvDocumentDataHookUp
import com.intellij.database.datagrid.GridUtil
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.util.registry.Registry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class CoreDataWranglerEngine private constructor() : DataWranglerEngine<DataWranglerCoreContext> {
  override val id: String
    get() = "core"
  private val factories: StateFlow<List<CommandFactory<*, DataWranglerCoreContext>>> =
    MutableStateFlow(listOf(FilterCommandFactory()))

  override fun createInitialContext(dataContext: DataContext): DataWranglerCoreContext? {
    if (!canCreateDWContext(dataContext)) return null
    val dataGrid = GridUtil.getDataGrid(dataContext) ?: return null
    return DataWranglerCoreContext(dataGrid)
  }

  override fun commandsFactories(): StateFlow<List<CommandFactory<*, DataWranglerCoreContext>>> =
    factories

  override fun canCreateDWContext(dataContext: DataContext): Boolean {
    if (!Registry.`is`("tables.datawrangler.plugin.show.core.internal.action")) return false
    val dataGrid = GridUtil.getDataGrid(dataContext) ?: return false
    return dataGrid.dataHookup is CsvDocumentDataHookUp
  }
}