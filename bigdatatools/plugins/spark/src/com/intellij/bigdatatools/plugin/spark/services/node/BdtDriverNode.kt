package com.intellij.bigdatatools.plugin.spark.services.node

import com.intellij.execution.services.ServiceViewDescriptor
import com.intellij.execution.services.SimpleServiceViewDescriptor
import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.project.Project
import com.intellij.ui.ClientProperty
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionData
import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.jetbrains.bigdatatools.common.monitoring.rfs.MonitoringDriver
import com.jetbrains.bigdatatools.common.rfs.driver.manager.DriverManager
import com.jetbrains.bigdatatools.common.services.BdtServiceViewDescriptor
import com.jetbrains.bigdatatools.common.settings.ConnectionSettings
import com.jetbrains.bigdatatools.common.util.BdtActionsBundle
import com.jetbrains.bigdatatools.common.util.ConnectionUtil
import org.jetbrains.annotations.Nls
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JComponent

abstract class BdtDriverNode(parent: BdtBaseNode,
                             project: Project,
                             val connData: ConnectionData) : BdtBaseNode(parent, project) {
  protected val isDisposed = AtomicBoolean(false)
  val driver: MonitoringDriver?
    get() = DriverManager.getDriverById(project, connData.innerId) as? MonitoringDriver

  protected val content by lazy {
    val controller = createContentController()
    ClientProperty.put(controller, ServiceViewDescriptor.ACTION_HOLDER_KEY, true)
    controller
  }

  override fun dispose() {
    isDisposed.set(true)
  }

  open val greyText: String? = null

  override fun getServiceDescriptor(project: Project, service: BdtBaseNode) = service.getNodeViewDescriptor(project)

  override fun getNodeViewDescriptor(project: Project) = object : BdtServiceViewDescriptor() {
    override fun getContentComponent() = content
    override fun getPresentation(): PresentationData {
      val textAttributesKey = if (connData.isEnabled) null
      else
        DefaultLanguageHighlighterColors.LINE_COMMENT
      return PresentationData(label, greyText, icon, textAttributesKey)
    }

    override fun getToolbarActions() = null

    override fun getPopupActions() = ActionManager.getInstance().getAction("BigDataTools.ServicesActionGroup") as? ActionGroup
  }


  protected open fun createContentController(): JComponent {
    if (!connData.isEnabled)
      return createConnectionDisabledPresentation()

    val controller = driver?.getController(project)

    return controller?.createMainController(connData)?.getComponent()
           ?: return createErrorPresentation(MessagesBundle.message("services.panel.connection.error.unexpected"))
  }


  protected open fun createConnectionDisabledPresentation() = panel {
    row {
      panel {
        row {
          text(MessagesBundle.message("services.panel.connection.disabled", connData.name)).align(AlignX.CENTER)
        }
        row {
          link(MessagesBundle.message("connection.enable")) {
            ConnectionUtil.enableIfDisabled(project, connData.innerId)
          }.align(AlignX.CENTER)
        }
      }
    }.resizableRow()
  }

  protected fun createErrorPresentation(@Nls text: String) = panel {
    row {
      panel {
        row { text(text).align(AlignX.CENTER) }
        row {
          @Suppress("DialogTitleCapitalization")
          link(BdtActionsBundle.message("action.BigDataTools.RfsOpenSettingsAction.text")) {
            ConnectionSettings.open(project, connData.innerId)
          }.align(AlignX.CENTER)
        }
      }
    }.resizableRow()
  }


  override fun getViewDescriptor(project: Project) = SimpleServiceViewDescriptor(label, null)

}