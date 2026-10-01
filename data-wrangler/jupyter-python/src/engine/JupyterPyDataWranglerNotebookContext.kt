package com.intellij.dataWrangler.jupyterPython.engine

import com.intellij.dataWrangler.jupyterPython.DataWranglerJupyterPyBundle
import com.intellij.jupyter.core.core.api.actions.NotebookCellLinesEditHelper
import com.intellij.notebooks.jupyter.core.jupyter.CellType
import com.intellij.notebooks.visualization.NotebookCellLines
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.python.scientific.powerfuldataviewer.editor.DataViewVirtualFile
import com.intellij.scientific.tables.DSTableDataRetrieverFromDataSourceImpl
import com.intellij.scientific.tables.api.DSDataFrameInfo
import com.intellij.scientific.tables.api.DSTableCommandExecutor
import com.intellij.scientific.tables.api.DSTableDataRetrieverFromDataSource
import com.intellij.scientific.tables.api.OutputPsiExpression
import com.intellij.scientific.tables.panel.DSTableWithStatistics
import com.intellij.scientific.tables.utils.launchIO
import com.jetbrains.python.PyElementTypes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JupyterPyDataWranglerNotebookContext(
  private val project: Project,
  private val table: DSTableWithStatistics,
  private var tableDataRetriever: DSTableDataRetrieverFromDataSource,
  private val executor: DSTableCommandExecutor,
  private val dataViewVirtualFile: DataViewVirtualFile?,
) : PythonDataWranglerContext {

  private val variablePsiElement: OutputPsiExpression? = tableDataRetriever.panelInfo.outputExpression
                                                         ?: dataViewVirtualFile?.outputPsiExpression

  // Generates unique var name for table. Table expression is named with format Out[x], but we can open multiple Out[x] with the same x, thus we need different names for them
  private var tableName = ""

  private val variableCodePreviewName: String = if (variablePsiElement == null) DW_VARIABLE_NAME
  else generateInputVariable(variablePsiElement).getNameForCodePreview()

  init {
    launchIO {
      val initialTableExpression = tableDataRetriever.initialTableExpression
      tableName = "__dw_table_${parseTableExpressionName(initialTableExpression)}_${System.currentTimeMillis()}__"
      // Executing command without updating the table. Creates copy variable to use for data wrangler.
      val commandCode = "${tableName} = ${initialTableExpression}.copy(deep=True)"
      executor.executeCommand(commandCode).getOrThrow()
    }
  }

  override fun getVariableCodePreviewName(): String = variableCodePreviewName

  /**
   * Table name currently displayed in the data wrangler.
   */
  override fun getTableName(): String = tableName
  override fun getProject(): Project = project
  override fun getInitializationCode(): String = generateInitializationCode(variablePsiElement)

  override fun dispose() {
    launchIO {
      executor.executeCommand(getCodeDeletePyVariable()).onFailure {
        thisLogger().warn("Failed to delete variable ${getVariableCodePreviewName()}", it)
      }
    }
  }

  override fun getDSDataFrameInfo(): DSDataFrameInfo = tableDataRetriever.dataFrameInfo
  fun generateInputVariableName(): String {
    return if (variablePsiElement != null) {
      generateInputVariable(variablePsiElement).variableName
    }
    else {
      // If we don't know the variable name from the input table, we can do little about it.
      // This case shouldn't happen, but if it happens, put placeholder and don't distrust exporting.
      generateOutputVariable()
    }
  }

  override fun getFile(): VirtualFile? = (tableDataRetriever.panelInfo.editor ?: dataViewVirtualFile?.originalEditor)?.virtualFile

  override suspend fun executeCommand(commandCode: String) {
    withContext(Dispatchers.IO) {
      executor.executeCommand(commandCode).onFailure {
        thisLogger().warn("Data Wrangler: Failed to execute command $commandCode", it)
      }.getOrThrow()
      updateTableDataExtractor()
    }
  }

  private fun generateInitializationCode(variablePsiElement: OutputPsiExpression?): String {
    // If we don't know the variable name from the input table, we can do little about it.
    // This case shouldn't happen, but if it happens, put a placeholder and still be able to show a code preview.
    if (variablePsiElement == null) return "${generateOutputVariable()} = ${generateOutputVariable()}"
    val inputVariable = generateInputVariable(variablePsiElement)
    return "${generateOutputVariable()} = ${inputVariable.asCopyExpression()}"
  }

  private fun generateInputVariable(variablePsiElement: OutputPsiExpression): TableVariable = TableVariable(variablePsiElement)

  // will be changed to supply unique variable name later.
  private fun generateOutputVariable(): String = DW_VARIABLE_NAME

  private suspend fun updateTableDataExtractor() {
    val expressionNew = getTableName()
    val preTableDataRetriever = tableDataRetriever
    val newDataFrameInfo = preTableDataRetriever.getTableDataProvider().loadDynamicTableDataFrameInfo(executor, expressionNew, "")
    val newTableDataRetriever = DSTableDataRetrieverFromDataSourceImpl(preTableDataRetriever.dataId, preTableDataRetriever.getDataManager(),
                                                                       expressionNew, preTableDataRetriever.getTableDataProvider(), executor, newDataFrameInfo, preTableDataRetriever.panelInfo, true)

    Disposer.register(preTableDataRetriever, newTableDataRetriever)
    // Update table
    tableDataRetriever = newTableDataRetriever
    table.fetchData(newTableDataRetriever, true)
  }

  private fun parseTableExpressionName(tableExpression: String): String {
    val regex = """Out\[(\d+)]""".toRegex()
    val matchResult = regex.find(tableExpression)
    return if (matchResult != null) {
      matchResult.groupValues[1]
    }
    else {
      tableExpression
    }
  }

  fun getNotebookName(): String? {
    val editor = tableDataRetriever.panelInfo.editor ?: dataViewVirtualFile?.originalEditor ?: return null
    return editor.virtualFile?.name
  }

  // keep until DW cells are stable
  fun addCodeCellAndNavigate(generatedCode: String) {
    if (generatedCode.isBlank()) return
    val editor = tableDataRetriever.panelInfo.editor ?: dataViewVirtualFile?.originalEditor ?: return
    val document = editor.document
    var line = tableDataRetriever.panelInfo.cellLinesIntervalSupplier?.invoke()?.endInclusive ?: dataViewVirtualFile?.cellLines?.last
    if (line == null) {
      val cellLines = NotebookCellLines.get(editor)
      line = cellLines.intervals.last().lines.last
    }
    val cellEditHelper = NotebookCellLinesEditHelper.getOrNull(editor) ?: return
    val generatedCell = cellEditHelper.makeCell(generatedCode.trim(), CellType.CODE) ?: return

    val lastCellLine = WriteCommandAction.writeCommandAction(project)
      .withName(commandName)
      .compute<Int, RuntimeException> {
        val cellLines = NotebookCellLines.get(document)
        val interval = if (cellLines.intervalsIterator(line).hasNext()) {
          cellLines.intervalsIterator(line).next()
        }
        else {
          // cellLinesIntervalSupplier can be outdated
          cellLines.intervals.last()
        }
        val prevCell = cellLines.intervals[interval.ordinal]
        val lastLine = prevCell.lines.last
        document.insertString(document.getLineEndOffset(lastLine), "\n$generatedCell")
        lastLine
      }

    val fileEditorManager = FileEditorManager.getInstance(project)
    fileEditorManager.openFile(editor.virtualFile!!, true)
    val caretModel = editor.caretModel
    caretModel.moveToOffset(document.getLineEndOffset(lastCellLine) + generatedCell.length + 1)
    editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
  }

  fun getEditor(): Editor? = tableDataRetriever.panelInfo.editor ?: dataViewVirtualFile?.originalEditor

  companion object {
    private val commandName
      get() = DataWranglerJupyterPyBundle.message("action.DataWrangler.Jupyter.Notebook.Export.text")
  }

  /**
   * Helper class for interacting with [variablePsiExpression]
   */
  private class TableVariable(val variablePsiExpression: OutputPsiExpression) {
    val variableName = variablePsiExpression.text

    /**
     *  It can be reference expressions or call expression.
     *  If it's a reference expression, we need to copy it so that we don't change the initial input variable table from the user.
     */
    fun asCopyExpression() = when (variablePsiExpression.elementType) {
      // what happens with getters? Can it be a problem?
      PyElementTypes.REFERENCE_EXPRESSION -> "${variablePsiExpression.text}.copy()"
      PyElementTypes.CALL_EXPRESSION -> variablePsiExpression.text
      else -> variablePsiExpression.text
    }

    fun getNameForCodePreview(): String = when (variablePsiExpression.elementType) {
      PyElementTypes.REFERENCE_EXPRESSION -> variablePsiExpression.text
      else -> DW_VARIABLE_NAME
    }
  }
}