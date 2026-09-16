package com.intellij.bigdatatools.databricks.toolwindow.controllers.workflow

import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.intellij.bigdatatools.databricks.client.DatabricksDataManager
import com.intellij.bigdatatools.databricks.model.WorkflowRunInfo
import com.intellij.bigdatatools.databricks.toolwindow.config.DatabricksToolWindowSettings
import com.intellij.bigdatatools.databricks.util.DatabricksBundle
import com.intellij.bigdatatools.databricks.util.DbIcons
import com.intellij.bigdatatools.databricks.util.labeledSelectableLabel
import com.intellij.bigdatatools.databricks.util.noGap
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.DataSink
import com.intellij.openapi.actionSystem.UiDataProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.PopupHandler
import com.intellij.ui.SideBorder
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.bigdatatools.common.monitoring.data.listener.DataModelListener
import com.jetbrains.bigdatatools.common.monitoring.data.model.DataModelError
import com.jetbrains.bigdatatools.common.monitoring.data.model.FieldsGroupModel
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.ComponentController
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.DetailsMonitoringController
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.ErrorPanel
import com.jetbrains.bigdatatools.common.rfs.driver.metainfo.components.SelectableLabel
import com.jetbrains.bigdatatools.common.settings.ConnectionSettings
import com.jetbrains.bigdatatools.common.ui.setCenterComponent
import com.jetbrains.bigdatatools.common.util.TimeUtils
import com.jetbrains.bigdatatools.common.util.ToolbarUtils
import java.awt.BorderLayout
import java.net.URI
import java.nio.charset.Charset
import javax.swing.JPanel
import javax.swing.SwingConstants

internal open class WorkflowDetailsController(val project: Project,
                                     private val dataManager: DatabricksDataManager) : ComponentController, DetailsMonitoringController<Long> {

  companion object {
    val WORKFLOW_DETAILS_CONTROLLER_KEY = DataKey.create<WorkflowDetailsController>("WorkflowDetailsController")
    val WORKFLOW_DETAILS_BROWSER_KEY = DataKey.create<JBCefBrowser>("WorkflowDetailsBrowser")
  }

  private var id: Long? = null
  private var dataModel: FieldsGroupModel<WorkflowRunInfo>? = null

  private val cefView by lazy {
    JBCefBrowser().also {
      Disposer.register(this@WorkflowDetailsController, it)

      //val actionManager = ActionManager.getInstance()
      //for (action in JcefShortcutProvider.getActions()) {
      //  action.second.registerCustomShortcutSet(actionManager.getAction(action.first!!).shortcutSet, it.component, it)
      //}

      PopupHandler.installPopupMenu(it.component, ActionManager.getInstance().getAction("Databricks.WorkflowToolbar") as ActionGroup,
                                    "WorkflowResultBrowser")
    }
  }

  internal enum class ContentType {
    CONSOLE,
    HTML
  }

  internal var contentType = ContentType.CONSOLE
    private set

  internal var content: String = ""
    private set

  // We are getting title from HTML result.
  internal var title: String = ""
    private set

  private val consoleView by lazy {
    ConsoleViewImpl(project, true).also {
      Disposer.register(this, it)
    }
  }

  private val panel = object : JPanel(BorderLayout()), UiDataProvider {
    override fun uiDataSnapshot(sink: DataSink) {
      sink[WORKFLOW_DETAILS_CONTROLLER_KEY] = this@WorkflowDetailsController
      if(contentType == ContentType.HTML) {
        sink[WORKFLOW_DETAILS_BROWSER_KEY] = cefView
      }
    }
  }
  private val innerPanel = OnePixelSplitter(false, "databricks.workflow.results.splitter.proportions", 0.7f)

  private val dataModelListener = object : DataModelListener {
    override fun onChanged() = updatePanel()
    override fun onError(msg: String, e: Throwable?) = updatePanel()
  }

  init {
    val toolbar = createToolbar()
    val toolbarComponent = toolbar.component.apply {
      border = IdeBorderFactory.createBorder(SideBorder.RIGHT)
    }
    toolbarComponent.let { panel.add(it, BorderLayout.WEST) }
    panel.add(innerPanel, BorderLayout.CENTER)
  }

  override fun dispose() {
    dataModel?.removeListener(dataModelListener)
  }

  private fun setInfoPanel(info: WorkflowRunInfo) {
    innerPanel.secondComponent = JBScrollPane(panel {
      @Suppress("HardCodedStringLiteral")
      group(DatabricksBundle.message("run.info.details")) {
        row(DatabricksBundle.message("run.info.job.id")) {
          if (info.info.runPageUrl != null) {
            browserLink(info.info.jobId.toString(), info.info.runPageUrl)
          }
          else {
            cell(SelectableLabel(info.info.jobId.toString()))
          }
        }.noGap()

        row(DatabricksBundle.message("run.info.status")) {
          @Suppress("HardCodedStringLiteral")
          label(info.info.state.resultState?.name ?: info.info.state.lifeCycleState?.name
                ?: DatabricksBundle.message("run.info.status.unknown")).apply {
            component.icon = info.info.state.resultState?.let { DbIcons.getForRunResultState(it) }
                             ?: info.info.state.lifeCycleState?.let { DbIcons.getForRunLifeCycleState(it) }
          }
        }.noGap()

        //labeledSelectableLabel(DatabricksBundle.message("run.info.job.run.id"), info.runId.toString())
        //labeledSelectableLabel(DatabricksBundle.message("run.info.task.run.id"), info.info.tasks.joinToString { it.runId.toString() })
        labeledSelectableLabel(DatabricksBundle.message("run.info.run.as"), info.info.creatorUserName)
        labeledSelectableLabel(DatabricksBundle.message("run.info.start.time"), info.formatedStartTime)

        val duration = info.duration ?: (info.endTime - info.startTime)
        labeledSelectableLabel(DatabricksBundle.message("run.info.end.time"), TimeUtils.unixTimeToString(info.startTime + duration))
        labeledSelectableLabel(DatabricksBundle.message("run.info.duration"), TimeUtils.intervalAsString(duration))

        if (info.info.queueDuration != null) {
          labeledSelectableLabel(DatabricksBundle.message("run.info.queue.duration"), TimeUtils.unixTimeToString(info.info.queueDuration))
        }

        val notebookLink = info.info.tasks.firstOrNull()?.notebookTask?.notebookPath
        if (notebookLink != null) {
          row {
            val urlWithParameters = URI(info.info.runPageUrl)
            val urlWithoutParameters = URI(urlWithParameters.scheme,
                                           urlWithParameters.getAuthority(),
                                           urlWithParameters.getPath(),
                                           null,
                                           null).toString()
            browserLink(notebookLink, "$urlWithoutParameters#workspace$notebookLink")
          }.noGap()
        }
      }
    }.withBorder(JBUI.Borders.emptyLeft(5)))
  }

  override fun getComponent(): JPanel = panel

  override fun setDetailsId(id: Long) {
    if (this.id == id) {
      return
    }

    this.id = id
    dataModel?.removeListener(dataModelListener)

    val fieldsGroupModel = dataManager.getWorkflowRunModel(id)
    fieldsGroupModel.addListener(dataModelListener)
    dataModel = fieldsGroupModel
    updatePanel()
  }


  private fun createToolbar(): ActionToolbar {
    return ToolbarUtils.createActionToolbar(panel, "DatabricksWorkflowDetails",
                                            ActionManager.getInstance().getAction("Databricks.WorkflowToolbar") as ActionGroup,
                                            false)
  }

  protected fun updatePanel() {
    val dataModel = dataModel
    if (dataModel == null) {
      innerPanel.removeAll()
      return
    }
    val dataModelError = dataModel.error
    if (dataModelError != null) {
      setErrorPanel(dataModelError)
      return
    }
    val data = dataModel.data
    if (data != null) {
      val runInfo = data.obj
      setLoadedPanel(runInfo)
      if (runInfo != null) {
        setInfoPanel(runInfo)
      }
      else {
        innerPanel.secondComponent = null
      }
    }
    else {
      setLoadingPanel()
    }
    panel.revalidate()
    panel.repaint()
  }

  private fun setLoadedPanel(runInfo: WorkflowRunInfo?) {
    when {
      runInfo?.isRunning == true -> setTaskExecutingPanel()
      runInfo?.exportResult != null -> {
        if (innerPanel.firstComponent != cefView.component) {
          innerPanel.firstComponent = cefView.component
        }
        content = runInfo.exportResult.views.firstOrNull()?.content ?: ""

        // Extract title
        val titleRegexpGroups = Regex("<title>(.*?)</title>").find(content)?.groups
        if (titleRegexpGroups != null && titleRegexpGroups.size > 1) {
          val wholeTitle = titleRegexpGroups[1]?.value ?: ""
          val indexOfFirstPoint = wholeTitle.indexOf('.')
          title = wholeTitle.substring(0, if (indexOfFirstPoint == -1) wholeTitle.length else indexOfFirstPoint)
        }

        fun getResourceAsString(fileName: String): String {
          return this::class.java.getResourceAsStream(fileName)!!.bufferedReader(Charset.forName("UTF-8")).readText()
        }

        val additionalScripts = StringBuilder("<script>\n").apply {
          append(getResourceAsString("/scripts/intelliJDatabricksTools.js")).append('\n')
          append(getResourceAsString("/scripts/hideTitleOnLoad.js")).append('\n')
          append(getResourceAsString("/scripts/hideToolbarOnLoad.js")).append('\n')
          append("</script>")
        }

        if (!DatabricksToolWindowSettings.getInstance().showCodeInResults) {
          additionalScripts.append("<script>").append(getResourceAsString("/scripts/hideCodeOnLoad.js")).append("</script>")
        }
        if (!DatabricksToolWindowSettings.getInstance().showAutogeneratedCellInResults) {
          additionalScripts.append("<script>").append(getResourceAsString("/scripts/hideGeneratedCellOnLoad.js")).append("</script>")
        }
        contentType = ContentType.HTML
        content = content.replace("</body>\n</html>", "$additionalScripts</body>\n</html>")
        cefView.loadHTML(content)
      }
      runInfo?.runOutput != null -> {
        if (innerPanel.firstComponent != consoleView.component) {
          innerPanel.firstComponent = consoleView.component
        }
        contentType = ContentType.CONSOLE
        content = runInfo.runOutput.logs?.takeIf { it.isNotBlank() } ?: runInfo.runOutput.error?.takeIf { it.isNotBlank() } ?: ""
        printToConsole(content)
      }
    }
  }

  private fun setErrorPanel(it: DataModelError) {
    innerPanel.firstComponent = ErrorPanel(it.cause) {
      ConnectionSettings.open(project, dataManager.connectionId)
    }
    innerPanel.secondComponent = null
  }

  private fun printToConsole(dataString: String) {
    consoleView.clear()
    consoleView.print(dataString, ConsoleViewContentType.NORMAL_OUTPUT)
  }

  private fun setLoadingPanel() {
    val emptyText = JBLabel(MessagesBundle.message("monitoring.log.loading"), AnimatedIcon.Default.INSTANCE, SwingConstants.CENTER).apply {
      fontColor = UIUtil.FontColor.BRIGHTER
    }
    innerPanel.setCenterComponent(emptyText)
    innerPanel.secondComponent = null
  }

  private fun setTaskExecutingPanel() {
    val emptyText = JBLabel(DatabricksBundle.message("monitoring.task.is.executing"), AnimatedIcon.Default.INSTANCE, SwingConstants.CENTER).apply {
      fontColor = UIUtil.FontColor.BRIGHTER
    }
    innerPanel.firstComponent = emptyText
  }
}