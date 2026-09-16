package com.jetbrains.spark.monitoring.ui.pages

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.ui.Splitter
import com.intellij.openapi.util.Disposer
import com.intellij.ui.OnePixelSplitter
import com.jetbrains.bigdatatools.common.monitoring.data.model.ObjectDataModel
import com.jetbrains.bigdatatools.common.monitoring.table.DataTable
import com.jetbrains.bigdatatools.common.monitoring.table.DataTableCreator
import com.jetbrains.bigdatatools.common.monitoring.table.extension.TableColumnsFitter
import com.jetbrains.bigdatatools.common.monitoring.table.extension.TableExtensionType
import com.jetbrains.bigdatatools.common.monitoring.table.extension.TableLoadingDecorator
import com.jetbrains.bigdatatools.common.monitoring.table.getSelectedData
import com.jetbrains.bigdatatools.common.monitoring.table.model.DataTableColumnModel
import com.jetbrains.bigdatatools.common.monitoring.table.model.DataTableModel
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.DetailsMonitoringController
import com.jetbrains.bigdatatools.common.settings.ColumnVisibilitySettings
import com.jetbrains.bigdatatools.common.table.MaterialJBScrollPane
import com.jetbrains.bigdatatools.common.table.MaterialTableUtils
import com.jetbrains.bigdatatools.common.ui.ToolbarVerticalLabelAction
import com.jetbrains.bigdatatools.common.ui.setCenterComponent
import com.jetbrains.bigdatatools.common.ui.setLineStartComponent
import com.jetbrains.bigdatatools.common.util.ToolbarUtils
import com.jetbrains.spark.monitoring.data.AppAttemptId
import com.jetbrains.spark.monitoring.data.RDDDataDistribution
import com.jetbrains.spark.monitoring.data.RDDPartitionInfo
import com.jetbrains.spark.monitoring.data.RDDStorageInfo
import com.jetbrains.spark.monitoring.models.SparkDataManager
import com.jetbrains.spark.monitoring.models.StoragesDataId
import com.jetbrains.spark.monitoring.settings.SparkToolwindowSettings
import com.jetbrains.spark.monitoring.ui.utils.OpenUrlAction
import com.jetbrains.spark.monitoring.util.SMMessagesBundle
import java.awt.BorderLayout
import java.util.EnumSet
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.ListSelectionEvent

class StoragePageController(project: Project, connectionId: String) : DetailsMonitoringController<AppAttemptId> {
  private val dataManager = SparkDataManager.getInstance(connectionId, project) ?: error("Data Manager is not initialized")

  private val splitter = OnePixelSplitter(false, SparkToolwindowSettings.getInstance().getStorageSplitterProportion(connectionId))
  private val dataSplitter = OnePixelSplitter(false,
                                              SparkToolwindowSettings.getInstance().getStorageDetailsSplitterProportion(connectionId))

  private val table: DataTable<RDDStorageInfo>

  private val dataDistributionTableModel = DataTableModel(
    null,
    DataTableColumnModel(RDDDataDistribution.renderableColumns,
                         SparkToolwindowSettings.getInstance().storagesDistributionColumnSettings))

  private val dataDistributionTable = DataTableCreator.create(
    dataDistributionTableModel,
    EnumSet.of(TableExtensionType.SPEED_SEARCH,
               TableExtensionType.RENDERERS_SETTER))

  private val storagePartitionTableModel: DataTableModel<RDDPartitionInfo>
  private val storagePartitionTable: DataTable<RDDPartitionInfo>

  private var storagesId: StoragesDataId? = null

  init {
    Disposer.register(this, dataDistributionTable)

    storagePartitionTableModel = DataTableModel(null, DataTableColumnModel(
      RDDPartitionInfo.renderableColumns,
      SparkToolwindowSettings.getInstance().storagesPartitionColumnSettings)
    )

    storagePartitionTable = DataTableCreator.create(storagePartitionTableModel, EnumSet.of(TableExtensionType.SPEED_SEARCH,
                                                                                           TableExtensionType.RENDERERS_SETTER))
    Disposer.register(this, storagePartitionTable)

    val settings = SparkToolwindowSettings.getInstance()
    val storagesColumnSettings = settings.storagesColumnSettings

    val columnModel = DataTableColumnModel(RDDStorageInfo.renderableColumns, storagesColumnSettings)
    val tableModel = DataTableModel(null, columnModel)

    table = DataTableCreator.create(tableModel, EnumSet.of(TableExtensionType.SPEED_SEARCH,
                                                           TableExtensionType.RENDERERS_SETTER,
                                                           TableExtensionType.ERROR_HANDLER,
                                                           TableExtensionType.SELECTION_PRESERVER,
                                                           TableExtensionType.LOADING_INDICATOR,
                                                           TableExtensionType.COLUMNS_FITTER,
                                                           TableExtensionType.SMART_RESIZER))

    Disposer.register(this, table)

    table.tableHeader.border = BorderFactory.createEmptyBorder()

    val tablePanel = SimpleToolWindowPanel(true, true).apply {
      setContent(MaterialJBScrollPane(table))
      val actionToolbar = createToolbar(project, connectionId, columnModel)
      actionToolbar.targetComponent = this
      toolbar = actionToolbar.component
    }

    if (settings.getSparkConfigOrDefault(connectionId).showStoragePartitions) {
      showStoragePartitions()
    }

    if (settings.getSparkConfigOrDefault(connectionId).showStorageDistribution) {
      showDataDistributions()
    }

    splitter.firstComponent = tablePanel
    splitter.secondComponent = dataSplitter

    setupSelectionListener(table)

    splitter.addPropertyChangeListener(Splitter.PROP_PROPORTION) {
      settings.setStorageSplitterProportion(connectionId, splitter.proportion)
    }

    dataSplitter.addPropertyChangeListener(Splitter.PROP_PROPORTION) {
      settings.setStorageDetailsSplitterProportion(connectionId, splitter.proportion)
    }
  }

  private fun createPartitionToolbar(): ActionToolbar {
    val configStoragesColumnsAction = ColumnVisibilitySettings.createAction(
      storagePartitionTableModel.columnModel.allColumns,
      SparkToolwindowSettings.getInstance().storagesPartitionColumnSettings)

    val actions = DefaultActionGroup(ToolbarVerticalLabelAction.create(SMMessagesBundle.message("applications.tab.storage.partitions")),
                                     configStoragesColumnsAction)
    return ToolbarUtils.createActionToolbar("BDTSparkStoragePartition", actions, horizontal = false)
  }

  private fun createDistributionToolbar(): ActionToolbar {
    val configStoragesColumnsAction = ColumnVisibilitySettings.createAction(
      dataDistributionTableModel.columnModel.allColumns,
      SparkToolwindowSettings.getInstance().storagesDistributionColumnSettings
    )

    val actions = DefaultActionGroup(ToolbarVerticalLabelAction.create(SMMessagesBundle.message("applications.tab.storage.distribution")),
                                     configStoragesColumnsAction)
    return ToolbarUtils.createActionToolbar("BDTSparkStorageDistribution", actions, horizontal = false)
  }

  private fun setupSelectionListener(table: DataTable<RDDStorageInfo>) {
    table.selectionModel.addListSelectionListener { e: ListSelectionEvent ->

      if (e.valueIsAdjusting) return@addListSelectionListener

      val storageInfo = table.getSelectedData()
      setDataDistributions(storageInfo?.dataDistribution)
      setStoragePartitions(storageInfo?.partitions)
    }
  }

  override fun setDetailsId(id: AppAttemptId) = setStoragesId(StoragesDataId(id))

  private fun setStoragesId(storagesId: StoragesDataId) {
    this.storagesId = storagesId
    val storagesModel = dataManager.getStoragesModel(storagesId.applicationId)
    table.tableModel.setDataModel(storagesModel)
    TableColumnsFitter.get(table)?.reset()
    TableLoadingDecorator.installOn(table)
  }

  private fun createToolbar(project: Project, connectionId: String, columnModel: DataTableColumnModel<RDDStorageInfo>): ActionToolbar {

    val settings = SparkToolwindowSettings.getInstance()

    val togglePartitionAction = object : DumbAwareToggleAction(SMMessagesBundle.message("applications.tab.storage.partitions"),
                                                               SMMessagesBundle.message("applications.tab.storage.partitions.toggle.hint"),
                                                               AllIcons.Actions.ListFiles) {
      init {
        templatePresentation.putClientProperty(ActionUtil.SHOW_TEXT_IN_TOOLBAR, true)
      }
      override fun isSelected(e: AnActionEvent) = settings.getSparkConfigOrDefault(connectionId).showStoragePartitions
      override fun getActionUpdateThread() = ActionUpdateThread.BGT
      override fun setSelected(e: AnActionEvent, state: Boolean) {
        settings.getSparkConfigOrDefault(connectionId).showStoragePartitions = state
        if (state) {
          showStoragePartitions()
        }
        else {
          dataSplitter.firstComponent = null
        }
      }
    }

    val toggleDistributionAction = object : DumbAwareToggleAction(SMMessagesBundle.message("applications.tab.storage.distribution"),
                                                                  SMMessagesBundle.message(
                                                                    "applications.tab.storage.distribution.toggle.hint"),
                                                                  AllIcons.Actions.PreviewDetails) {
      init {
        templatePresentation.putClientProperty(ActionUtil.SHOW_TEXT_IN_TOOLBAR, true)
      }
      override fun isSelected(e: AnActionEvent) = settings.getSparkConfigOrDefault(connectionId).showStorageDistribution
      override fun getActionUpdateThread() = ActionUpdateThread.BGT
      override fun setSelected(e: AnActionEvent, state: Boolean) {
        settings.getSparkConfigOrDefault(connectionId).showStorageDistribution = state
        if (state) {
          showDataDistributions()
        }
        else {
          dataSplitter.secondComponent = null
        }
      }
    }

    val configStoragesColumnsAction = ColumnVisibilitySettings.createAction(columnModel.allColumns, settings.storagesColumnSettings)

    val actions = DefaultActionGroup().apply {
      add(configStoragesColumnsAction)
      add(OpenUrlAction({ "storage" }, project, dataManager) { storagesId?.applicationId?.toString() })
      add(togglePartitionAction)
      add(toggleDistributionAction)
    }

    return ActionManager.getInstance().createActionToolbar("BDTSparkStorage", actions, false)
  }

  private fun showDataDistributions() {
    dataSplitter.secondComponent = JPanel(BorderLayout()).apply {
      setCenterComponent(MaterialJBScrollPane(dataDistributionTable).apply {
        border = BorderFactory.createEmptyBorder()
      })
      val actionToolbar = createDistributionToolbar()
      actionToolbar.targetComponent = this
      setLineStartComponent(actionToolbar.component)
    }
  }

  private fun showStoragePartitions() {
    dataSplitter.firstComponent = JPanel(BorderLayout()).apply {
      setCenterComponent(MaterialJBScrollPane(storagePartitionTable).apply {
        border = BorderFactory.createEmptyBorder()
      })
      val actionToolbar = createPartitionToolbar()
      actionToolbar.targetComponent = this
      setLineStartComponent(actionToolbar.component)
    }
  }

  private fun setDataDistributions(dataDistribution: List<RDDDataDistribution>?) {
    dataDistributionTableModel.setDataModel(
      if (dataDistribution == null)
        null
      else
        ObjectDataModel(RDDDataDistribution::address) {
          dataDistribution to false
        }.also {
          it.update()
        })

    MaterialTableUtils.fitColumnsWidth(dataDistributionTable)
  }

  private fun setStoragePartitions(partitions: List<RDDPartitionInfo>?) {
    storagePartitionTableModel.setDataModel(
      if (partitions == null)
        null
      else
        ObjectDataModel(RDDPartitionInfo::blockName) {
          partitions to false
        }.also {
          it.update()
        })

    MaterialTableUtils.fitColumnsWidth(storagePartitionTable)
  }

  override fun getComponent(): JComponent = splitter

  override fun dispose() {}
}