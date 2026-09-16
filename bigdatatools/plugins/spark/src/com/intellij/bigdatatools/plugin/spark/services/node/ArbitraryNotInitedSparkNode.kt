package com.intellij.bigdatatools.plugin.spark.services.node

import com.intellij.bigdatatools.coreUi.util.BdIdeRegistryUtil
import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.intellij.bigdatatools.plugin.spark.BigdatatoolsPluginSparkIcons
import com.intellij.bigdatatools.plugin.spark.arbitrary.ArbitraryClusterConnectionData
import com.intellij.bigdatatools.plugin.spark.arbitrary.ArbitraryClusterDataManager
import com.intellij.bigdatatools.plugin.spark.services.SparkJobServiceViewContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.platform.util.coroutines.childScope
import com.intellij.ssh.ui.unified.SshUiData
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.ActionLink
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.RowsRange
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.bigdatatools.common.rfs.driver.depend.MasterConnectionData
import com.jetbrains.bigdatatools.common.settings.ConnectionSettings
import com.jetbrains.bigdatatools.common.util.BdtActionsBundle
import com.jetbrains.bigdatatools.common.util.BdtSshUtils
import com.jetbrains.bigdatatools.common.util.ConnectionUtil
import com.jetbrains.bigdatatools.common.util.toPresentableText
import com.jetbrains.spark.monitoring.settings.SparkConnectionData
import com.jetbrains.spark.monitoring.util.SMMessagesBundle
import com.jetbrains.spark.submit.util.SparkMessagesBundle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

class ArbitraryNotInitedSparkNode(project: Project,
                                  parent: BdtBaseNode,
                                  connectionData: ArbitraryClusterConnectionData
) : BdtDriverNode(parent, project, connectionData) {
  override val label: String = connectionData.name
  override val icon = BigdatatoolsPluginSparkIcons.ArbitraryCluster

  private val dataManager: ArbitraryClusterDataManager
    get() = (driver?.dataManager as ArbitraryClusterDataManager)
  private val scope = driver?.safeExecutor?.coroutineScope?.childScope()

  private lateinit var errorMessage: Cell<SimpleColoredComponent>
  private lateinit var loadingRows: RowsRange
  private lateinit var errorRows: RowsRange

  private lateinit var errorFix: Cell<ActionLink>

  override fun dispose() {
    super.dispose()
    scope?.cancel("Disposed")
  }

  override fun getAllowsChildren(): Boolean = false
  override fun isLeaf(): Boolean = true
  override fun getChildren(): List<BdtDriverNode> = emptyList()

  private var initSparkConnectionJob: Job? = null

  override fun createContentController(): JComponent {
    if (!connData.isEnabled)
      return createConnectionDisabledPresentation()

    return panel {
      row {
        panel {
          loadingRows = rowsRange {
            row {
              val label = label(SMMessagesBundle.message("services.spark.creating")).align(AlignX.CENTER)
              label.component.icon = AnimatedIcon.Default()
            }
            row {
              link(SMMessagesBundle.message("services.spark.creating.cancel")) {
                initSparkConnectionJob?.cancel("By User")
                showErrorMessage(
                  SparkMessagesBundle.message("services.error.create.spark.connection.canceled.message"),
                  SparkMessagesBundle.message("services.error.create.spark.connection.canceled.fix.label")) {
                  initSparkConnectionJob = scope?.launch {
                    checkAndCreateConnection()
                  }
                }
              }.align(AlignX.CENTER)
            }
          }
          errorRows = rowsRange {
            row {
              @Suppress(
                "DialogTitleCapitalization") val label = label(
                SparkMessagesBundle.message("services.error.top.cannot.create.spark.connection")).align(AlignX.CENTER).resizableColumn()
              label.component.icon = AllIcons.General.Error
            }
            row {
              val component = SimpleColoredComponent()
              errorMessage = cell(component).align(AlignX.CENTER)
              errorMessage.component.colorModel
            }

            row {
              errorFix = link("") {}.align(AlignX.CENTER)
            }
          }.visible(false)
        }
      }.resizableRow()
      initSparkConnectionJob = scope?.launch {
        checkAndCreateConnection()
      }
    }
  }

  private suspend fun checkAndCreateConnection() {
    withContext(Dispatchers.EDT) {
      showLoading()
    }

    val sshConfig = dataManager.getSshConfig(project) ?: let {
      withContext(Dispatchers.EDT) {
        showErrorMessage(SparkMessagesBundle.message("services.error.ssh.config.is.not.found"),
                         BdtActionsBundle.message("action.BigDataTools.RfsOpenSettingsAction.text")) {
          ConnectionSettings.open(project, connData.innerId)
          parent?.refresh()
        }
      }
      return
    }

    try {
      withTimeout(BdIdeRegistryUtil.RFS_DEFAULT_TIMEOUT.toLong()) {
        BdtSshUtils.testConnectionOrThrow(SshUiData.create(sshConfig), project /*isCheckStopped*/)
      }
    }
    catch (ce: CancellationException) {
      throw ce
    }
    catch (t: Throwable) {
      withContext(Dispatchers.EDT) {
        showErrorMessage(t.message ?: t.toPresentableText(), BdtActionsBundle.message("action.BigDataTools.RfsOpenSettingsAction.text")) {
          ConnectionSettings.open(project, connData.innerId)
          parent?.refresh()
        }
      }
      return
    }
    withContext(Dispatchers.EDT) {
      dataManager.createSparkConnection(project) {
        parent?.refresh()
        SparkJobServiceViewContributor.Utils.getInstance()?.focusOnSparkApp(project, it, null, ignoreIfError = false)
      }
    }
  }

  override fun createConnectionDisabledPresentation() = panel {
    row {
      panel {
        row {
          text(MessagesBundle.message("services.panel.connection.disabled", connData.name)).align(AlignX.CENTER)
        }
        row {
          link(MessagesBundle.message("connection.enable")) {
            val sparkConnId = (connData as MasterConnectionData<*>)
              .getDependConnections(project)
              .firstOrNull { it is SparkConnectionData }
              ?.innerId

            val connections = listOfNotNull(connData.innerId, sparkConnId)
            ConnectionUtil.enableConnectionsByIds(project, connections)
          }.align(AlignX.CENTER)
        }
      }
    }.resizableRow()
  }

  private fun showErrorMessage(@Nls msg: String, @Nls fixAction: String, action: () -> Unit) {
    loadingRows.visible(false)
    errorRows.visible(true)

    errorMessage.component.clear()
    errorMessage.component.append(msg, SimpleTextAttributes.ERROR_ATTRIBUTES)

    errorFix.component.text = fixAction
    errorFix.component.actionListeners.toList().forEach {
      errorFix.component.removeActionListener(it)
    }
    errorFix.component.addActionListener {
      action()
    }
  }

  private fun showLoading() {
    loadingRows.visible(true)
    errorRows.visible(false)
  }
}