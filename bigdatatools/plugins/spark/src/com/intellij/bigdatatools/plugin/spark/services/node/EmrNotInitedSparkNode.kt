package com.intellij.bigdatatools.plugin.spark.services.node

import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionData
import com.intellij.bigdatatools.emr.data.EmrDataManager
import com.intellij.bigdatatools.emr.model.EmrClusterDetails
import com.intellij.bigdatatools.emr.model.EmrClusterInfo
import com.intellij.bigdatatools.emr.rfs.EmrRfsTreeNode
import com.intellij.bigdatatools.plugin.spark.services.SparkJobServiceViewContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.platform.util.coroutines.childScope
import com.intellij.ssh.ui.unified.SshUiData
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.RowsRange
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.bigdatatools.common.util.BdtSshUtils
import com.jetbrains.bigdatatools.common.util.toPresentableText
import com.jetbrains.spark.monitoring.util.SMMessagesBundle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JComponent

class EmrNotInitedSparkNode(project: Project,
                            parent: EmrDriverNode,
                            connectionData: ConnectionData,
                            private val clusterSummary: EmrClusterInfo
) : BdtDriverNode(parent, project, connectionData) {
  override val label: String = clusterSummary.name
  override val icon = EmrRfsTreeNode.getIconForCluster(clusterSummary)

  private val isCheckStopped = AtomicBoolean(false)

  private val dataManager = (parent.driver?.dataManager as EmrDataManager)
  private val scope = dataManager.driver.safeExecutor.coroutineScope.childScope()

  private lateinit var errorMessage: Cell<SimpleColoredComponent>
  private lateinit var loadingRows: RowsRange
  private lateinit var errorTunnelRows: RowsRange

  private var cachedCluster: EmrClusterDetails? = null
  override fun dispose() {
    super.dispose()
    scope.cancel("Disposed")

    isCheckStopped.set(false)
  }

  override fun getAllowsChildren(): Boolean = false
  override fun isLeaf(): Boolean = true
  override fun getChildren(): List<BdtDriverNode> = emptyList()

  override fun createContentController(): JComponent = panel {
    row {
      panel {
        loadingRows = rowsRange {
          row {
            val label = label(SMMessagesBundle.message("services.spark.creating")).align(AlignX.CENTER)
            label.component.icon = AnimatedIcon.Default()
          }
          row {
            link(SMMessagesBundle.message("services.spark.creating.cancel")) {
              isCheckStopped.set(true)
            }.align(AlignX.CENTER)
          }
        }
        errorTunnelRows = rowsRange {
          row {
            val label = label(SMMessagesBundle.message("services.spark.tunnel.error")).align(AlignX.CENTER).resizableColumn()
            label.component.icon = AllIcons.General.Error
          }
          row {
            errorMessage = cell(SimpleColoredComponent()).align(AlignX.CENTER)
            errorMessage.component.colorModel
          }

          row {
            link(SMMessagesBundle.message("services.spark.fix.connection")) {
              scope.launch {
                val cluster = getOrLoadCLuster()
                dataManager.sshManager.chooseFromUiConnectionData(project, cluster) ?: return@launch
                checkAndCreateConnection()
              }
            }.align(AlignX.CENTER)
          }
        }.visible(false)
      }
    }.resizableRow()
    scope.launch {
      checkAndCreateConnection()
    }
  }

  private suspend fun checkAndCreateConnection() {
    withContext(Dispatchers.EDT) {
      errorTunnelRows.visible(false)
      loadingRows.visible(true)
    }

    val cluster = getOrLoadCLuster()
    val sshConfig = dataManager.sshManager.getOrCreateSshConfig(project, cluster)
    isCheckStopped.set(false)
    val (success, message) = try {
      withTimeout(20000) {
        // TODO nashikhmin please replace isCheckStopped with proper cancellation, also here are a lot of duplicated code and hanging launches
        BdtSshUtils.testConnectionAndWrapResult(SshUiData.create(sshConfig), project /*isCheckStopped*/)
      }
    }
    catch (ce: CancellationException) {
      throw ce
    }
    catch (t: Throwable) {
      false to (t.message ?: t.toPresentableText())
    }
    if (success) {
      dataManager.createSparkConnection(project, cluster) {
        parent?.refresh()
        SparkJobServiceViewContributor.Utils.getInstance()?.focusOnSparkApp(project, it, null, ignoreIfError = false)
      }
      withContext(Dispatchers.EDT) {
        loadingRows.visible(false)
        errorTunnelRows.visible(false)
        errorMessage.component.clear()
      }
    }
    else {
      withContext(Dispatchers.EDT) {
        errorMessage.component.clear()
        @Suppress("HardCodedStringLiteral") var messageShort = message.take(150)
        if (messageShort.length != message.length) {
          messageShort += "..."
        }
        errorMessage.component.append(messageShort, SimpleTextAttributes.ERROR_ATTRIBUTES)


        errorTunnelRows.visible(true)
        loadingRows.visible(false)
      }
    }
  }

  private suspend fun getOrLoadCLuster(): EmrClusterDetails {
    val cluster = cachedCluster ?: withContext(Dispatchers.IO) {
      dataManager.loadClusterById(clusterSummary.id)
    }

    cachedCluster = cluster
    return cluster
  }
}