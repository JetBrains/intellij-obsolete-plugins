package com.intellij.dataWrangler.jupyterPython.engine

import com.intellij.dataWrangler.executor.DataWranglerEngine
import com.intellij.dataWrangler.impl.database.DataWranglerLocalDatabase
import com.intellij.dataWrangler.impl.fus.DataWranglerInputType
import com.intellij.dataWrangler.impl.fus.DataWranglerProviderCollector
import com.intellij.dataWrangler.impl.operations.DataWranglerTransformationStepsManagerImpl
import com.intellij.dataWrangler.impl.service.DataWranglerService
import com.intellij.dataWrangler.impl.service.DataWranglerSessionImpl
import com.intellij.dataWrangler.impl.view.DATA_WRANGLER_GRID_KEY
import com.intellij.dataWrangler.impl.view.DWMainPanel
import com.intellij.dataWrangler.jupyterPython.engine.PythonDataWranglerEngineUtils.canCreateNotebookContext
import com.intellij.dataWrangler.jupyterPython.engine.PythonDataWranglerEngineUtils.canCreateTableFileContext
import com.intellij.dataWrangler.jupyterPython.engine.console.DataWranglerLocalFile
import com.intellij.dataWrangler.jupyterPython.engine.console.PyDataWranglerDatabaseContext
import com.intellij.dataWrangler.jupyterPython.engine.console.PyDataWranglerLocalTableContext
import com.intellij.dataWrangler.jupyterPython.operations.HandleOutliersMADFactory
import com.intellij.dataWrangler.jupyterPython.operations.JupyterGroupByCommandFactory
import com.intellij.dataWrangler.jupyterPython.operations.JupyterOneHotEncodingCommandFactory
import com.intellij.dataWrangler.jupyterPython.operations.JupyterPyChangeColumnTypeFactory
import com.intellij.dataWrangler.jupyterPython.operations.JupyterPyDropColFactory
import com.intellij.dataWrangler.jupyterPython.operations.JupyterPyDropDuplicatesFactory
import com.intellij.dataWrangler.jupyterPython.operations.JupyterPyDropMissFactory
import com.intellij.dataWrangler.jupyterPython.operations.JupyterPyDropRowsFactory
import com.intellij.dataWrangler.jupyterPython.operations.JupyterPyFillMissingValuesFactory
import com.intellij.dataWrangler.jupyterPython.operations.JupyterPyFilterFactory
import com.intellij.dataWrangler.jupyterPython.operations.JupyterPyHandleOutliersEDFactory
import com.intellij.dataWrangler.jupyterPython.operations.JupyterPyHandleOutliersWithIQRFactory
import com.intellij.dataWrangler.jupyterPython.operations.JupyterPyHandleSkewedFactory
import com.intellij.dataWrangler.jupyterPython.operations.JupyterPyMinMaxFactory
import com.intellij.dataWrangler.jupyterPython.operations.JupyterPyRemoveEmptyCommandFactory
import com.intellij.dataWrangler.jupyterPython.operations.JupyterPyReplaceFactory
import com.intellij.dataWrangler.jupyterPython.operations.JupyterPyRoundFactory
import com.intellij.dataWrangler.jupyterPython.operations.JupyterPySplitFactory
import com.intellij.dataWrangler.jupyterPython.operations.JupyterPyStandardizationFactory
import com.intellij.dataWrangler.jupyterPython.operations.JupyterStringTransformationCommandFactory
import com.intellij.dataWrangler.jupyterPython.operations.custom.JupyterCustomCommandFactories
import com.intellij.dataWrangler.jupyterPython.operations.init.JupyterCopyOutputVariableCommand
import com.intellij.dataWrangler.jupyterPython.operations.init.PythonCreateDataFrameCommand
import com.intellij.dataWrangler.jupyterPython.operations.init.PythonDatabaseTableCommand
import com.intellij.dataWrangler.operations.CommandFactory
import com.intellij.dataWrangler.operations.TransformationStep
import com.intellij.database.datagrid.DataGrid
import com.intellij.database.datagrid.GridUtil
import com.intellij.jupyter.core.core.impl.file.BackedNotebookVirtualFile
import com.intellij.jupyter.tables.JupyterTableCommandExecutorFactory
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.components.service
import com.intellij.python.scientific.powerfuldataviewer.editor.DataViewVirtualFile
import com.intellij.scientific.tables.DSDataHookUp
import com.intellij.scientific.tables.api.DSTableDataType
import com.intellij.scientific.tables.panel.DSTableWithStatistics
import com.intellij.scientific.tables.panel.DS_TABLE_DATA_KEY
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.jetbrains.python.console.PydevConsoleCommunication
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private val FIXED_COMMAND_FACTORIES = listOf(
  JupyterPyFilterFactory(),
  JupyterPyDropColFactory(),
  JupyterPyDropDuplicatesFactory(),
  JupyterPyDropMissFactory(),
  JupyterPyReplaceFactory(),
  JupyterPyRemoveEmptyCommandFactory(),
  JupyterStringTransformationCommandFactory(),
  JupyterOneHotEncodingCommandFactory(),
  JupyterGroupByCommandFactory(),
  JupyterPyDropRowsFactory(),
  JupyterPyFillMissingValuesFactory(),
  JupyterPyMinMaxFactory(),
  JupyterPyStandardizationFactory(),
  JupyterPyRoundFactory(),
  JupyterPyHandleOutliersWithIQRFactory(),
  JupyterPySplitFactory(),
  JupyterPyHandleSkewedFactory(),
  JupyterPyChangeColumnTypeFactory(),
  HandleOutliersMADFactory(),
  JupyterPyHandleOutliersEDFactory(),
)

private fun createFactoriesFlow() = service<JupyterCustomCommandFactories>().run {
  factories.map { FIXED_COMMAND_FACTORIES + it }.stateIn(cs, SharingStarted.Lazily, FIXED_COMMAND_FACTORIES)
}

private val FIXED_COMMAND_FACTORY_BY_ID = FIXED_COMMAND_FACTORIES.associateBy { it.id }

internal fun getCommandFactoryById(id: String): CommandFactory<*, PythonDataWranglerContext>? {
  FIXED_COMMAND_FACTORY_BY_ID[id]?.let { return it }
  return service<JupyterCustomCommandFactories>().getById(id)
}

private val COMMAND_INIT_BY_CONTEXT = mapOf(
  PyDataWranglerLocalTableContext::class.java to PythonCreateDataFrameCommand.createStep(),
  JupyterPyDataWranglerNotebookContext::class.java to JupyterCopyOutputVariableCommand.createStep(),
  PyDataWranglerDatabaseContext::class.java to PythonDatabaseTableCommand.createStep()
)

private val supportedLibraries = setOf<DSTableDataType>(DSTableDataType.PANDAS_DATA_FRAME)

object PythonDataWranglerEngineUtils {
  fun canCreateNotebookContext(dataContext: DataContext): Boolean {
    val dataGrid = GridUtil.getDataGrid(dataContext) ?: return false
    val dsTable = dataContext.getData(DS_TABLE_DATA_KEY) as? DSTableWithStatistics ?: return false
    val dataViewVirtualFile = dataContext.getData(CommonDataKeys.VIRTUAL_FILE) as? DataViewVirtualFile
    val tableDataRetriever = (dataGrid.dataHookup as? DSDataHookUp)?.loader?.getTableDataRetrieverFromDataSource() ?: return false
    (tableDataRetriever.panelInfo.editor ?: dataViewVirtualFile?.originalEditor) ?: return false
    if (tableDataRetriever.getTableType() !in supportedLibraries) return false
    dsTable.getDSTableCommandExecutor() ?: return false
    return true
  }

  fun canCreateTableFileContext(dataContext: DataContext): Boolean {
    return canCreatePythonLocalTableContext(dataContext) || canCreatePythonContextFromDatabase(dataContext)
  }

  internal fun canCreatePythonLocalTableContext(dataContext: DataContext): Boolean {
    val dataGrid = GridUtil.getDataGrid(dataContext) ?: return false
    val file = GridUtil.getVirtualFile(dataGrid.dataHookup) ?: return false
    return isSupportedDWFileExtension(file.extension ?: "")
  }

  internal fun canCreatePythonContextFromDatabase(@Suppress("UNUSED_PARAMETER") dataContext: DataContext): Boolean {
    return false
  }

  const val PYTHON_ENGINE_ID: String = "python"
}

class PythonDataWranglerEngine : DataWranglerEngine<PythonDataWranglerContext> {
  override val id: String
    get() = PythonDataWranglerEngineUtils.PYTHON_ENGINE_ID
  private val factories: StateFlow<List<CommandFactory<*, PythonDataWranglerContext>>> by lazy {
    createFactoriesFlow()
  }

  override fun commandsFactories(): StateFlow<List<CommandFactory<*, PythonDataWranglerContext>>> =
    factories

  override fun createInitialContext(dataContext: DataContext): PythonDataWranglerContext? {
    val dataGrid = GridUtil.getDataGrid(dataContext) ?: return null
    val dsTable = dataContext.getData(DS_TABLE_DATA_KEY) as? DSTableWithStatistics ?: return null
    val dataViewVirtualFile = dataContext.getData(CommonDataKeys.VIRTUAL_FILE) as? DataViewVirtualFile
    return createJupyterContext(dataGrid, dataViewVirtualFile, dsTable)
  }

  internal fun createJupyterContext(dataGrid: DataGrid, dataViewVirtualFile: DataViewVirtualFile?, dsTable: DSTableWithStatistics): PythonDataWranglerContext? {
    val tableDataRetriever = (dataGrid.dataHookup as? DSDataHookUp)?.loader?.getTableDataRetrieverFromDataSource() ?: return null
    val editor = (tableDataRetriever.panelInfo.editor ?: dataViewVirtualFile?.originalEditor) ?: return null
    val notebookVirtualFile = BackedNotebookVirtualFile.takeIfBacked(editor.virtualFile ?: return null) ?: return null
    if (tableDataRetriever.getTableType() !in supportedLibraries) return null
    // TODO investigate different executors for different tables
    val executor = JupyterTableCommandExecutorFactory.getJupyterTableCommandExecutor(dataGrid.project,
                                               notebookVirtualFile,
                                               cellPointer = null,
                                               editor = editor)
    return JupyterPyDataWranglerNotebookContext(dataGrid.project, dsTable, tableDataRetriever, executor, dataViewVirtualFile)
  }

  internal fun createPythonConsoleContext(dataGrid: DataGrid, pyFrame: PydevConsoleCommunication, localFile: DataWranglerLocalFile): PythonDataWranglerContext? {
    val tableDataRetriever = (dataGrid.dataHookup as? DSDataHookUp)?.loader?.getTableDataRetrieverFromDataSource() ?: return null
    if (tableDataRetriever.getTableType() !in supportedLibraries) return null
    return PyDataWranglerLocalTableContext(dataGrid.project, pyFrame, tableDataRetriever, localFile)
  }

  internal fun createPythonDatabaseConsoleContext(dataGrid: DataGrid, pyFrame: PydevConsoleCommunication, localDatabase: DataWranglerLocalDatabase): PythonDataWranglerContext? {
    val tableDataRetriever = (dataGrid.dataHookup as? DSDataHookUp)?.loader?.getTableDataRetrieverFromDataSource() ?: return null
    return PyDataWranglerDatabaseContext(dataGrid.project, pyFrame, tableDataRetriever, localDatabase)
  }

  override fun getPreviewProvider() = PyCodePreviewProvider()

  override fun canCreateDWContext(dataContext: DataContext): Boolean {
    return canCreateNotebookContext(dataContext) || canCreateTableFileContext(dataContext)
  }

  override fun getInitialStep(context: PythonDataWranglerContext): TransformationStep<*, PythonDataWranglerContext>? {
    return COMMAND_INIT_BY_CONTEXT[context::class.java]
  }
}

@RequiresEdt
internal fun getOrCreateTable(grid: DataGrid, engine: PythonDataWranglerEngine, context: PythonDataWranglerContext): DWMainPanel {
  return grid.getUserData(DATA_WRANGLER_GRID_KEY) ?: createMainPanel(grid, engine, context)
}

@RequiresEdt
private fun createMainPanel(grid: DataGrid, engine: PythonDataWranglerEngine, context: PythonDataWranglerContext): DWMainPanel {
  val transformationManager = DataWranglerTransformationStepsManagerImpl.createDefault(context)
  val previewProvider = engine.getPreviewProvider()
  val coroutineScope = grid.project.service<DataWranglerService>().coroutineScope
  val session = DataWranglerSessionImpl(coroutineScope, engine, transformationManager, previewProvider, context)
  DataWranglerProviderCollector.logDWOpened(if (context is PyDataWranglerLocalTableContext) DataWranglerInputType.LOCAL_FILE else DataWranglerInputType.PYTHON_VARIABLE)
  return DWMainPanel(grid, session)
}