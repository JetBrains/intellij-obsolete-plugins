package com.intellij.dataWrangler.jupyterPython.engine

import com.intellij.dataWrangler.jupyterPython.engine.console.DataWranglerLocalFile
import com.intellij.dataWrangler.jupyterPython.operations.pyStr
import com.intellij.database.datagrid.DataGrid
import com.intellij.database.datagrid.GridPanel.ViewPosition
import com.intellij.execution.console.LanguageConsoleView
import com.intellij.jupyter.core.editor.handlers.DataInputCodeGenerationContext
import com.intellij.jupyter.core.editor.handlers.TableDataFileDropHandler
import com.intellij.jupyter.core.editor.handlers.TableDataFileDropHandlerContext
import com.intellij.jupyter.core.editor.handlers.guessCsvSeparator
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.python.scientific.powerfuldataviewer.editor.DataViewFileEditor
import com.intellij.python.scientific.powerfuldataviewer.editor.DataViewVirtualFile
import com.intellij.scientific.tables.api.OutputPsiExpression
import com.intellij.util.SuspendingLazy
import com.intellij.util.suspendingLazy
import com.jetbrains.python.PyElementTypes
import com.jetbrains.python.PythonLanguage
import com.jetbrains.python.console.PydevConsoleCommunication
import com.jetbrains.python.console.PydevConsoleRunner
import com.jetbrains.python.console.PydevConsoleRunnerImpl
import com.jetbrains.python.console.PythonConsoleRunnerFactory
import com.jetbrains.python.console.PythonConsoleToolWindow
import com.jetbrains.python.debugger.values.DataFrameDebugValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

fun isSupportedDWFileExtension(extension: String): Boolean {
  return findApplicableDropHandler(extension) != null
}

private class ConsoleHolder(project: Project, coroutineScope: CoroutineScope) {
  private var runner: PydevConsoleRunner = PythonConsoleRunnerFactory.getInstance().createConsoleRunner(project, null)
  private var consoleCommunication: SuspendingLazy<PydevConsoleCommunication?> = coroutineScope.suspendingLazy {
    return@suspendingLazy waitForConsoleInitialization()
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  private suspend fun waitForConsoleInitialization(): PydevConsoleCommunication? =
    suspendCancellableCoroutine { continuation ->
      val listener = object: PydevConsoleRunnerImpl.ConsoleListenerEx {
        override fun handleConsoleInitialized(consoleView: LanguageConsoleView) {
          val comm = runner.consoleExecuteActionHandler.consoleCommunication
          runner.removeConsoleListener(this)
          continuation.resumeWith(Result.success(comm as? PydevConsoleCommunication))
        }

        override fun handleInitializationError(th: Throwable) {
          continuation.resumeWith(Result.failure(th))
        }
      }
      runner.addConsoleListener(listener)
      runner.runSync(false)
      continuation.invokeOnCancellation {
        runner.removeConsoleListener(listener)
      }
    }

  suspend fun getConsoleCommunication(): PydevConsoleCommunication? =
    consoleCommunication.getValue()

  fun getEditor(): Editor? = runner.consoleView.editor

  fun isValid(): Boolean {
    return !consoleCommunication.isInitialized() || consoleCommunication.getInitialized()?.isCommunicationClosed == false
  }
}

@Service(Service.Level.PROJECT)
internal class DataWranglerPythonConsoleManager(val project: Project, val coroutineScope: CoroutineScope) {
  @Volatile
  private var consoleHolder = ConsoleHolder(project, coroutineScope)

  internal suspend fun getPyFrameAccessor() = getConsoleCommunication()


  suspend fun createDataViewVirtualFile(tableDWName: String, code: String?): DataViewVirtualFile? {
    val console = try {
      getConsoleCommunication() ?: return null
    }
    catch (ce: CancellationException) {
      throw ce
    }
    catch (th: Throwable) {
      withContext(Dispatchers.EDT) {
        PythonConsoleToolWindow.getInstance(project).activate {}
      }
      throw CancellationException().apply { addSuppressed(th) }
    }
    if (!code.isNullOrBlank()) {
      console.execRaw(code)
    }
    val debugValue = console.evaluate(tableDWName, true, true) ?: return null
    if (debugValue.type != DataFrameDebugValue.pyDataFrameType) return null
    val file = DataViewVirtualFile(getConsoleHolder().getEditor(), null, debugValue.fullName, console, OutputPsiExpression(debugValue.name, PyElementTypes.REFERENCE_EXPRESSION))
    return file
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  suspend fun createAndWaitForTask(code: String) {
    if (code.isBlank()) return
    val console = getConsoleCommunication() ?: return
    console.execRaw(code)
  }

  private fun getConsoleHolder(): ConsoleHolder {
    val res = consoleHolder
    if (res.isValid()) {
      return res
    }
    synchronized(this) {
      if (res == consoleHolder) {
        consoleHolder = ConsoleHolder(project, coroutineScope)
      }
      return consoleHolder
    }
  }

  private suspend fun getConsoleCommunication(): PydevConsoleCommunication? =
    getConsoleHolder().getConsoleCommunication()
}

private val pythonKeywords = setOf("False", "None", "True", "and", "as", "assert",
                                   "async", "await", "break", "class", "continue", "def", "del",
                                   "elif", "else", "except", "finally", "for", "from", "global",
                                   "if", "import", "in", "is", "lambda", "nonlocal", "not", "or",
                                   "pass", "raise", "return", "try", "while", "with", "yield")

private val regex = Regex("[^a-zA-Z0-9_]")

private fun createPythonVariableName(tableFile: VirtualFile): String {
  val sanitized = tableFile.nameWithoutExtension.replace(regex, "_")
  val validStart = if (sanitized.firstOrNull()?.isDigit() == true) "_$sanitized" else sanitized
  val validName = if (validStart in pythonKeywords) "${validStart}_" else validStart
  return "__${validName}__"
}

internal fun getInitDataFrameCode(project: Project, dataFrameName: String, tableFile: VirtualFile): String {
  getDropHandlerInitCode(project, tableFile, dataFrameName)?.let {
    return it
  }
  val separator = guessCsvSeparator(tableFile.toNioPath()) ?: ','
  return getCSVInitCode(dataFrameName, separator.toString(), tableFile.path)
}

private fun getCSVInitCode(dataFrameName: String, separator: String, path: String): String {
  return "import pandas as pd\n" +
         "$dataFrameName = pd.read_csv('${path}', sep=${separator.pyStr})\n"
}

private fun getDropHandlerInitCode(project: Project, tableFile: VirtualFile, dataFrameName: String): String? {
  val fileExtension = tableFile.extension ?: ""
  val handler = findApplicableDropHandler(fileExtension)
  if (handler != null) {
    return handler.generateCellCode(TableDataFileDropHandlerContext(
      tableFile.path, fileExtension, project , DataInputCodeGenerationContext.FOR_EXECUTION, 0, dataFrameName
    ))
  }
  return null
}

private suspend fun openFileInTab(project: Project, file: VirtualFile): DataGrid? {
  val fileEditors = FileEditorManager.getInstance(project).openEditor(OpenFileDescriptor(project, file), true)
  val dataViewFileEditor = fileEditors.firstOrNull { it is DataViewFileEditor } as? DataViewFileEditor ?: return null
  val newGrid = dataViewFileEditor.dataViewerPanel.gridMutableStateFlow.first { it != null } ?: return null
  return newGrid
}

suspend fun openLocalFileInDataWranglerPyConsole(localFile: DataWranglerLocalFile, project: Project) {
  val consoleManager = project.service<DataWranglerPythonConsoleManager>()
  val tableDWName = createPythonVariableName(localFile.tableVirtualFile)
  val code = getInitDataFrameCode(project, tableDWName, localFile.tableVirtualFile)
  val file = consoleManager.createDataViewVirtualFile(tableDWName, code) ?: return
  withContext(Dispatchers.EDT) {
    val newGrid = openFileInTab(project, file) ?: return@withContext
    val engine = PythonDataWranglerEngine()
    val pyFrame = consoleManager.getPyFrameAccessor() ?: return@withContext
    val context = engine.createPythonConsoleContext(newGrid, pyFrame, localFile)
                  ?: return@withContext
    val dwPanel = getOrCreateTable(newGrid, engine, context)
    newGrid.panel.putSideView(dwPanel, ViewPosition.RIGHT, null)
  }
}

private fun findApplicableDropHandler(fileExtension: @NlsSafe String): TableDataFileDropHandler? =
  TableDataFileDropHandler.findApplicable(PythonLanguage.getInstance(), fileExtension)
