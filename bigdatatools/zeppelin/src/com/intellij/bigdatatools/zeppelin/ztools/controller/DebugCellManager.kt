package com.intellij.bigdatatools.zeppelin.ztools.controller

import com.google.gson.JsonPrimitive
import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellStatus
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookSchema
import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.bigdatatools.zeppelin.drivers.ZeppelinDriverManager
import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.bigdatatools.zeppelin.models.interpreter.InterpreterSettings
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinCell
import com.intellij.bigdatatools.zeppelin.ztools.controller.model.ZtoolsDebugTask
import com.intellij.bigdatatools.zeppelin.ztools.settings.ZtoolsConfig
import com.intellij.openapi.project.Project
import com.intellij.bigdatatools.coreUi.util.executeOnPooledThread
import com.jetbrains.bigdatatools.common.util.invokeLater
import java.util.concurrent.atomic.AtomicReference

class DebugCellManager(private val zeppelinEditor: ZeppelinEditor,
                       private val project: Project,
                       private val file: NotebookVirtualFile,
                       private val configId: String) {
  private val debugCellTasks: MutableList<ZtoolsDebugTask> = mutableListOf()
  private val currentTask = AtomicReference<ZtoolsDebugTask?>(null)

  @Volatile
  private var debugCellZtoolsId: String? = null

  @Volatile
  private var debugCell: NotebookCell? = null

  private var config: ZtoolsConfig
    get() = ZeppelinDriverManager.getDriver(project, configId)!!.connectionData.ztoolsConf
    set(value) {
      ZeppelinDriverManager.getDriver(project, configId)!!.connectionData.ztoolsConf = value
    }

  fun runDebugCellIfDebugCellCreated(cell: NotebookCell) {
    if (!isDebugCell(cell))
      return

    if (cell.status == CellStatus.READY) {
      debugCell = cell
      runDebugCell(cell)
    }
  }

  fun onCellDelete(cell: NotebookCell) {
    if (!isDebugCell(cell))
      return

    debugCell = null
    debugCellZtoolsId = null
    currentTask.set(null)

    executeOnPooledThread {
      tryToProcessNextTask()
    }
  }

  fun deleteCompletedCell() {
    val currentDebugCell = debugCell
    if (currentDebugCell == null)
      error("Debug cell should not be null when cell execution is in process.")
    invokeLater {
      zeppelinEditor.actionNotify { it.deleteCell(currentDebugCell) }
    }
  }

  private fun runDebugCell(cell: NotebookCell) = zeppelinEditor.actionNotify {
    it.runCell(cell)
  }

  fun clear() {
    synchronized(debugCellTasks) {
      debugCellTasks.clear()
    }
    currentTask.set(null)
  }

  fun addTask(task: ZtoolsDebugTask) {
    if (!ZtoolsService.isZtoolsCellSupported(task)) {
      return
    }
    if (currentTask.get() != task) {
      synchronized(debugCellTasks) {
        if (!debugCellTasks.contains(task)) debugCellTasks.add(task)
      }
    }
    executeOnPooledThread {
      tryToProcessNextTask()
    }
  }

  fun addTasks(tasks: List<ZtoolsDebugTask>) {
    synchronized(debugCellTasks) {
      tasks
        .filter { ZtoolsService.isZtoolsCellSupported(it) }
        .forEach { task ->
          if (currentTask.get() == task || debugCellTasks.contains(task)) {
            return@synchronized
          }
          else
            debugCellTasks.add(task)
        }
    }
    executeOnPooledThread {
      tryToProcessNextTask()
    }
  }

  fun isDebugCellCompleted(changedCell: ZeppelinCell, changedFields: Set<String>): Boolean =
    isDebugCell(changedCell) &&
    changedFields.contains(NotebookSchema.cellStatus) &&
    (!changedCell.isLaunched && changedCell.status != CellStatus.READY)


  private fun isDebugCell(cell: NotebookCell): Boolean {
    val debugId = cell.getMetadata(ZtoolsNoteController.DEBUG_CELL_ID)?.asString ?: return false
    val curDebugCellId = debugCellZtoolsId

    return curDebugCellId != null && debugId == curDebugCellId
  }


  private fun tryToProcessNextTask(): Unit = synchronized(debugCellTasks) {
    debugCellTasks.ifEmpty { return@synchronized }

    val nextTask = debugCellTasks.removeAt(debugCellTasks.size - 1)
    if (currentTask.compareAndSet(null, nextTask)) {
      val isNotEmpty = createZtoolsNoteCell(nextTask)
      if (!isNotEmpty) {
        currentTask.set(null)
        tryToProcessNextTask()
      }
    }
    else
      debugCellTasks.add(0, nextTask)
  }

  private fun createZtoolsNoteCell(testTask: ZtoolsDebugTask): Boolean {
    val cellText = ZtoolsCellHelper.getDebugCellText(config, testTask, project, file)
    if (cellText.isBlank())
      return false

    debugCellZtoolsId = testTask.debugId


    val metadata = mapOf(
      ZtoolsNoteController.DEBUG_CELL_MARKER to JsonPrimitive(true),
      ZtoolsNoteController.DEBUG_CELL_ID to JsonPrimitive(testTask.debugId),
    )

    zeppelinEditor.actionNotify {
      it.addCell(0, cellText, true, true, metadata, false)
    }

    return true
  }

  companion object {
    fun createDebugTask(interpreterSettings: InterpreterSettings,
                        marker: String,
                        onDemand: Boolean,
                        forceIgnoreSql: Boolean): ZtoolsDebugTask? {
      val interpreter = if (marker.isEmpty())
        interpreterSettings.interpreterGroup.find { it.defaultInterpreter }
      else
        interpreterSettings.interpreterGroup.find { it.name == marker }
        ?: interpreterSettings.interpreterGroup.find { "${interpreterSettings.name}.${it.name}" == marker }
      val language = interpreter?.editor?.get("language")?.toString() ?: return null

      return ZtoolsDebugTask(interpreterGroup = interpreterSettings.group,
                             interpreterName = interpreterSettings.name,
                             onDemand = onDemand,
                             language = language,
                             subInterpreter = interpreter.name,
                             forceIgnoreSql = forceIgnoreSql)
    }
  }
}

