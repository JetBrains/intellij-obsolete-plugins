package com.jetbrains.hadoop.monitoring.toolwindow

import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.ScrollPaneFactory
import com.jetbrains.bigdatatools.common.monitoring.data.model.FieldsDataModel
import com.jetbrains.bigdatatools.common.monitoring.list.DataListCreator
import com.jetbrains.bigdatatools.common.monitoring.list.ListClickHelper
import com.jetbrains.bigdatatools.common.monitoring.list.extension.ListExtensionType
import com.jetbrains.bigdatatools.common.monitoring.list.getValueForRow
import com.jetbrains.bigdatatools.common.monitoring.list.model.ListTableModel
import com.jetbrains.bigdatatools.common.settings.ColumnVisibilitySettings
import com.jetbrains.bigdatatools.common.ui.ToolbarVerticalLabelAction
import com.jetbrains.bigdatatools.common.ui.setCenterComponent
import com.jetbrains.bigdatatools.common.ui.setLineStartComponent
import com.jetbrains.bigdatatools.common.util.ToolbarUtils
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.AppInfo
import com.jetbrains.hadoop.monitoring.settings.HadoopConnectionData
import com.jetbrains.hadoop.monitoring.settings.HadoopSettings
import com.jetbrains.hadoop.monitoring.util.HadoopMessagesBundle
import com.jetbrains.hadoop.monitoring.util.HadoopUtils
import java.awt.BorderLayout
import java.util.EnumSet
import java.util.Locale
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Manages Application details view. View - list with  fields and names from Application data class.
 * Displayable fields controlled via HadoopSettings.getInstance(project).applicationDetailsColumnSettings.
 *
 * @author: vitaly.khudobakhshov
 */
class ApplicationDetails(private val project: Project,
                         private val connectionData: HadoopConnectionData,
                         showStacktrace: Boolean = true) : Disposable {

  companion object {
    private val logger = Logger.getInstance(this::class.java)
  }

  private val diagnosticsConsole: ConsoleViewImpl?

  private val splitter = OnePixelSplitter(false, 0.35f)

  private val panel = JPanel(BorderLayout())

  private var data: FieldsDataModel? = null
  private var tableModel: ListTableModel? = null

  init {
    splitter.border = null

    panel.setCenterComponent(splitter)
    panel.setLineStartComponent(createDetailsToolBar().apply { targetComponent = panel }.component)

    if (showStacktrace) {
      val actionGroup = DefaultActionGroup(
        ToolbarVerticalLabelAction.create(HadoopMessagesBundle.message("application.diagnostics.header")))

      diagnosticsConsole = ConsoleViewImpl(project, true)

      val consolePanel = JPanel(BorderLayout()).apply {
        setLineStartComponent(
          ToolbarUtils.createActionToolbar(this, "BDTHadoopApplicationDetails", actionGroup, horizontal = false).component)
        setCenterComponent(diagnosticsConsole.component)
      }

      splitter.secondComponent = consolePanel
      Disposer.register(this, diagnosticsConsole)
    }
    else {
      diagnosticsConsole = null
    }
  }

  fun getComponent(): JComponent = panel

  override fun dispose() {}

  fun setApplication(appInfo: AppInfo?) {
    this.data = FieldsDataModel.createForObject(appInfo)
    if (tableModel == null) {
      tableModel = ListTableModel(data!!, HadoopSettings.getInstance().applicationDetailsColumnSettings)
      val dataList = DataListCreator.create(tableModel!!, EnumSet.allOf(ListExtensionType::class.java))
      Disposer.register(this, dataList)
      splitter.firstComponent = ScrollPaneFactory.createScrollPane(dataList, true)

      ListClickHelper.installOn(dataList, "trackingUrl") { url ->
        if (url as? String == null) return@installOn
        if (dataList.getValueForRow("applicationType")?.toString()?.lowercase(Locale.getDefault())?.contains("spark") == true) {
          HadoopUtils.openSparkUrl(project, url, connectionData)
        }
        else {
          BrowserUtil.open(url)
        }
      }

      ListClickHelper.installBrowseOn(dataList, AppInfo.renderableColumns.map { it.field })
    }
    else {
      tableModel!!.setDataModel(data!!)
    }

    updateDiagnosticsConsole()
  }

  private fun updateDiagnosticsConsole() {
    diagnosticsConsole ?: return
    data ?: return

    try {
      val diagnostics = data!!["diagnostics"] as String?
      diagnosticsConsole.clear()
      diagnosticsConsole.print(if (diagnostics.isNullOrBlank()) HadoopMessagesBundle.message("application.diagnostics.empty")
                               else diagnostics,
                               ConsoleViewContentType.ERROR_OUTPUT)
      diagnosticsConsole.scrollTo(0)
    }
    catch (e: Exception) {
      logger.error(e)
    }
  }

  private fun createDetailsToolBar(): ActionToolbar {
    val appDetailsColumnsVisibility = ColumnVisibilitySettings.createAction(AppInfo.renderableColumns,
                                                                            HadoopSettings.getInstance().applicationDetailsColumnSettings)
    val toolbar = DefaultActionGroup(
      ToolbarVerticalLabelAction.create(HadoopMessagesBundle.message("application.details.header")),
      Separator(),
      appDetailsColumnsVisibility)

    return ToolbarUtils.createActionToolbar("BDTHadoopApplicationDetails", toolbar, horizontal = false)
  }
}