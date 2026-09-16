package com.jetbrains.hadoop.monitoring.toolwindow

import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SideBorder
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.util.ui.UIUtil
import com.jetbrains.bigdatatools.common.monitoring.data.listener.DataModelListener
import com.jetbrains.bigdatatools.common.monitoring.data.model.StringDataModel
import com.jetbrains.bigdatatools.common.monitoring.table.DataTableCreator
import com.jetbrains.bigdatatools.common.monitoring.table.extension.TableExtensionType
import com.jetbrains.bigdatatools.common.monitoring.table.extension.TableHeightFitter
import com.jetbrains.bigdatatools.common.monitoring.table.model.DataTableColumnModel
import com.jetbrains.bigdatatools.common.monitoring.table.model.DataTableModel
import com.jetbrains.bigdatatools.common.settings.ColumnVisibilitySettings
import com.jetbrains.bigdatatools.common.table.FilterSearch
import com.jetbrains.bigdatatools.common.table.MaterialJBScrollPane
import com.jetbrains.bigdatatools.common.table.MaterialTable
import com.jetbrains.bigdatatools.common.ui.CollapsiblePanel
import com.jetbrains.bigdatatools.common.ui.CustomComponentActionImpl
import com.jetbrains.bigdatatools.common.ui.addSearchExtension
import com.jetbrains.hadoop.monitoring.data.HadoopDataManager
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.HadoopConfigurationProperty
import com.jetbrains.hadoop.monitoring.settings.HadoopConnectionData
import com.jetbrains.hadoop.monitoring.settings.HadoopSettings
import com.jetbrains.hadoop.monitoring.settings.ToolCategory
import com.jetbrains.hadoop.monitoring.util.HadoopMessagesBundle
import java.awt.BorderLayout
import java.awt.Dimension
import java.util.EnumSet
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollPane

class Tools(project: Project, connectionData: HadoopConnectionData) : Disposable {
  private val dataManager = HadoopDataManager.getInstance(connectionData.innerId, project) ?: error("Data Manager is not inited")

  private val mainPanel = SimpleToolWindowPanel(false, true)

  private val collapsiblePanels: Array<CollapsiblePanel>

  private fun JComponent.preferredHeight(height: Int) {
    preferredSize = Dimension(preferredSize.width, preferredSize.height.coerceAtLeast(height))
  }

  init {
    val panel = JPanel()
    panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)

    val configuration = CollapsiblePanel(HadoopMessagesBundle.message("tools.configuration"))

    val configurationTable = createConfiguration()
    val configurationTableScrollPane = MaterialJBScrollPane(configurationTable)

    val filterText = ExtendableTextField(15).addSearchExtension()

    val filterAction = object : CustomComponentActionImpl(filterText) {
      override fun update(e: AnActionEvent) {
        e.presentation.isVisible = !configuration.collapsed
      }

      override fun getActionUpdateThread() = ActionUpdateThread.BGT
    }

    FilterSearch.installOn(configurationTable, filterText.document) {
      TableHeightFitter.fitSize(configurationTableScrollPane, configurationTable)
      panel.revalidate()
    }
    configuration.addActions(filterAction)

    configuration.border = IdeBorderFactory.createBorder(SideBorder.BOTTOM)
    configuration.component = configurationTableScrollPane
      .apply { border = BorderFactory.createEmptyBorder(); preferredHeight(300) }
    configuration.collapsed = !HadoopSettings.getInstance().getExpandedTools(connectionData.innerId, ToolCategory.Configuration)
    configuration.addExpandListener { value ->
      HadoopSettings.getInstance().setExpandedTools(connectionData.innerId, ToolCategory.Configuration, value)
    }
    panel.add(configuration)

    val logs = Logs(project, connectionData)
    Disposer.register(this, logs)
    val logsPanel = CollapsiblePanel(HadoopMessagesBundle.message("tools.localLogs")).apply {
      border = IdeBorderFactory.createBorder(SideBorder.BOTTOM)
      component = logs.getComponent()
      collapsed = !HadoopSettings.getInstance().getExpandedTools(connectionData.innerId, ToolCategory.LocalLogs)
    }
    logsPanel.addExpandListener { value ->
      HadoopSettings.getInstance().setExpandedTools(connectionData.innerId, ToolCategory.LocalLogs, value)
    }
    panel.add(logsPanel)

    val stacksModel = dataManager.getToolsStacks()
    val stacksPanel = createPanelWithConsole(project, HadoopMessagesBundle.message("tools.serverStacks"), stacksModel)
    stacksPanel.collapsed = !HadoopSettings.getInstance().getExpandedTools(connectionData.innerId, ToolCategory.ServerStacks)
    stacksPanel.addExpandListener { value ->
      HadoopSettings.getInstance().setExpandedTools(connectionData.innerId, ToolCategory.ServerStacks, value)
    }
    panel.add(stacksPanel)

    val metricsModel = dataManager.getToolsMetrics()
    val metricsPanel = createPanelWithConsole(project, HadoopMessagesBundle.message("tools.serverMetrics"), metricsModel)
    metricsPanel.collapsed = !HadoopSettings.getInstance().getExpandedTools(connectionData.innerId, ToolCategory.ServerMetrics)
    metricsPanel.addExpandListener { value ->
      HadoopSettings.getInstance().setExpandedTools(connectionData.innerId, ToolCategory.ServerMetrics, value)
    }
    panel.add(metricsPanel)

    collapsiblePanels = arrayOf(configuration, logsPanel, stacksPanel, metricsPanel)

    val component = JPanel(BorderLayout())
    component.add(panel, BorderLayout.NORTH)

    mainPanel.setContent(JBScrollPane(component))
  }

  private fun createPanelWithConsole(project: Project, @NlsContexts.BorderTitle title: String, dataModel: StringDataModel) =
    CollapsiblePanel(title).apply {
      border = IdeBorderFactory.createBorder(SideBorder.BOTTOM)
      component = createStringModelView(project, dataModel)
    }

  private fun createConfiguration(): MaterialTable {
    val configurationModel = dataManager.getToolsConfiguration()

    val columnModel = DataTableColumnModel(HadoopConfigurationProperty.renderableColumns, ColumnVisibilitySettings(
      mutableListOf("name", "value", "final", "source"))
    )
    val tableModel = DataTableModel(configurationModel, columnModel)

    val table = DataTableCreator.create(tableModel, EnumSet.of(TableExtensionType.SPEED_SEARCH,
                                                               TableExtensionType.RENDERERS_SETTER,
                                                               TableExtensionType.COLUMNS_FITTER,
                                                               TableExtensionType.ERROR_HANDLER,
                                                               TableExtensionType.SELECTION_PRESERVER,
                                                               TableExtensionType.LOADING_INDICATOR,
                                                               TableExtensionType.SMART_RESIZER))

    Disposer.register(this, table)

    return table
  }

  private fun createStringModelView(project: Project, dataModel: StringDataModel): JScrollPane {

    val consoleView = ConsoleViewImpl(project, false)
    consoleView.autoscrolls = false
    Disposer.register(this, consoleView)

    dataModel.addListener(object : DataModelListener {
      override fun onChanged() = setText(dataModel.data ?: "", ConsoleViewContentType.NORMAL_OUTPUT)

      override fun onError(msg: String, e: Throwable?) = setText(msg, ConsoleViewContentType.ERROR_OUTPUT)

      private fun setText(text: String, type: ConsoleViewContentType) {
        UIUtil.invokeLaterIfNeeded {
          val scrollingModel = consoleView.editor?.scrollingModel
          val offset = scrollingModel?.verticalScrollOffset

          consoleView.clear()
          consoleView.print(text, type)

          if (scrollingModel != null && offset != null) {
            consoleView.performWhenNoDeferredOutput {
              scrollingModel.disableAnimation()
              scrollingModel.scrollVertically(offset)
              scrollingModel.enableAnimation()
            }
          }
        }
      }
    })

    return ScrollPaneFactory.createScrollPane(consoleView.component.apply { preferredHeight(300) }, true)
  }

  fun getComponent() = mainPanel

  override fun dispose() {}
}