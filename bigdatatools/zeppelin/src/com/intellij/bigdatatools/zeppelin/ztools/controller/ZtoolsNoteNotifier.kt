package com.intellij.bigdatatools.zeppelin.ztools.controller

import com.intellij.bigdatatools.coreUi.util.NotificationUtils
import com.intellij.bigdatatools.notebooks.core.impl.nbformat.CellResultMessage
import com.intellij.bigdatatools.notebooks.core.impl.nbformat.CellResultType
import com.intellij.bigdatatools.zeppelin.components.containers.service.ZeppelinNoteCacheConnection
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.util.NlsSafe
import com.intellij.ui.components.JBScrollPane
import java.awt.Dimension
import javax.swing.JTextArea
import kotlin.math.min

class ZtoolsNoteNotifier(val project: Project,
                         val cacheConnection: ZeppelinNoteCacheConnection,
                         private val noteName: String) : Disposable {
  private var notification: Notification? = null

  override fun dispose() {
    notification?.expire()
  }

  fun showZtoolsErrorNotification(messages: List<CellResultMessage>) {
    notification?.expire()
    notification = createZtoolsErrorNotification(messages)
    notification?.let { Notifications.Bus.notify(it, project) }
  }

  private fun createZtoolsErrorNotification(messages: List<CellResultMessage>): Notification {
    val fullErrorText = messages.filter { it.type == CellResultType.TEXT }.joinToString("\n") { it.data }

    val notification = Notification(NotificationUtils.ACTION_FAILURE_NOTIFICATION_GROUP,
                                    ZepMessagesBundle.message("ztools.error.title", noteName),
                                    ZepMessagesBundle.message("ztools.error.content"),
                                    NotificationType.WARNING)

    val showErrorMessageAction = createShowErrorAction(fullErrorText, notification)

    notification.addAction(showErrorMessageAction)
    return notification
  }

  private fun createShowErrorAction(fullErrorText: String, notification: Notification): DumbAwareAction =
    object : DumbAwareAction(ZepMessagesBundle.message("ztools.show.error.action")) {
      override fun actionPerformed(e: AnActionEvent) = try {
        createStacktraceMessage(fullErrorText)
      }
      finally {
        notification.expire()
      }
    }

  private fun createStacktraceMessage(@NlsSafe fullErrorText: String) {
    val stackEditor = JTextArea().apply {
      caret.isVisible = false
      lineWrap = false
      text = fullErrorText
      isEditable = false
      caretPosition = 0
    }

    val builder = DialogBuilder()
    builder.addOkAction()
    builder.setCenterPanel(JBScrollPane(stackEditor).apply {
      preferredSize = Dimension(min(stackEditor.preferredSize.width, 450), min(stackEditor.preferredSize.height, 400))
    })
    builder.setTitle(ZepMessagesBundle.message("ztools.stacktrace.error.title"))
    builder.show()
  }
}