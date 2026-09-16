package com.intellij.bigdatatools.plugin.spark.arbitrary

import com.intellij.bigdatatools.plugin.spark.arbitrary.utils.ArbitraryClusterUtils
import com.intellij.bigdatatools.plugin.spark.arbitrary.wizard.ArbitraryClusterWizardUtils
import com.intellij.openapi.Disposable
import com.intellij.openapi.observable.util.whenTreeChanged
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.enteredTextSatisfies
import com.intellij.bigdatatools.coreUi.settings.CommonSettingsKeys
import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.intellij.bigdatatools.coreUi.settings.ModificationKey
import com.intellij.bigdatatools.coreUi.fields.StringNamedField
import com.intellij.bigdatatools.coreUi.fields.WrappedComponent
import com.intellij.bigdatatools.coreUi.settings.withNotEmptyValidator
import com.jetbrains.bigdatatools.common.settings.ConnectionSettingsPanel
import com.jetbrains.bigdatatools.common.settings.connections.ConnectionConfigurable
import com.jetbrains.bigdatatools.common.settings.manager.RfsConnectionDataManager
import com.intellij.bigdatatools.coreUi.ui.row
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionData
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionDataImpl
import com.intellij.bigdatatools.coreUi.ui.doOnChange
import com.jetbrains.bigdatatools.common.settings.defaultui.SettingsPanelCustomizerExImpl
import com.jetbrains.bigdatatools.sftp.settings.SftpConnectionGroup
import com.jetbrains.bigdatatools.sftp.settings.fields.SshConfigWrappedComponent
import com.jetbrains.bigdatatools.sftp.util.SftpMessagesBundle
import com.jetbrains.spark.monitoring.settings.SparkConnectionGroup
import com.jetbrains.spark.submit.util.SparkMessagesBundle
import org.jetbrains.annotations.Nls
import javax.swing.SwingUtilities

class ArbitraryClusterSettingsCustomizer(
  val project: Project,
  private val originConnData: ArbitraryClusterConnectionData,
  uiDisposable: Disposable
) : SettingsPanelCustomizerExImpl<ArbitraryClusterConnectionData>() {

  internal val nameField = StringNamedField(ConnectionData::name, CommonSettingsKeys.NAME_KEY, originConnData)
    .withNotEmptyValidator(uiDisposable, MessagesBundle.message("validator.nameField"))

  internal val sshComponent = SshConfigWrappedComponent(ModificationKey(SftpMessagesBundle.message("settings.fields.ssh")),
                                                        originConnData, project, uiDisposable)

  internal val sparkMonitoringConnection = StringNamedField(ArbitraryClusterConnectionData::sparkMonitoringDriverId,
                                                            Utils.SPARK_MONITORING_TYPE_KEY,
                                                            originConnData)

  internal val sftpConnection = StringNamedField(ArbitraryClusterConnectionData::sftpDriverId,
                                                 Utils.SFTP_MONITORING_TYPE_KEY,
                                                 originConnData)


  override fun getDefaultFields(): List<WrappedComponent<in ArbitraryClusterConnectionData>> = listOf(nameField, sshComponent,
                                                                                                      sparkMonitoringConnection,
                                                                                                      sftpConnection)

  private val settingsPanel
    get() = getUserData(ConnectionSettingsPanel.CONNECTION_SETTINGS_PANEL_KEY)

  override fun getDefaultComponent(fields: List<WrappedComponent<in ArbitraryClusterConnectionData>>,
                                   conn: ArbitraryClusterConnectionData) = panel {
    row(nameField)

    row(MessagesBundle.message("settings.tunnel.ssh"), sshComponent.getComponent()).bottomGap(BottomGap.MEDIUM)

    row(SparkMessagesBundle.message("label.implicit.cluster.depend.spark.connection")) {
      link(MessagesBundle.message("link.settings.setup.depend.connection")) {
        addNewSparkHistory()
      }.visibleIf(sparkMonitoringConnection.getTextComponent().enteredTextSatisfies { it == "" })

      val sparkConnectionLink = link(getConnectionName(sparkMonitoringConnection.getValue())) {
        openConnection(sparkMonitoringConnection.getValue())
      }.visibleIf(sparkMonitoringConnection.getTextComponent().enteredTextSatisfies { it != "" })
      sparkMonitoringConnection.getTextComponent().doOnChange {
        sparkConnectionLink.component.text = getConnectionName(sparkMonitoringConnection.getValue())
      }
    }

    row(SparkMessagesBundle.message("label.implicit.cluster.depend.sftp")) {
      link(MessagesBundle.message("link.settings.setup.depend.connection")) {
        addNewSftp()
      }.visibleIf(sftpConnection.getTextComponent().enteredTextSatisfies { it == "" })

      val link = link(getConnectionName(sftpConnection.getValue())) {
        openConnection(sftpConnection.getValue())
      }.visibleIf(sftpConnection.getTextComponent().enteredTextSatisfies { it != "" })
      sftpConnection.getTextComponent().doOnChange {
        link.component.text = getConnectionName(sftpConnection.getValue())
      }
    }

    settingsPanel?.tree?.whenTreeChanged {
      sparkMonitoringConnection.setValue(checkDependConnection(sparkMonitoringConnection.getValue()))
      sftpConnection.setValue(checkDependConnection(sftpConnection.getValue()))
    }
    ArbitraryClusterSettingsCollector.Util.getInstance().initPanel(this)


    if (settingsPanel?.addedConnections?.contains(conn) == true) {
      runWizard()
    }
  }

  private fun openConnection(connId: String) {
    val settingsPanel = settingsPanel ?: return

    if (connId.isBlank())
      return
    val treeNode = settingsPanel.getTreeNode(connId) ?: return
    settingsPanel.selectNodeInTree(treeNode)
  }

  private fun addNewSparkHistory(selectNode: Boolean = true) {
    val settingsPanel = settingsPanel ?: return

    val connection = settingsPanel.createCurrentTestConnection() as? ArbitraryClusterConnectionData ?: return
    val newData = settingsPanel.createNewConnectionFor(SparkConnectionGroup(),
                                                       data = ArbitraryClusterUtils.createSparkConnection(connection),
                                                       selectAddedNode = selectNode)
    sparkMonitoringConnection.setValue(newData.innerId)
  }

  private fun addNewSftp(selectNode: Boolean = true) {
    val settingsPanel = settingsPanel ?: return

    val connection = settingsPanel.createCurrentTestConnection() as? ArbitraryClusterConnectionData ?: return
    val newData = settingsPanel.createNewConnectionFor(SftpConnectionGroup(),
                                                       data = ArbitraryClusterUtils.createSftp(connection),
                                                       selectAddedNode = selectNode)
    sftpConnection.setValue(newData.innerId)
  }


  private fun checkDependConnection(connId: String): String {
    if (connId.isBlank())
      return connId

    val storedConnections = RfsConnectionDataManager.instance?.getConnections(project)
                              ?.map { it.innerId }
                            ?: emptyList()
    val newConnections = settingsPanel?.addedConnections?.map { it.innerId } ?: emptyList()
    val removedConnections = settingsPanel?.removedConnections?.map { it.innerId } ?: emptyList()
    val connections = storedConnections + newConnections - removedConnections.toSet()
    if (connId in connections)
      return connId.removeSuffix(ConnectionDataImpl.TEST_SUFFIX)
    else
      return ""
  }

  override fun apply(conn: ArbitraryClusterConnectionData): List<ModificationKey> {
    val sparkConnId = sparkMonitoringConnection.getValue()
    if (sparkConnId.isNotBlank()) {
      getConnection(sparkConnId)?.sourceConnection = conn.innerId.removeSuffix(ConnectionDataImpl.TEST_SUFFIX)
    }
    val sftpId = sftpConnection.getValue()
    if (sftpId.isNotBlank()) {
      getConnection(sftpId)?.sourceConnection = conn.innerId.removeSuffix(ConnectionDataImpl.TEST_SUFFIX)
    }

    return super.apply(conn)
  }

  private fun getConnection(innerId: String) =
    RfsConnectionDataManager.instance?.getConnectionById(project, innerId)
    ?: settingsPanel?.addedConnections?.firstOrNull { it.innerId == innerId }

  @Nls
  private fun getConnectionName(innerId: String): String =
    getConnection(innerId)?.name ?: MessagesBundle.message("combobox.item.is.not.selected")

  private fun runWizard() {
    val connections = ArbitraryClusterWizardUtils.invokeWizard(project) ?: let {
      SwingUtilities.invokeLater {
        val node = settingsPanel?.getTreeNode(originConnData.innerId) ?: return@invokeLater
        settingsPanel?.removeNode(node)
        settingsPanel?.selectNodeInTree(null as String?)
      }
      return
    }

    SwingUtilities.invokeLater {
      val configurable = settingsPanel?.getTreeNode(originConnData.innerId)?.userObject as ConnectionConfigurable<*, *>
      configurable.component?.perProjectCheckbox?.isEnabled = false
    }

    sshComponent.setValue(connections.sshConfig)

    if (connections.spark != null) {
      connections.spark.sourceConnection = originConnData.innerId
      settingsPanel?.createNewConnectionFor(SparkConnectionGroup(), connections.spark, selectAddedNode = false)
      sparkMonitoringConnection.setValue(connections.spark.innerId)
    }

    if (connections.sftp != null) {
      connections.sftp.sourceConnection = originConnData.innerId
      settingsPanel?.createNewConnectionFor(SftpConnectionGroup(), connections.sftp, selectAddedNode = false)
      sftpConnection.setValue(connections.sftp.innerId)
    }
  }


  object Utils {
    val SPARK_MONITORING_TYPE_KEY = ModificationKey("SPARK_MONITORING_TYPE_KEY")
    val SFTP_MONITORING_TYPE_KEY = ModificationKey("SFTP_MONITORING_TYPE_KEY")
  }
}