package com.jetbrains.hadoop.monitoring.toolwindow

import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.LoadingDecorator
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.ui.Splitter
import com.intellij.openapi.util.Disposer
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.TreeUIHelper
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.tree.TreeUtil
import com.jetbrains.bigdatatools.common.util.ToolbarUtils
import com.jetbrains.hadoop.monitoring.data.HadoopDataManager
import com.jetbrains.hadoop.monitoring.data.models.FsListListener
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.LogFileInfo
import com.jetbrains.hadoop.monitoring.settings.HadoopConnectionData
import com.jetbrains.hadoop.monitoring.settings.HadoopSettings
import com.jetbrains.hadoop.monitoring.ui.LogTreeCellRenderer
import com.jetbrains.hadoop.monitoring.ui.LogTreeNode
import com.jetbrains.hadoop.monitoring.ui.LogsTreeModel
import com.jetbrains.hadoop.monitoring.util.HadoopMessagesBundle
import org.jetbrains.concurrency.AsyncPromise
import javax.swing.tree.TreeSelectionModel

class Logs(project: Project, private val connectionData: HadoopConnectionData) : Disposable {

  private val diagnosticsConsole = ConsoleViewImpl(project, true)

  private val loadingDecorator = LoadingDecorator(diagnosticsConsole.component, this, 300)

  private val splitter = OnePixelSplitter(false, HadoopSettings.getInstance().getLogToContentProportion(connectionData.innerId))

  private val panel = SimpleToolWindowPanel(false, true)
  private val dataManager = HadoopDataManager.getInstance(connectionData.innerId, project) ?: error("Data Manager is not inited")
  private val dataModelFs = dataManager.getToolsLogs()
  private val logsTreeModel = LogsTreeModel(dataModelFs)
  private val logsTree = Tree()

  private var lastSelectedNode: LogTreeNode? = null

  init {
    Disposer.register(this, diagnosticsConsole)
    setConsoleEmptyText()

    panel.setContent(splitter)
    panel.toolbar = createDetailsToolBar().apply { targetComponent = panel }.component

    logsTree.cellRenderer = LogTreeCellRenderer()
    logsTree.isRootVisible = false
    logsTree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
    logsTree.selectionModel.addTreeSelectionListener { e ->
      lastSelectedNode = e.newLeadSelectionPath?.lastPathComponent as? LogTreeNode
      diagnosticsConsole.clear()
      contentLoadingPromise?.cancel()
      if (lastSelectedNode != null && lastSelectedNode!!.isLeaf) {
        lastSelectedNode!!.file?.let { showContent(it.path) }
      }
      else {
        loadingDecorator.stopLoading()
        setConsoleEmptyText()
      }
    }

    dataModelFs.addListListener(object : FsListListener {
      override fun onListUpdate(path: String, list: List<LogFileInfo>) {
        logsTree.emptyText.clear()
        logsTree.emptyText.appendText(HadoopMessagesBundle.message("tools.logs.empty"), SimpleTextAttributes.REGULAR_ATTRIBUTES)
      }

      override fun onError(msg: String, t: Throwable) {
        logsTree.emptyText.clear()
        logsTree.emptyText.appendText(MessagesBundle.message("table.loading.error", msg), SimpleTextAttributes.ERROR_ATTRIBUTES)
        logsTree.emptyText.appendSecondaryText(MessagesBundle.message("table.loading.error.secondary"),
                                               SimpleTextAttributes.GRAYED_ATTRIBUTES, null)
      }
    })

    TreeUIHelper.getInstance().installTreeSpeedSearch(logsTree)

    logsTree.model = AsyncTreeModel(logsTreeModel, true, this)

    val consolePanel = SimpleToolWindowPanel(false, true)
    consolePanel.toolbar = createConsoleToolBar().apply { targetComponent = consolePanel }.component
    consolePanel.setContent(loadingDecorator.component)

    splitter.firstComponent = JBScrollPane(logsTree)
    splitter.secondComponent = consolePanel

    splitter.addPropertyChangeListener(Splitter.PROP_PROPORTION) {
      HadoopSettings.getInstance().setLogToContentProportion(connectionData.innerId, splitter.proportion)
    }
  }

  private fun setConsoleEmptyText() {
    diagnosticsConsole.print(HadoopMessagesBundle.message("tools.logs.fileNotSelected"), ConsoleViewContentType.SYSTEM_OUTPUT)
  }

  private var contentLoadingPromise: AsyncPromise<String>? = null

  private fun showContent(path: String) {
    loadingDecorator.startLoading(false)
    contentLoadingPromise = dataModelFs.getContent(path)

    val capturedPromise = contentLoadingPromise!!
    contentLoadingPromise!!.onSuccess {
      loadingDecorator.stopLoading()
      if (it.isBlank()) {
        diagnosticsConsole.print(HadoopMessagesBundle.message("tools.logs.fileEmpty"), ConsoleViewContentType.SYSTEM_OUTPUT)
      }
      else {
        diagnosticsConsole.print(it, ConsoleViewContentType.NORMAL_OUTPUT)
      }
      scrollConsoleIfRequired()
      contentLoadingPromise = null
    }.onError {
      if (!capturedPromise.isCancelled) {
        loadingDecorator.stopLoading()
        diagnosticsConsole.print(HadoopMessagesBundle.message("tools.logs.fileError", it.message ?: ""),
                                 ConsoleViewContentType.ERROR_OUTPUT)
        scrollConsoleIfRequired()
        contentLoadingPromise = null
      }
    }
  }

  private fun scrollConsoleIfRequired() {
    if (!HadoopSettings.getInstance().getScrollLogToBottom(connectionData.innerId)) {
      diagnosticsConsole.scrollTo(0)
    }
  }

  private fun createConsoleToolBar(): ActionToolbar {
    val actions = DefaultActionGroup()

    val refreshAction = DumbAwareAction.create(HadoopMessagesBundle.message("tools.logs.refresh"), AllIcons.Actions.Refresh) {
        lastSelectedNode?.let {
          if (it.isLeaf && it.file != null) {
            logsTreeModel.resetContentCache(it)
            diagnosticsConsole.clear()
            showContent(it.file.path)
          }
        }
    }
    actions.add(refreshAction)

    val scrollToBottomAction = object : DumbAwareToggleAction(HadoopMessagesBundle.message("tools.logs.scrollToBottom.text"),
                                                              HadoopMessagesBundle.message("tools.logs.scrollToBottom.hint"),
                                                              AllIcons.RunConfigurations.Scroll_down) {
      override fun isSelected(e: AnActionEvent) = HadoopSettings.getInstance().getScrollLogToBottom(connectionData.innerId)
      override fun getActionUpdateThread() = ActionUpdateThread.BGT
      override fun setSelected(e: AnActionEvent, state: Boolean) {
        HadoopSettings.getInstance().setScrollLogToBottom(connectionData.innerId, state)
        if (state) {
          diagnosticsConsole.scrollToEnd()
        }
      }
    }
    actions.add(scrollToBottomAction)

    return ToolbarUtils.createActionToolbar("BDTHadoopLogsConsole", actions, false)
  }

  private fun createDetailsToolBar(): ActionToolbar {
    val refreshAction = DumbAwareAction.create(HadoopMessagesBundle.message("tools.logs.refresh"), AllIcons.Actions.Refresh) {
      logsTreeModel.refresh()
    }

    val expandAction = DumbAwareAction.create(HadoopMessagesBundle.message("tools.expandAll"), AllIcons.Actions.Expandall) {
      TreeUtil.expandAll(logsTree)
    }

    val collapseAction = DumbAwareAction.create(HadoopMessagesBundle.message("tools.collapseAll"), AllIcons.Actions.Collapseall) {
      TreeUtil.collapseAll(logsTree, 0)
    }

    val actions = DefaultActionGroup(refreshAction, Separator(), expandAction, collapseAction)

    return ToolbarUtils.createActionToolbar("BDTHadoopLogs", actions, false)
  }

  fun getComponent() = panel

  override fun dispose() {}
}