package com.intellij.bigdatatools.databricks.toolwindow.controllers.server

import com.databricks.sdk.service.compute.ResultType
import com.databricks.sdk.service.compute.Results
import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.intellij.bigdatatools.databricks.client.DatabricksDataManager
import com.intellij.bigdatatools.databricks.model.ServerRunInfo
import com.intellij.bigdatatools.databricks.model.isRunning
import com.intellij.bigdatatools.databricks.toolwindow.ImagePanel
import com.intellij.bigdatatools.databricks.util.DatabricksBundle
import com.intellij.bigdatatools.databricks.util.DbIcons
import com.intellij.bigdatatools.databricks.util.labeledSelectableLabel
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.SideBorder
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.bigdatatools.common.monitoring.data.listener.DataModelListener
import com.jetbrains.bigdatatools.common.monitoring.data.model.DataModelError
import com.jetbrains.bigdatatools.common.monitoring.data.model.FieldsGroupModel
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.ComponentController
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.DetailsMonitoringController
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.ErrorPanel
import com.jetbrains.bigdatatools.common.settings.ConnectionSettings
import com.jetbrains.bigdatatools.common.util.TimeUtils
import com.jetbrains.bigdatatools.common.util.ToolbarUtils
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.util.Base64
import javax.imageio.ImageIO
import javax.swing.BoxLayout
import javax.swing.ImageIcon
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import kotlin.math.max

internal class ServerRunDetailsController(val project: Project,
                                 private val dataManager: DatabricksDataManager) : ComponentController, DetailsMonitoringController<String> {
  private var id: String? = null
  private var dataModel: FieldsGroupModel<ServerRunInfo>? = null

  //private val cefView by lazy {
  //  JBCefBrowser().also {
  //    Disposer.register(this, it)
  //  }
  //}

  private val consoleView by lazy {
    object : ConsoleViewImpl(project, true), AnsiEscapeDecoder.ColoredTextAcceptor {
      override fun coloredTextAvailable(text: String, attributes: Key<*>) {
        print(text, ConsoleViewContentType.getConsoleViewType(attributes))
      }
    }.also {
      Disposer.register(this, it)
    }
  }

  private val panel = JPanel(BorderLayout())
  private val innerPanel = OnePixelSplitter(false, "databricks.run.results.splitter.proportions", 0.7f)

  private val dataModelListener = object : DataModelListener {
    override fun onChanged() = updatePanel()
    override fun onError(msg: String, e: Throwable?) = updatePanel()
  }

  init {
    val toolbar = createToolbar().apply { targetComponent = panel }
    val toolbarComponent = toolbar.component.apply {
      border = IdeBorderFactory.createBorder(SideBorder.RIGHT)
    }
    toolbarComponent.let { panel.add(it, BorderLayout.WEST) }
    panel.add(innerPanel, BorderLayout.CENTER)
  }

  override fun dispose() {
    dataModel?.removeListener(dataModelListener)
  }

  override fun getComponent() = panel

  override fun setDetailsId(id: String) {
    if (this.id == id) {
      return
    }

    this.id = id
    dataModel?.removeListener(dataModelListener)

    val fieldsGroupModel = dataManager.getServerRunModel(id)
    fieldsGroupModel.addListener(dataModelListener)
    dataModel = fieldsGroupModel
    updatePanel()
  }

  private fun createToolbar(): ActionToolbar {
    val actions = DefaultActionGroup()
    actions.add(Separator.create())
    return ToolbarUtils.createActionToolbar("BDTConsoleLogsMonitoring", actions, false)
  }

  private fun updatePanel() {
    if (dataModel == null) {
      innerPanel.removeAll()
      return
    }
    val dataModelError = dataModel?.error
    if (dataModelError != null) {
      setErrorPanel(dataModelError)
      return
    }
    val data = dataModel?.data?.obj
    if (data != null) {
      setPanel(data)
    }
    else {
      setLoadingPanel()
    }
    panel.revalidate()
    panel.repaint()
  }

  private fun setPanel(info: ServerRunInfo) {
    val result = info.response?.results
    when {
      result != null -> {
        setResultPanel(result)
        setInfoPanel(info)
      }
      info.status.isRunning -> {
        setTaskExecutingPanel()
      }
    }
  }

  private fun setResultPanel(result: Results) {
    try {
      when (result.resultType) {
        ResultType.ERROR -> setConsolePanel(result.cause, isError = true)
        ResultType.IMAGE -> setImagePanel(result.fileName)
        ResultType.IMAGES -> setImagesPanel(result.fileNames)
        ResultType.TABLE -> setConsolePanel(result.data.toString(), isError = false)
        ResultType.TEXT -> setConsolePanel(result.data.toString(), isError = false)
        null -> setConsolePanel("Result type is null", isError = true)
      }
    }
    catch (e: Throwable) {
      setConsolePanel(e.stackTraceToString(), isError = true)
    }
  }

  private fun setInfoPanel(info: ServerRunInfo) {
    innerPanel.secondComponent = panel {
      @Suppress("HardCodedStringLiteral")
      group(DatabricksBundle.message("run.info.details")) {
        row(DatabricksBundle.message("run.info.status")) {
          label(info.status.name).apply {
            component.icon = DbIcons.getForCommandStatus(info.status)
          }
        }
        labeledSelectableLabel(DatabricksBundle.message("run.info.context.id"), info.contextId)
        labeledSelectableLabel(DatabricksBundle.message("run.info.start.time"), info.formatedStartTime)
        labeledSelectableLabel(DatabricksBundle.message("run.info.end.time"), TimeUtils.unixTimeToString(info.endTime))
        labeledSelectableLabel(DatabricksBundle.message("run.info.duration"), TimeUtils.intervalAsString(info.endTime - info.startTime))
      }
    }.withBorder(JBUI.Borders.emptyLeft(5))
  }

  /** https://docs.databricks.com/api/workspace/commandexecution/commandstatus#results-fileNames */
  private fun setImagesPanel(data: Collection<String>) {
    val panel = JPanel(null).apply {
      setLayout(BoxLayout(this, BoxLayout.Y_AXIS))
    }

    data.forEach {
      val image = extractImageFromString(it)
      panel.add(JLabel(ImageIcon(image)).apply {
        minimumSize = Dimension(1, 1)
        maximumSize = Dimension(Int.MAX_VALUE, Int.MAX_VALUE)
      })
    }

    innerPanel.firstComponent = JBScrollPane(panel)
  }

  /** https://docs.databricks.com/api/workspace/commandexecution/commandstatus#results-fileName */
  private fun setImagePanel(data: String) {
    val image = extractImageFromString(data)
    innerPanel.firstComponent = ImagePanel().apply {
      this.image = image
      minimumSize = Dimension(1, 1)
      maximumSize = Dimension(Int.MAX_VALUE, Int.MAX_VALUE)
    }
  }

  private fun extractImageFromString(data: String): BufferedImage {
    // filename contains "data:image/png;base64,iVBORw0KGgoAAAANSU..."
    val commaIndex = data.indexOf(',')
    if (commaIndex == -1) {
      setConsolePanel("Unknown image data ${data.substring(0, max(data.length, 15))}", isError = true)
    }

    val apostropheIndex = data.indexOf('\'')
    // We need to trim because we have \n at the end.
    val imageData = data.substring(commaIndex + 1, if (apostropheIndex == -1) data.length else apostropheIndex).trim()
    val imageByte = Base64.getDecoder().decode(imageData)
    val bis = ByteArrayInputStream(imageByte)
    val image = ImageIO.read(bis)
    bis.close()

    return image
  }

  private fun setConsolePanel(text: String, isError: Boolean) {
    if (innerPanel.firstComponent != consoleView.component) {
      innerPanel.firstComponent = consoleView.component
    }
    printToConsole(if (text.isBlank() && !isError) DatabricksBundle.message("controller.server.run.no.result") else text, isError)
  }

  private fun setErrorPanel(it: DataModelError) {
    innerPanel.firstComponent = ErrorPanel(it.cause) {
      ConnectionSettings.open(project, dataManager.connectionId)
    }
    innerPanel.secondComponent = null
  }

  private fun printToConsole(dataString: String, isError: Boolean) {
    consoleView.clear()
    val ansiEscapeDecoder = AnsiEscapeDecoder()
    ansiEscapeDecoder.escapeText(dataString, if (isError) ProcessOutputTypes.STDERR else ProcessOutputTypes.STDOUT, consoleView)
  }

  private fun setLoadingPanel() {
    val emptyText = JBLabel(MessagesBundle.message("monitoring.log.loading"), AnimatedIcon.Default.INSTANCE, SwingConstants.CENTER).apply {
      fontColor = UIUtil.FontColor.BRIGHTER
    }
    innerPanel.firstComponent = emptyText
    innerPanel.secondComponent = null
  }

  private fun setTaskExecutingPanel() {
    val emptyText = JBLabel(DatabricksBundle.message("monitoring.task.is.executing"), AnimatedIcon.Default.INSTANCE, SwingConstants.CENTER).apply {
      fontColor = UIUtil.FontColor.BRIGHTER
    }
    innerPanel.firstComponent = emptyText
  }
}