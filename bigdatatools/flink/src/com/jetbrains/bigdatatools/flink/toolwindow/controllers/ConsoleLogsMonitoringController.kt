package com.jetbrains.bigdatatools.flink.toolwindow.controllers

import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Disposer
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.SideBorder
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanelWithEmptyText
import com.intellij.util.ui.StatusText
import com.intellij.util.ui.UIUtil
import com.jetbrains.bigdatatools.common.monitoring.data.listener.DataModelListener
import com.jetbrains.bigdatatools.common.monitoring.data.model.StringDataModel
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.ComponentController
import com.jetbrains.bigdatatools.common.util.ToolbarUtils
import com.jetbrains.bigdatatools.common.util.invokeLater
import com.jetbrains.bigdatatools.flink.data.FlinkDataManager
import com.jetbrains.bigdatatools.flink.util.FlinkMessagesBundle
import java.awt.BorderLayout
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.JPanel
import javax.swing.SwingConstants

abstract class ConsoleLogsMonitoringController(val project: Project,
                                               protected val dataManager: FlinkDataManager,
                                               private val isFormatted: Boolean = false) : ComponentController {
  private var state: PanelState = PanelState.NOT_LOAD

  private var dataModel: StringDataModel? = null
  private var textOfConsole = ""

  private val consoleView = ConsoleViewImpl(project, true).also {
    Disposer.register(this, it)
  }

  private val dataModelListener = object : DataModelListener {
    override fun onChanged() {
      if (state != PanelState.LOADED) {
        state = PanelState.LOADED
        setLoadedPanel()
      }

      val dataString = dataModel?.data ?: ""
      if (textOfConsole == dataString) {
        return
      }

      if (dataString.startsWith(textOfConsole)) {
        printWithFormatting(dataString.substring(textOfConsole.length))
      }
      else {
        consoleView.clear()
        printWithFormatting(dataString)
      }
      textOfConsole = dataString
    }
  }

  private val downloadFileAction = object : DumbAwareAction(FlinkMessagesBundle.message("flink.download.action"),
                                                            null,
                                                            AllIcons.Actions.Download) {
    override fun actionPerformed(e: AnActionEvent) {
      val descriptor = FileSaverDescriptor(FlinkMessagesBundle.message("flink.download.action.title"),
                                           FlinkMessagesBundle.message("flink.download.action.description"))
      val chooser = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
      val projectPath: String? = project.basePath
      val baseDir: Path? = if (projectPath != null) Paths.get(projectPath) else null
      val fileWrapper = chooser.save(baseDir, downloadFileName()) ?: return

      ApplicationManager.getApplication().runWriteAction {
        try {
          fileWrapper.file.bufferedWriter().use { out ->
            out.write(dataModel?.data.toString())
          }
        }
        catch (e: Exception) {
          invokeLater {
            Messages.showErrorDialog(project, e.message, FlinkMessagesBundle.message("text.download.action.failed"))
          }
        }
      }
    }

    override fun update(e: AnActionEvent) {
      e.presentation.isEnabledAndVisible = dataModel?.data != null
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
  }

  private val refreshAction = DumbAwareAction.create(FlinkMessagesBundle.message("flink.refresh.action.file"), AllIcons.Actions.Refresh) {
    refresh()
  }

  protected val panel = JPanel(BorderLayout())
  private val innerPanel = JPanel(BorderLayout())

  init {
    val toolbar = createToolbar().apply { targetComponent = panel }
    val toolbarComponent = toolbar.component.apply {
      border = IdeBorderFactory.createBorder(SideBorder.RIGHT)
    }
    toolbarComponent.let { panel.add(it, BorderLayout.WEST) }
    panel.add(innerPanel, BorderLayout.CENTER)
  }

  protected open val downloadFileName: () -> String = { "output.txt" }

  override fun dispose() {
    dataModel?.removeListener(dataModelListener)
  }

  fun setupBaseModel() {
    val newModel = getDataModel()
    newModel?.let { setupDataModel(it) }
  }

  override fun getComponent() = panel

  protected fun setupDataModel(newDataModel: StringDataModel) {
    dataModel?.removeListener(dataModelListener)
    newDataModel.addListener(dataModelListener)
    dataModel = newDataModel

    if (!newDataModel.isInitedByFirstTime) {
      state = PanelState.NOT_LOAD
      setNotLoadedPanel()
    }
    if (newDataModel.isLoading.get()) {
      state = PanelState.LOADING
      setLoadingPanel()
    }
    if (newDataModel.isInitedByFirstTime) {
      state = PanelState.LOADED
      setLoadedPanel()
      dataModelListener.onChanged()
    }

    panel.revalidate()
    panel.repaint()
  }

  private fun refresh() {
    setLoadingPanel()
    dataModel?.let { dataManager.updater.invokeRefreshModel(it) }
  }

  private fun addListener() {
    dataModel = getDataModel()
    dataModel?.addListener(dataModelListener)
  }

  private fun createToolbar(): ActionToolbar {
    val actions = DefaultActionGroup()
    actions.add(refreshAction)
    actions.add(downloadFileAction)
    actions.add(Separator.create())
    actions.addAll(getAdditionalActions())
    return ToolbarUtils.createActionToolbar("BDTConsoleLogsMonitoring", actions, false)
  }

  protected open fun getAdditionalActions(): List<AnAction> = emptyList()

  protected abstract fun getDataModel(): StringDataModel?

  private fun printWithFormatting(dataString: String) {
    if (!isFormatted) {
      consoleView.print(dataString, ConsoleViewContentType.NORMAL_OUTPUT)
    }
    else {
      val removedRedundantChars = dataString.substring(17)
      for (tokens in removedRedundantChars.split("},{")) {
        val formatted = tokens.replace("\\t", "\t").replace("\\n", "\n").replace("\\\"", "\"")
        val word = formatted.trimEnd('{', '}', '[', ']', '"').replace("\"\"", "\"")
        consoleView.print(word, ConsoleViewContentType.NORMAL_OUTPUT)
      }
    }
  }

  private fun setNotLoadedPanel() {
    state = PanelState.NOT_LOAD

    val emptyPanel = JBPanelWithEmptyText()

    emptyPanel.emptyText.apply {
      appendText(MessagesBundle.message("monitoring.log.not.loaded.text"), StatusText.DEFAULT_ATTRIBUTES)
      appendSecondaryText(MessagesBundle.message("monitoring.log.not.loaded.load"), SimpleTextAttributes.LINK_ATTRIBUTES) { refresh() }
      isShowAboveCenter = false
    }

    innerPanel.removeAll()
    innerPanel.add(emptyPanel, BorderLayout.CENTER)
  }

  private fun setLoadingPanel() {
    state = PanelState.LOADING
    val emptyText = JBLabel(MessagesBundle.message("monitoring.log.loading"), AnimatedIcon.Default.INSTANCE, SwingConstants.CENTER)

    emptyText.apply {
      fontColor = UIUtil.FontColor.BRIGHTER
    }
    innerPanel.removeAll()
    innerPanel.add(emptyText, BorderLayout.CENTER)
  }

  private fun setLoadedPanel() {
    state = PanelState.LOADED
    innerPanel.removeAll()
    innerPanel.add(consoleView.component, BorderLayout.CENTER)
  }

  enum class PanelState {
    NOT_LOAD, LOADING, LOADED
  }
}