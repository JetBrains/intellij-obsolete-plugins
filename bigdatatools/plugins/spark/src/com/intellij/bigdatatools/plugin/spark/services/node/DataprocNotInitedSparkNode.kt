package com.intellij.bigdatatools.plugin.spark.services.node

import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionData
import com.intellij.bigdatatools.plugin.spark.services.SparkJobServiceViewContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.platform.util.coroutines.childScope
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.RowsRange
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.bigdatatools.common.util.toPresentableText
import com.jetbrains.bigdatatools.dataproc.data.DataprocDataManager
import com.jetbrains.bigdatatools.dataproc.dependend.drivers.DataprocDependsManager
import com.jetbrains.bigdatatools.dataproc.model.DataprocClusterInfo
import com.jetbrains.bigdatatools.dataproc.util.DataprocMessagesBundle
import com.jetbrains.spark.monitoring.util.SMMessagesBundle
import com.jetbrains.spark.submit.util.SparkMessagesBundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JComponent
import javax.swing.JLabel

class DataprocNotInitedSparkNode(project: Project,
                                 parent: DataprocDriverNode,
                                 connectionData: ConnectionData,
                                 private val clusterInfo: DataprocClusterInfo
) : BdtDriverNode(parent, project, connectionData) {
  override val label: String = clusterInfo.name
  override val icon = clusterInfo.icon

  private val isCheckStopped = AtomicBoolean(false)

  private val dataManager = (parent.driver?.dataManager as DataprocDataManager)
  private val scope = dataManager.driver.safeExecutor.coroutineScope.childScope()

  private lateinit var errorMessage: Cell<SimpleColoredComponent>
  private lateinit var loadingRows: RowsRange
  private lateinit var loadingLabel: Cell<JLabel>
  private lateinit var errorTunnelRows: RowsRange

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
            loadingLabel = label(SMMessagesBundle.message("services.spark.creating")).align(AlignX.CENTER)
            loadingLabel.component.icon = AnimatedIcon.Default()
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

    val cluster = clusterInfo

    isCheckStopped.set(false)
    val (success, message) = try {
      withTimeout(20000) {
        val appInfo = DataprocDependsManager.getSparkApp(cluster) ?: return@withTimeout false to DataprocMessagesBundle.message(
          "error.spark.is.not.found")
        withContext(Dispatchers.EDT) {
          loadingLabel.component.text = SparkMessagesBundle.message("check.connection.availability")
        }
        dataManager.dependsManager.checkAppConnectAvailable(project, cluster, appInfo)
      }
    }
    catch (t: Throwable) {
      false to (t.message ?: t.toPresentableText())
    }

    if (success) {
      withContext(Dispatchers.EDT) {
        loadingLabel.component.text = SMMessagesBundle.message("services.spark.creating")
      }
      val connectionData = dataManager.createSparkConnection(project, cluster)
      if (connectionData == null) {
        withContext(Dispatchers.EDT) {
          errorTunnelRows.visible(true)
          loadingRows.visible(false)
          errorMessage.component.clear()
          errorMessage.component.append(SparkMessagesBundle.message("services.spark.connection.is.not.created"),
                                        SimpleTextAttributes.ERROR_ATTRIBUTES)
        }
        return
      }
      SparkJobServiceViewContributor.Utils.getInstance()?.focusOnSparkApp(project, connectionData, null, ignoreIfError = false)
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
}