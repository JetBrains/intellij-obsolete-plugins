package com.intellij.bigdatatools.zeppelin.idea.toolwindow

import com.intellij.bigdatatools.zeppelin.file.ZeppelinFileType
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.bigdatatools.zeppelin.ztools.variableview.VariableView
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.serviceContainer.AlreadyDisposedException
import com.intellij.ui.components.JBPanelWithEmptyText
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManager
import com.intellij.util.ui.StatusText
import com.intellij.util.ui.UIUtil
import com.jetbrains.bigdatatools.common.util.invokeLater

/**
 * Manages StateViewer variable view tool window.
 */
object ZtoolsToolWindowUtils {
  private val logger = Logger.getInstance(this::class.java)
  private var shouldShowNotification = true

  // Only adds toolwindow StripeButton. (Here we have hack with hide(), because we have in new UI no possibility to show only stripe button)
  fun showToolWindowStripeButton(project: Project) = runInEdt {
    val toolWindow = getZeppelinToolWindow(project) ?: return@runInEdt
    toolWindow.isAvailable = true
    toolWindow.hide()
  }

  // Adds StripeButton and shows toolwindow.
  fun showToolWindow(project: Project) = runInEdt {
    val toolWindow = getZeppelinToolWindow(project) ?: return@runInEdt
    toolWindow.isAvailable = true
    toolWindow.show()
  }

  // Hides toolwindows and removes StripeButton.
  fun hideToolWindow(project: Project) = runInEdt {
    val toolWindow = getZeppelinToolWindow(project) ?: return@runInEdt
    toolWindow.isAvailable = false
    toolWindow.hide()
  }

  fun setVariableView(project: Project, variableView: VariableView) {
    val toolWindow = getZeppelinToolWindow(project) ?: return
    toolWindow.isAvailable = true
    val contentManager = toolWindow.contentManager
    val found = contentManager.contents.firstOrNull { it.displayName == variableView.name }
    if (found != null) {
      return
    }

    val content = contentManager.factory.createContent(variableView, variableView.name, true)
    content.isCloseable = false
    contentManager.addContent(content)

    contentManager.contents.forEach { if (it != content) contentManager.removeContent(it, false) }
  }

  // We should run this in UI thread, because of the special cases
  // https://youtrack.jetbrains.com/issue/BDIDE-3347
  // where this method called from non-UI thread.
  fun removeVariableView(project: Project, variableView: VariableView) = UIUtil.invokeLaterIfNeeded {
    val toolWindow = try {
      getZeppelinToolWindow(project)
    }
    catch (t: AlreadyDisposedException) {
      logger.info("Cannot remove variable view, tool window is disposed", t)
      null
    }
    val contentManager = toolWindow?.contentManager ?: return@invokeLaterIfNeeded
    val content = contentManager.contents.firstOrNull { it.displayName == variableView.name } ?: return@invokeLaterIfNeeded

    if (contentManager.contents.size <= 1) {
      contentManager.addContent(createEmptyContent(contentManager))
    }

    contentManager.removeContent(content, true)
  }

  fun showNotificationOpenZtools(project: Project) = runAfterProjectInit(project) {
    val toolWindow = getZeppelinToolWindow(project) ?: return@runAfterProjectInit
    if (ZtoolsGlobalSettings.getInstance().offerToShowSuggestionOpenPanel && !toolWindow.isVisible && shouldShowNotification) {
      val notification = Notification(ZeppelinFileType.name,
                                      ZepMessagesBundle.message("ztools.note.action.open.variable.view.title"),
                                      ZepMessagesBundle.message("ztools.note.action.open.variable.view.text"),
                                      NotificationType.INFORMATION)
      notification.addAction(DumbAwareAction.create(ZepMessagesBundle.message("ztools.note.action.open.variable.view.show")) {
        if (toolWindow.isDisposed)
          return@create

        toolWindow.show()
        notification.expire()
      })
      notification.addAction(DumbAwareAction.create(ZepMessagesBundle.message("ztools.note.action.open.variable.view.show.ignore")) {
        notification.expire()
        ZtoolsGlobalSettings.getInstance().offerToShowSuggestionOpenPanel = false
      })

      shouldShowNotification = false
      Notifications.Bus.notify(notification, project)
    }
  }

  /**
   * Creates panel with background color as editor background and empty state clickable label, leading to settings.
   * https://jetbrains.github.io/ui/principles/empty_state
   */
  fun createEmptyContent(contentManager: ContentManager): Content {
    val panel = JBPanelWithEmptyText()
    val emptyText = panel.emptyText
    val nonLinkAttributes = StatusText.DEFAULT_ATTRIBUTES

    emptyText.appendText(ZepMessagesBundle.message("toolwindow.empty.text"), nonLinkAttributes)
    emptyText.isShowAboveCenter = false

    return contentManager.factory.createContent(panel, "", true).apply {
      isCloseable = false
    }
  }

  private fun getZeppelinToolWindow(project: Project): ToolWindow? {
    if (project.isDisposed)
      return null
    return ToolWindowManager.getInstance(project).getToolWindow(ZtoolsToolWindowFactory.ID)
  }

  private fun runAfterProjectInit(project: Project, body: () -> Unit) = StartupManager.getInstance(project).runAfterOpened {
    invokeLater {
      body()
    }
  }
}