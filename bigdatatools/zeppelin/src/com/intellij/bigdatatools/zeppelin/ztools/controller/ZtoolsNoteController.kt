package com.intellij.bigdatatools.zeppelin.ztools.controller

import com.intellij.bigdatatools.coreUi.util.executeNotOnEdt
import com.intellij.bigdatatools.coreUi.util.executeOnPooledThread
import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellAdded
import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellChanged
import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellRemoved
import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellStatus
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookChangeListener
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookEvent
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookOutput
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookSchema
import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.bigdatatools.notebooks.core.impl.nbformat.OutputCode
import com.intellij.bigdatatools.zeppelin.ZeppelinProjectService
import com.intellij.bigdatatools.zeppelin.components.connections.ZeppelinConnectionListener
import com.intellij.bigdatatools.zeppelin.components.containers.service.ZeppelinNoteCacheConnection
import com.intellij.bigdatatools.zeppelin.drivers.ZeppelinDriverManager
import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.bigdatatools.zeppelin.idea.toolwindow.ZtoolsToolWindowUtils
import com.intellij.bigdatatools.zeppelin.models.interpreter.InterpreterSettings
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinCell
import com.intellij.bigdatatools.zeppelin.ztools.controller.model.ZtoolsDebugInfo
import com.intellij.bigdatatools.zeppelin.ztools.controller.model.ZtoolsDebugTask
import com.intellij.bigdatatools.zeppelin.ztools.inlays.ZtoolsInlaysService
import com.intellij.bigdatatools.zeppelin.ztools.settings.ZtoolsConfig
import com.intellij.bigdatatools.zeppelin.ztools.settings.ZtoolsDialogBuilder
import com.intellij.bigdatatools.zeppelin.ztools.variableview.VariableViewManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.jetbrains.bigdatatools.common.util.invokeLater
import java.util.Date

/**
 * Component, which is responsible for handling of service Intellij data from Zeppelin
 */
class ZtoolsNoteController(val zeppelinEditor: ZeppelinEditor, val cacheConnection: ZeppelinNoteCacheConnection) : Disposable {
  private val project = zeppelinEditor.editor.project ?: throw Exception("Project is not found")
  val file = zeppelinEditor.file
  private val notebook = zeppelinEditor.note
  private val config = cacheConnection.config

  internal var debugInfo: ZtoolsDebugInfo? = ZtoolsDebugInfo()

  var ztoolsConf: ZtoolsConfig?
    get() = ZeppelinDriverManager.getDriver(project, config.innerId)?.connectionData?.ztoolsConf
    private set(value) {
      ZeppelinDriverManager.getDriver(project, config.innerId)!!.connectionData.ztoolsConf = value!!
    }

  private val debugTaskManager = DebugCellManager(zeppelinEditor, project, file, config.innerId)

  private val notifier = ZtoolsNoteNotifier(project, cacheConnection, zeppelinEditor.note.name)

  private val noteChangeListener: NotebookChangeListener = object : NotebookChangeListener {
    override fun onEvent(notebookEvent: NotebookEvent) {
      when (notebookEvent) {
        is CellAdded -> {
          //handle adding debugCell here if the added cell is a debug cell
          debugTaskManager.runDebugCellIfDebugCellCreated(notebookEvent.cell)
        }

        is CellRemoved -> {
          //handle removing debugCell here if the removed cell is a debug cell
          debugTaskManager.onCellDelete(notebookEvent.cell)
        }

        is CellChanged -> {
          debugTaskManager.runDebugCellIfDebugCellCreated(notebookEvent.cell)

          val changedCell = notebookEvent.cell as ZeppelinCell
          val changedFields = notebookEvent.changedFields

          if (!changedFields.contains(NotebookSchema.cellStatus))
            return
          if (changedCell.isLaunched || changedCell.status == CellStatus.READY)
            return

          processZtoolsOutput(changedCell, changedFields)
          processNormalCellOutput(changedCell, changedFields)
        }

        else -> Unit
      }
    }
  }

  private val cacheConnectionListener = object : ZeppelinConnectionListener {
    override fun onDisconnected(statusCode: Int?, reason: String?) = debugTaskManager.clear()
  }

  init {
    Disposer.register(this, ZtoolsInlaysService(zeppelinEditor))
    Disposer.register(this, notifier)
    file.putUserData(ZTOOLS_KEY, this)

    cacheConnection.addListener(cacheConnectionListener)
    notebook.addNotebookChangeListener(noteChangeListener)

    //Init project service
    ZeppelinProjectService.getInstance(project)

    try {
      val variableView = VariableViewManager.getInstance(project).initVariableView(file, this)
      ZtoolsToolWindowUtils.setVariableView(project, variableView)
    }
    catch (t: Throwable) {
      logger.error(t)
    }
  }

  override fun dispose() {
    file.putUserData(ZTOOLS_KEY, null)
    cacheConnection.removeListener(cacheConnectionListener)
    notebook.removeNotebookChangeListener(noteChangeListener)
  }

  private fun handleZtoolsResult(result: NotebookOutput, interpreterCode: String) = executeOnPooledThread {

    VariableViewManager.getInstance(project).clearErrors(file, interpreterCode)
    VariableViewManager.getInstance(project).refreshUpdateInfo(file)

    val serviceOutput = result.msg.firstOrNull() ?: return@executeOnPooledThread
    try {
      ZtoolsService.handleZtoolsOutput(project, file, config, serviceOutput.data, interpreterCode, zeppelinEditor)
    }
    catch (e: Exception) {
      logger.error(e)
    }
  }

  private fun processNormalCellOutput(changedCell: ZeppelinCell, changedFields: Set<String>) {
    if (!isSupportedNormalCellFinished(changedCell, changedFields))
      return
    val cellInterpreter = getSupportedInterpreterByMarker(changedCell) ?: return
    executeNotOnEdt {
      file.putUserData(ZTOOLS_LAST_UPDATE_SOURCE, changedCell)
      val debugCellTask = DebugCellManager.createDebugTask(cellInterpreter, changedCell.interpreterCode, false, false)
                          ?: error("failed to create debug cell!")
      debugTaskManager.addTask(debugCellTask)
    }
  }

  private fun processZtoolsOutput(changedCell: ZeppelinCell, changedFields: Set<String>) {
    if (!debugTaskManager.isDebugCellCompleted(changedCell, changedFields))
      return

    invokeLater {
      debugTaskManager.deleteCompletedCell()
    }

    processZtoolsOutput(changedCell)
  }


  private fun isSupportedNormalCellFinished(changedCell: ZeppelinCell, changedFields: Set<String>): Boolean =
    !changedCell.isDebugCell &&
    changedFields.contains(NotebookSchema.cellStatus) &&
    !changedCell.isLaunched && changedCell.status != CellStatus.READY

  private fun getSupportedInterpreterByMarker(cell: ZeppelinCell): InterpreterSettings? {
    val interpreterCode = cell.interpreterCode

    val supported = cacheConnection.interpreterSettings
    val defaultInterpreter = cacheConnection.defaultBindingInterpreterSettings ?: return null
    val isDefaultInterpreterSupported = supported.any { it.name == defaultInterpreter.name }

    if (isDefaultInterpreterSupported && interpreterCode.isEmpty()) {
      return defaultInterpreter
    }
    val defaultInterpreterGroupNames = getInterpreterGroupNames(defaultInterpreter)

    if (defaultInterpreterGroupNames.contains(interpreterCode))
      return defaultInterpreter

    return supported.firstOrNull { interpreter ->
      val groupNames = getInterpreterGroupNames(interpreter)
      val interpreterName = interpreter.name
      val allSupportedNames = if (groupNames.size > 1) {
        listOf(interpreterName) + groupNames.map { "$interpreterName.$it" }
      }
      else {
        listOf(interpreterName)
      }
      allSupportedNames.contains(interpreterCode)
    }
  }

  private fun getInterpreterGroupNames(interpreterSettings: InterpreterSettings) = interpreterSettings.interpreterGroup.map { it.name }

  fun showSettings() {
    val newResult = ztoolsConf?.let { ZtoolsDialogBuilder.showAndGet(project, it) } ?: return
    ztoolsConf = newResult
  }

  fun refresh() = executeNotOnEdt {
    val debugCells = notebook.cells.mapNotNull {
      val interpreterSettings = getSupportedInterpreterByMarker(it) ?: return@mapNotNull null
      DebugCellManager.createDebugTask(interpreterSettings, it.interpreterCode, true, true)
    }.distinct().filter { ZtoolsService.isZtoolsCellSupported(it) }

    val tasks: List<ZtoolsDebugTask> = debugCells
      .groupBy { it.interpreterName }
      .filter { it.value.isNotEmpty() }
      .flatMap { it.value.drop(1) + it.value.first().copy(forceIgnoreSql = false) }

    file.putUserData(ZTOOLS_LAST_UPDATE_SOURCE, null)
    debugTaskManager.addTasks(tasks)
  }

  private fun processZtoolsOutput(changedCell: ZeppelinCell) {
    val result = changedCell.output
    debugInfo?.totalOutput = mapOf(
      "output" to changedCell.output?.msg,
      "time" to (if (changedCell.dateStarted != null && changedCell.dateFinished != null)
        changedCell.dateFinished!!.time - changedCell.dateStarted!!.time
      else
        null))

    when (result?.code) {
      OutputCode.SUCCESS -> {
        file.putUserData(ZTOOLS_LAST_UPDATE_DATE, Date())
        VariableViewManager.getInstance(project).addErrors(file, changedCell.interpreterCode, null)
        handleZtoolsResult(result, changedCell.interpreterCode)
      }
      OutputCode.ERROR -> {
        VariableViewManager.getInstance(project).addErrors(file, changedCell.interpreterCode, result.msg.map { it.data })
        notifier.showZtoolsErrorNotification(result.msg)
      }
      else -> Unit
    }
  }


  companion object {
    private val logger = Logger.getInstance(this::class.java)
    const val DEBUG_CELL_MARKER = "IS_INTELLIJ_SERVICE"
    const val DEBUG_CELL_ID = "ZTOOLS_DEBUG_CELL_ID"
    private val ZTOOLS_KEY = Key<ZtoolsNoteController>("ZTOOLS_NOTE")

    val ZTOOLS_LAST_UPDATE_DATE = Key<Date>("ZTOOLS_UPDATE_DATE")
    val ZTOOLS_LAST_UPDATE_SOURCE = Key<ZeppelinCell?>("ZTOOLS_UPDATE_SOURCE")

    fun getFor(file: NotebookVirtualFile) = file.getUserData(ZTOOLS_KEY)


    val ZeppelinCell.isDebugCell: Boolean
      get() = getMetadata(DEBUG_CELL_MARKER)?.asBoolean ?: false
  }
}