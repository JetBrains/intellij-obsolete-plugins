package com.intellij.bigdatatools.emr.toolwindow.controllers

import com.intellij.bigdatatools.emr.data.EmrDataManager
import com.intellij.bigdatatools.emr.settings.EmrToolWindowSettings
import com.intellij.bigdatatools.emr.util.EmrMessagesBundle
import com.intellij.bigdatatools.sftp.icons.BigdatatoolsSftpIcons
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.ActionLink
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.WrapLayout
import com.intellij.bigdatatools.coreUi.util.NotificationUtils
import com.jetbrains.bigdatatools.common.monitoring.actions.OpenUrlAction
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.AbstractGroupFieldsModelsController
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.AbstractTableController
import com.jetbrains.bigdatatools.common.ui.BdtJsonInfoDialog
import com.jetbrains.bigdatatools.common.ui.setNorthComponent
import com.jetbrains.bigdatatools.sftp.util.SshUtils
import kotlinx.coroutines.launch
import org.com.jetbrains.bigdatatools.aws.s3.S3DriverCompatible
import org.com.jetbrains.bigdatatools.utils.HdfsMessagesBundle
import java.awt.FlowLayout
import javax.swing.JPanel

class EmrClusterInfoController(project: Project, override val dataManager: EmrDataManager) :
  AbstractGroupFieldsModelsController<String>(project, dataManager.connectionData.innerId) {

  override val toolWindowSettings = EmrToolWindowSettings.getInstance()

  init {
    init()
    intermediatePanel.setNorthComponent(createLinksPanel())
  }

  override fun createActions(): List<AnAction> {

    val openUrlAction = OpenUrlAction(dataManager) {
      val clusterId = id ?: return@OpenUrlAction null
      "#cluster-details:$clusterId"
    }

    val openMasterSftpAction = object : DumbAwareAction(
      EmrMessagesBundle.message("aws.cluster.info.action.open.master.sftp"), null, BigdatatoolsSftpIcons.Sftp) {

      override fun actionPerformed(e: AnActionEvent) {
        val clusterId = id ?: return
        dataManager.openMasterSftpConnection(project, clusterId)
      }

      override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = dataManager.getCachedClusterDetails(id)?.state?.isRunning() ?: false
        e.presentation.text = if (e.presentation.isEnabled || e.place != AbstractTableController.TABLE_TOOLBAR_PLACE) {
          EmrMessagesBundle.message("aws.cluster.info.action.open.master.sftp")
        }
        else {
          @Suppress("DialogTitleCapitalization") // All is ok here. "Open Master SFTP Connection" - name of the item, and it is capitalized.
          EmrMessagesBundle.message("aws.cluster.info.action.open.master.sftp.unavailable")
        }
      }

      override fun getActionUpdateThread() = ActionUpdateThread.BGT
    }

    val createSshConnectionAction = object : DumbAwareAction(EmrMessagesBundle.message("action.ssh.master.node"), null,
                                                             AllIcons.Debugger.Console) {
      override fun actionPerformed(e: AnActionEvent) {
        val id = id ?: return
        val cluster = dataManager.getClusterInfoModel(id).originObject ?: return

        dataManager.driver.safeExecutor.coroutineScope.launch {
          val config = dataManager.sshManager.getOrAskSetupOrShowError(project, cluster) ?: return@launch
          SshUtils.runSshConsole(project, config, "/")
        }

      }

      override fun update(e: AnActionEvent) {
        e.presentation.isVisible = SshUtils.isSshConsoleAvailable()
        e.presentation.isEnabled = dataManager.isClusterRun(id)
      }

      override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
    }


    val openS3LogAction = object : DumbAwareAction(
      EmrMessagesBundle.message("aws.cluster.info.action.open.s3.log"), null, S3DriverCompatible.driverIcon) {

      override fun actionPerformed(e: AnActionEvent) {
        val clusterId = id ?: return
        dataManager.createS3LogConnection(project, clusterId, null)
      }

      override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = id != null
      }

      override fun getActionUpdateThread() = ActionUpdateThread.BGT
    }

    val terminateProtectionAction = object : DumbAwareAction(
      EmrMessagesBundle.message("aws.cluster.info.action.terminate.protection"), null, AllIcons.Diff.GutterCheckBox) {

      override fun actionPerformed(e: AnActionEvent) {
        val id = id ?: return
        val curValue = dataManager.getClusterInfoModel(id).originObject?.cluster?.terminationProtected() ?: return
        val msg = if (curValue) {
          EmrMessagesBundle.message("emr.cluster.terminate.protection.disable")
        }
        else {
          EmrMessagesBundle.message("emr.cluster.terminate.protection.enable")
        }
        val res = Messages.showYesNoDialog(project, msg, EmrMessagesBundle.message("emr.cluster.terminate.protection.title"),
                                           Messages.getQuestionIcon())

        if (res == Messages.YES)
          dataManager.setTerminationProtection(id, !curValue)
      }

      override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = dataManager.getCachedClusterDetails(id)?.state?.isRunning() ?: false

        val id = id ?: return
        val isProtectionEnabled = dataManager.getClusterInfoModel(id).originObject?.cluster?.terminationProtected() ?: false
        e.presentation.icon = if (isProtectionEnabled) AllIcons.Diff.GutterCheckBoxSelected else AllIcons.Diff.GutterCheckBox

        if (e.presentation.isEnabled || e.place != AbstractTableController.TABLE_TOOLBAR_PLACE) {
          e.presentation.text = EmrMessagesBundle.message("aws.cluster.info.action.terminate.protection")
        }
        else {
          @Suppress("DialogTitleCapitalization") //"Termination Protection" capitalized because this is button title.
          e.presentation.text = EmrMessagesBundle.message("aws.cluster.info.action.terminate.protection.unavailable")
        }
      }

      override fun getActionUpdateThread() = ActionUpdateThread.BGT
    }

    val showClusterDetailsAction = object : DumbAwareAction(HdfsMessagesBundle.message("emr.cluster.info.details"), null,
                                                            AllIcons.FileTypes.Json) {
      override fun actionPerformed(e: AnActionEvent) {
        val id = id ?: return
        val clusterDetails = dataManager.getClusterInfoModel(id).originObject?.cluster ?: return
        BdtJsonInfoDialog(project, clusterDetails.name(), clusterDetails).show()
      }

      override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = id != null
      }

      override fun getActionUpdateThread() = ActionUpdateThread.BGT
    }

    return super.createActions() + listOf(terminateProtectionAction, Separator.create(),
                                          createSshConnectionAction,
                                          openMasterSftpAction, openS3LogAction,
                                          Separator.create(),
                                          showClusterDetailsAction,
                                          openUrlAction)
  }

  private fun createLinksPanel(): JPanel {
    val openSshConfig = ActionLink(EmrMessagesBundle.message("aws.cluster.info.action.open.ssh")) {
      val id = id ?: return@ActionLink
      val cluster = dataManager.getClusterInfoModel(id).originObject ?: return@ActionLink
      if (cluster.isStopped) {
        NotificationUtils.showInfoMessage(project, EmrMessagesBundle.message("error.cluster.must.be.started"),
                                             EmrMessagesBundle.message("error.title"))
        return@ActionLink
      }
      dataManager.driver.safeExecutor.coroutineScope.launch {
        dataManager.sshManager.chooseFromUiConnectionData(project, cluster)
      }
    }


    val openSubnet = ActionLink(EmrMessagesBundle.message("aws.cluster.info.action.open.subnet")) {
      val id = id ?: return@ActionLink
      val ec2SubnetId = dataManager.getClusterInfoModel(id).originObject?.cluster?.ec2InstanceAttributes()?.ec2SubnetId()
                        ?: return@ActionLink
      BrowserUtil.browse("https://console.aws.amazon.com/vpc/home?region=${dataManager.region}#subnets:search=$ec2SubnetId")
    }

    val openMasterSecurityGroup = ActionLink(EmrMessagesBundle.message("aws.cluster.info.action.open.master.security.group")) {
      val id = id ?: return@ActionLink
      val cluster = dataManager.getClusterInfoModel(id).originObject?.cluster
      val masterSecGroup = cluster?.ec2InstanceAttributes()?.emrManagedMasterSecurityGroup() ?: return@ActionLink
      BrowserUtil.browse("https://console.aws.amazon.com/ec2/home?region=${dataManager.region}#SecurityGroups:search=$masterSecGroup")
    }

    val openSlaveSecurityGroup = ActionLink(EmrMessagesBundle.message("aws.cluster.info.action.open.slave.security.group")) {
      val id = id ?: return@ActionLink
      val cluster = dataManager.getClusterInfoModel(id).originObject?.cluster
      val slaveSecGroup = cluster?.ec2InstanceAttributes()?.emrManagedSlaveSecurityGroup() ?: return@ActionLink
      BrowserUtil.browse("https://console.aws.amazon.com/ec2/home?region=${dataManager.region}#SecurityGroups:search=$slaveSecGroup")
    }

    return JPanel(WrapLayout(FlowLayout.LEADING, JBUI.scale(10), JBUI.scale(10))).apply {
      add(openSshConfig)
      add(openSubnet)
      add(openMasterSecurityGroup)
      add(openSlaveSecurityGroup)
    }
  }


  override fun getFieldsGroupModel(id: String) = dataManager.getClusterInfoModel(id)
}