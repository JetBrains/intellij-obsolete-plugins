package com.intellij.bigdatatools.databricks.toolwindow.configuration

import com.databricks.sdk.service.compute.State
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionDataImpl
import com.intellij.bigdatatools.coreUi.util.NotificationUtils
import com.intellij.bigdatatools.databricks.client.DatabricksDataManager
import com.intellij.bigdatatools.databricks.model.ClusterInfoPresentable
import com.intellij.bigdatatools.databricks.sync.SyncStatus
import com.intellij.bigdatatools.databricks.util.DatabricksBundle
import com.intellij.bigdatatools.databricks.util.DbIcons
import com.intellij.bigdatatools.databricks.util.noGap
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.openapi.observable.properties.ObservableProperty
import com.intellij.openapi.observable.util.bind
import com.intellij.openapi.observable.util.equalsTo
import com.intellij.openapi.observable.util.isNotNull
import com.intellij.openapi.observable.util.isNull
import com.intellij.openapi.observable.util.transform
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.listCellRenderer.listCellRenderer
import com.intellij.util.ui.AsyncProcessIcon
import com.intellij.util.ui.JBUI
import com.jetbrains.bigdatatools.common.monitoring.data.listener.DataModelListener
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.ComponentController
import com.jetbrains.bigdatatools.common.rfs.driver.SafeExecutor
import com.jetbrains.bigdatatools.common.rfs.driver.metainfo.components.SelectableLabel
import kotlinx.coroutines.launch
import org.jetbrains.annotations.Nls
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.awt.event.ItemEvent
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.BorderFactory
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants

internal class DbConfigurationController(val project: Project, val dataManager: DatabricksDataManager) : ComponentController {
  private val user = AtomicProperty("")
  private val connType = AtomicProperty(dataManager.connectionData.connType.name)
  private val dataModel = dataManager.configurationModel

  private val clusterCombobox = createClusterComboBox()

  private val clusterInfo = AtomicProperty(clusterCombobox.item)

  private val isClusterStarted = clusterInfo.transform {
    it?.info?.isClusterStarted() == true
  }
  private val isClusterStopped = clusterInfo.transform {
    it?.info?.isClusterStarted() == false
  }

  private val syncTask = dataManager.syncManager.getTaskForProject(project)

  private val innerPanel = panel {
    clusterGroup()
    syncDestGroup()
    workspaceGroup()
  }.withBorder(JBUI.Borders.emptyLeft(5))

  private val jbScrollPane = JBScrollPane(innerPanel).apply {
    border = BorderFactory.createEmptyBorder()
    horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
  }

  // Left toolbar and center panel.
  private val externalPanel = JPanel(BorderLayout()).apply {
    add(jbScrollPane, BorderLayout.CENTER)
  }

  //private val openDbAction = OpenUrlAction(dataManager) { "" }

  private val isUpdateCombobox = AtomicBoolean(false)

  private val modelListener = object : DataModelListener {
    override fun onChanged() {
      val data = dataModel.data?.obj
      user.set(data?.user ?: "")
      connType.set(dataManager.connectionData.connType.name)

      updateClusterComboBox()
    }
  }

  init {
    dataModel.addListener(modelListener)

    updateClusterComboBox()
  }

  override fun dispose() {
    dataModel.removeListener(modelListener)
  }

  override fun getComponent(): JPanel = externalPanel

  private fun Panel.workspaceGroup() {
    group(DatabricksBundle.message("title.workspace")) {
      rowWithSelectableLabel(DatabricksBundle.message("configuration.auth"), connType)
      rowWithSelectableLabel(DatabricksBundle.message("configuration.user"), user)
      @Suppress("HardCodedStringLiteral") // URI cannot be i18.
      val httpUrl = ConnectionDataImpl.getUrlWithHttp(dataManager.connectionData.getRealUri())
      linkIfNotBlank(DatabricksBundle.message("configuration.host"), httpUrl) {
        BrowserUtil.browse(httpUrl)
      }
    }
  }

  private fun getClusterStateTitle(state: State?): @Nls String {
    if (state == null) return ""
    return when (state) {
      State.ERROR -> DatabricksBundle.message("cluster.state.error")
      State.PENDING -> DatabricksBundle.message("cluster.state.pending")
      State.RESIZING -> DatabricksBundle.message("cluster.state.resizing")
      State.RESTARTING -> DatabricksBundle.message("cluster.state.restarting")
      State.RUNNING -> DatabricksBundle.message("cluster.state.running")
      State.TERMINATED -> DatabricksBundle.message("cluster.state.terminated")
      State.TERMINATING -> DatabricksBundle.message("cluster.state.terminating")
      State.UNKNOWN -> DatabricksBundle.message("cluster.state.unknown")
    }
  }

  private fun Panel.clusterGroup() {
    val manager = ActionManager.getInstance()
    group(DatabricksBundle.message("title.cluster")) {
      row(DatabricksBundle.message("combobox.configuration.cluster")) {
        val startClusterButton = createActionButton(manager.getAction("Databricks.StartCluster"), CLUSTER_ACTIONS_PLACE).visibleIf(isClusterStopped)
        val restartClusterButton = createActionButton(manager.getAction("Databricks.RestartCluster"), CLUSTER_ACTIONS_PLACE).visibleIf(isClusterStarted)
        val stopClusterButton = createActionButton(manager.getAction("Databricks.StopCluster"), CLUSTER_ACTIONS_PLACE).visibleIf(isClusterStarted)

        val openClusterInBrowser = createActionButton(manager.getAction("Databricks.OpenClusterInBrowser"), CLUSTER_ACTIONS_PLACE)

        val hintLabel = JLabel(AllIcons.General.ContextHelp).visibleIf(clusterInfo.isNotNull())

        fun updateTooltipText() {
          val clusterInfo = clusterInfo.get()

          hintLabel.toolTipText = if (clusterInfo == null) {
            DatabricksBundle.message("cluster.not.selected")
          }
          else {
            @Suppress("HardCodedStringLiteral")
            """
          ${DatabricksBundle.message("cluster.driver")}: ${clusterInfo.info?.driver ?: ""}<br>
          ${DatabricksBundle.message("cluster.worker")}: ${clusterInfo.info?.nodes ?: DatabricksBundle.message("cluster.worker.single")}<br>
          ${DatabricksBundle.message("cluster.runtime")}: ${clusterInfo.info?.runtime ?: ""}<br>
          ${DatabricksBundle.message("cluster.creator")}: ${clusterInfo.info?.creator ?: ""}
        """.trimIndent()
          }
        }

        updateTooltipText()

        // The reason for complex layout here is: We should properly reduce width of combobox, when the panel width changed.
        val panel = JPanel(BorderLayout()).apply {
          add(clusterCombobox, BorderLayout.CENTER)
          val buttonsPanel = JPanel().apply {
            add(startClusterButton)
            add(restartClusterButton)
            add(stopClusterButton)
            add(openClusterInBrowser)
            add(hintLabel)
          }
          add(buttonsPanel, BorderLayout.LINE_END)
        }
        cell(panel)

        clusterInfo.afterChange {
          startClusterButton.update()
          restartClusterButton.update()
          stopClusterButton.update()
          openClusterInBrowser.update()
          updateTooltipText()
        }
      }.noGap()

      row(DatabricksBundle.message("cluster.state")) {
        val stateObservableProperty = clusterInfo.transform { it?.info?.state }

        val progressIcon = AsyncProcessIcon("Loading")
        val stateText = JLabel().bind(stateObservableProperty.transform { getClusterStateTitle(stateObservableProperty.get()) })

        cell(progressIcon).gap(RightGap.SMALL).visibleIf(stateObservableProperty.equalsTo(State.PENDING))
        cell(stateText).gap(RightGap.SMALL)
      }.noGap().visibleIf(clusterInfo.isNotNull())

      row {
        label(DatabricksBundle.message("cluster.is.not.selected")).component.icon = DbIcons.CLUSTER_EMPTY
      }.noGap().visibleIf(clusterInfo.isNull())
    }
  }

  private fun Panel.syncDestGroup() {
    val syncState = syncTask.processListener.status

    group(DatabricksBundle.message("title.sync.dest")) {
      row {
        label(DatabricksBundle.message("path")).gap(RightGap.SMALL)

        // The reason for complex layout here is: We should properly reduce width of path with navigate buttons when the panel width changed.
        val pathObservable = user.transform { syncTask.syncMapper.baseRemotePath.stringRepresentation() }
        cell(JPanel(BorderLayout()).apply {
          add(SelectableLabel(pathObservable.get()).apply {
            pathObservable.afterChange { this.text = it }
          }, BorderLayout.CENTER)
          val action = DumbAwareAction.create(DatabricksBundle.message("configuration.change.path.action.text"), AllIcons.Actions.MenuOpen) {
            val newPath = Messages.showInputDialog(DatabricksBundle.message("configuration.change.path.dialog.text"),
                                                   DatabricksBundle.message("dialog.databricks.title"), null,
                                                   syncTask.syncMapper.baseRemotePath.stringRepresentation(),
                                                   null)
            if (newPath != null) {
              dataManager.connectionData.customRemotePath = newPath.ifBlank { null }
              user.set(user.get()) // To update path label.
            }
          }
          val actionButton = createActionButton(action, "DatabricksConfigurationPath")
          add(actionButton, BorderLayout.LINE_END)
        })
      }

      val linkLabel = syncState.transform {
        when (it) {
          SyncStatus.STOPPED -> DatabricksBundle.message("link.sync.start")
          SyncStatus.IN_PROGRESS, SyncStatus.WATCHING_FOR_CHANGES -> DatabricksBundle.message("link.sync.stop")
          SyncStatus.ERROR -> DatabricksBundle.message("link.sync.start")
        }
      }
      linkIfNotBlank("", linkLabel) {
        SafeExecutor.instance.coroutineScope.launch {
          try {
            if (syncState.get().isRunning()) {
              syncTask.stop()
            }
            else {
              syncTask.start()
            }
          }
          catch (t: Throwable) {
            NotificationUtils.notifyException(t, DatabricksBundle.message("databricks.sync.notification.label"))
          }
        }
      }

      row {
        val jLabel = label("").component
        val iconProp = syncState.transform { it.icon }
        jLabel.icon = iconProp.get()
        iconProp.afterChange {
          jLabel.icon = it
        }
        text("").bindText(syncTask.processListener.statusMessage).component.isEditable = false
      }.noGap()
    }
  }

  private fun createClusterComboBox(): ComboBox<ClusterInCombobox> {
    val comboBox = ComboBox<ClusterInCombobox>(DefaultComboBoxModel())
    comboBox.isSwingPopup = false
    comboBox.isUsePreferredSizeAsMinimum = false

    comboBox.renderer = listCellRenderer<ClusterInCombobox?> {
      val value = value
      if (value == null) {
        text(DatabricksBundle.message("combobox.cluster.empty")) {
          foreground = greyForeground
        }
        return@listCellRenderer
      }

      value.info?.let {
        icon(DbIcons.getForClusterState(it.info.state))
      }
      if (value.isCreateNew) {
        text(DatabricksBundle.message("combobox.cluster.create.new"))
      }
      else {
        text(value.info?.name ?: "")
        value.info?.id?.let {
          @Suppress("HardCodedStringLiteral") // ClusterId cannot be i18.
          text(it) {
            foreground = greyForeground
          }
        }
      }
      value.title?.let {
        separator { text = it }
      }
    }

    comboBox.addItemListener { event ->
      if (event.stateChange != ItemEvent.SELECTED)
        return@addItemListener

      if (isUpdateCombobox.get())
        return@addItemListener

      val cluster = comboBox.item
      if (cluster?.isCreateNew == true) {
        BrowserUtil.browse(ConnectionDataImpl.getUrlWithHttp(dataManager.connectionData.getRealUri()) + "/#create/cluster")
        comboBox.item = clusterInfo.get()
        return@addItemListener
      }

      dataManager.changeAttachedCluster(cluster?.info)
      clusterInfo.set(cluster.takeIf { it?.isCreateNew == false })
    }
    return comboBox
  }

  private fun JComponent.visibleIf(property: ObservableProperty<Boolean>): JComponent {
    isVisible = property.get()
    property.afterChange {
      isVisible = it
    }
    return this
  }

  private fun ActionButton.visibleIf(property: ObservableProperty<Boolean>): ActionButton {
    isVisible = property.get()
    property.afterChange {
      isVisible = it
    }
    return this
  }

  private fun prepareClusterList(): List<ClusterInCombobox> {
    val clusters = dataManager.getClusters()

    val result = mutableListOf<ClusterInCombobox>()

    val userName = dataModel.data?.obj?.user
    if (userName != null) {
      val singleUserMe = clusters.filter { userName == it.info.singleUserName }
      if (singleUserMe.isNotEmpty()) {
        result += ClusterInCombobox(info = singleUserMe.first(), title = DatabricksBundle.message("combobox.cluster.single.user.me", userName))
        result += singleUserMe.drop(1).map { ClusterInCombobox(info = it) }
      }
    }

    val notSingleUser = clusters.filter { it.info.singleUserName == null }
    if (notSingleUser.isNotEmpty()) {
      result += ClusterInCombobox(info = notSingleUser.first(), title = DatabricksBundle.message("combobox.cluster.not.single.user"))
      result += notSingleUser.drop(1).map { ClusterInCombobox(info = it) }
    }

    val otherSingleUser = clusters.filter { it.info.singleUserName != null && it.info.singleUserName != userName }
    if (otherSingleUser.isNotEmpty()) {
      result += ClusterInCombobox(info = otherSingleUser.first(), title = DatabricksBundle.message("combobox.cluster.single.user.other"))
      result += otherSingleUser.drop(1).map { ClusterInCombobox(info = it) }
    }

    return result + ClusterInCombobox(isCreateNew = true, title = DatabricksBundle.message("combobox.cluster.create.new.title"))
  }

  private fun createActionButton(action: AnAction, place: String): ActionButton {
    return ActionButton(action, action.templatePresentation.clone(), place, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE)
  }

  private fun Panel.rowWithSelectableLabel(@NlsContexts.Label label: String, prop: ObservableProperty<String>): Row {
    return row {
      label(label).gap(RightGap.SMALL)
      cell(SelectableLabel(prop.get()).apply {
        prop.afterChange { this.text = it }
      })
    }.noGap()
  }

  private fun Panel.linkIfNotBlank(@NlsContexts.Label label: String, @NlsContexts.Label prop: String,
                                   action: (ActionEvent) -> Unit) {
    row {
      label(label).gap(RightGap.SMALL)
      link(prop, action)
    }.noGap()
  }

  private fun Panel.linkIfNotBlank(@NlsContexts.Label label: String, prop: ObservableProperty<@NlsContexts.Label String>,
                                   action: (ActionEvent) -> Unit) {
    row {
      label(label).gap(RightGap.SMALL)
      val link = link(prop.get(), action)
      prop.afterChange {
        link.component.text = it
      }
    }.noGap()
  }

  private fun updateClusterComboBox() {
    val newElements = prepareClusterList()
    val selectedClusterId = dataManager.connectionData.connectedClusterId

    // Will try to select something, but if newElements.size == 1 this means that we have only "Cluster management" item.
    val selectedItem = if (newElements.contains(clusterCombobox.selectedItem)) clusterCombobox.selectedItem
    else newElements.firstOrNull { it.info?.id == selectedClusterId } ?: if (newElements.size > 1) newElements.firstOrNull() else null

    val oldElements = (0 until clusterCombobox.model.size).map {
      clusterCombobox.model.getElementAt(it)
    }

    if (oldElements == newElements) {
      clusterCombobox.selectedItem = selectedItem
      return
    }
    isUpdateCombobox.set(true)
    try {
      val model = clusterCombobox.model as DefaultComboBoxModel
      model.removeAllElements()
      model.addAll(if (newElements.size == 1) listOf(null) + newElements else newElements)
    }
    finally {
      isUpdateCombobox.set(false)
    }

    clusterCombobox.selectedItem = selectedItem
  }

  /**
   * 3-state description of combobox item.
   * if isCreateNew flag set - this is a special "create new" item. (other values ignored)
   * if title + info set - this is a cluster node with titled separator
   * if info set - this is normal cluster node
   */
  private data class ClusterInCombobox(val isCreateNew: Boolean = false,
                                       val info: ClusterInfoPresentable? = null,
                                       @Nls val title: String? = null)

  companion object {
    private const val CLUSTER_ACTIONS_PLACE = "DatabricksConfigurationCluster"
  }
}