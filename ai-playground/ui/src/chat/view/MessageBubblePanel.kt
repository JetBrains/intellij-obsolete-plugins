package com.intellij.aiplayground.ui.chat.view

import com.intellij.aiplayground.models.chat.AssistantMessage
import com.intellij.aiplayground.models.chat.ChatMessage
import com.intellij.aiplayground.models.chat.UserMessage
import com.intellij.aiplayground.ui.AIPlaygroundColors
import com.intellij.aiplayground.ui.AIPlaygroundUIBundle
import com.intellij.aiplayground.ui.utils.getModelIcon
import com.intellij.aiplayground.ui.utils.getModelName
import com.intellij.icons.AllIcons
import com.intellij.ide.ActivityTracker
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.platform.util.coroutines.childScope
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.panels.BackgroundRoundedPanel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.JLabelUtil
import com.intellij.util.ui.SwingTextTrimmer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Font
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

private val formattedViewProvider = HtmlFormattedViewProvider()

interface MessageBubble<T : ChatMessage> {

  fun updateContent(message: T)

}

abstract class MessageBubbleComponent<T : ChatMessage>(protected val project: Project) : JComponent(), MessageBubble<T> {

  protected var message: T? = null

  protected val header: JPanel
  protected val footer: JPanel
  private var headerLabel: JComponent? = null
  protected val panel: JPanel
  protected val messageLabel: FormattedView

  init {
    isOpaque = false
    layout = BorderLayout()

    header = JPanel(BorderLayout())
    header.border = JBUI.Borders.emptyBottom(12)
    header.isOpaque = false

    footer = JPanel(BorderLayout())
    footer.border = JBUI.Borders.emptyTop(12)
    footer.isOpaque = false

    panel = BackgroundRoundedPanel(12)
    panel.layout = BorderLayout()

    messageLabel = formattedViewProvider.createView(project)
    panel.border = JBUI.Borders.empty(8)
    panel.add(messageLabel.component, BorderLayout.CENTER)
    panel.add(header, BorderLayout.NORTH)
    panel.add(footer, BorderLayout.SOUTH)
    add(panel, BorderLayout.CENTER)
  }

  protected fun updateContent(text: String) {
    messageLabel.updateText(text)
  }

  override fun setBackground(bg: Color?) {
    panel.background = bg
  }

  override fun getBackground(): Color? {
    return panel.background
  }

  protected fun setActionGroup(group: ActionGroup?) {
    if (group == null) return
    val actionToolbar = ActionManager.getInstance().createActionToolbar("MessageBubbleToolbar", group, true)
    actionToolbar.setTargetComponent(this)
    actionToolbar.component.isOpaque = false
    actionToolbar.component.border = JBUI.Borders.empty()
    header.add(actionToolbar.component, BorderLayout.EAST)
  }

  protected fun setHeaderLabel(label: JLabel?) {
    if (headerLabel != null) {
      header.remove(headerLabel)
      headerLabel = null
    }
    if (label != null) {
      header.add(label, BorderLayout.CENTER)
    }
    headerLabel = label
    header.revalidate()
  }

  protected fun setFooter(label: JLabel?) {
    footer.removeAll()
    if (label != null) {
      footer.add(label, BorderLayout.CENTER)
    }
    footer.revalidate()
  }
}

open class EditableMessageBubble<M : ChatMessage>(
  private val project: Project,
  private val onSendClick: (String) -> Unit,
  private val onCancelClick: () -> Unit,
  private val componentProvider: () -> JComponent,
) : JComponent(), MessageBubble<M> {

  private var message: M? = null

  init {
    isOpaque = false
    layout = BorderLayout()
    endEdit()
  }

  fun startEdit(withText: String? = null) {
    removeAll()
    val historyWillBeRegenerated = message is UserMessage
    val messageInput = MessageInput(project, withText ?: message?.content ?: "", {}, onSendClick, {})
    messageInput.replaceBottomToolbar(
      JPanel(BorderLayout()).apply {
        isOpaque = false
        border = JBUI.Borders.empty()
        if (historyWillBeRegenerated) {
          add(JBLabel(AIPlaygroundUIBundle.message("label.editing.this.message.will.restart.conversation.will.from.this.point")).apply {
            JLabelUtil.setTrimOverflow(this, true)
            putClientProperty(SwingTextTrimmer.KEY, SwingTextTrimmer.THREE_DOTS_AT_RIGHT)
            foreground = JBUI.CurrentTheme.ContextHelp.FOREGROUND
            toolTipText = AIPlaygroundUIBundle.message("tooltip.editing.this.message.will.restart.conversation.will.from.this.point")
          }, BorderLayout.CENTER)
        }
        add(JPanel().apply {
          isOpaque = false
          border = JBUI.Borders.empty()
          layout = BoxLayout(this, BoxLayout.X_AXIS)
          add(JButton(AIPlaygroundUIBundle.message("cancel")).apply {
            isOpaque = false
            addActionListener {
              onCancelClick()
              endEdit()
            }
          })
          val saveText = if (historyWillBeRegenerated) AIPlaygroundUIBundle.message("send") else AIPlaygroundUIBundle.message("save")
          add(JButton(saveText).apply {
            isOpaque = false
            addActionListener {
              messageInput.sendMessage()
              endEdit()
            }
          })
        }, BorderLayout.EAST)
      }
    )
    add(messageInput, BorderLayout.CENTER)
    revalidate()
    repaint()
  }

  fun endEdit() {
    removeAll()
    val component = componentProvider()
    add(component, BorderLayout.CENTER)
    message?.let { (component as? MessageBubble<M>)?.updateContent(it) }
    revalidate()
    repaint()
  }

  override fun updateContent(message: M) {
    this.message = message
    components.firstOrNull()?.let {
      (it as? MessageBubble<M>)?.updateContent(message)
    }
  }
}

class EditableUserMessageBubble(
  project: Project,
  onCancelClick: () -> Unit,
  onSendClick: (String) -> Unit,
  onEditClick: () -> Unit,
) : EditableMessageBubble<UserMessage>(project, onSendClick, onCancelClick, { UserMessageBubble(project, onEditClick) })

class EditableAssistantUserMessageBubble(
  project: Project,
  onCancelClick: () -> Unit,
  onSendClick: (String) -> Unit,
  onEditClick: () -> Unit,
  onRegenerateClick: () -> Unit,
  coroutineScope: CoroutineScope,
) : EditableMessageBubble<AssistantMessage>(project, onSendClick, onCancelClick, { AssistantMessageBubble(onEditClick, onRegenerateClick, coroutineScope, project) })


class UserMessageBubble(
  project: Project,
  private val onEditClick: () -> Unit,
) : MessageBubbleComponent<UserMessage>(project) {

  override fun updateContent(message: UserMessage) {
    this.message = message
    messageLabel.updateText(message.content)
  }

  private val actions = arrayOf<AnAction>(
    object : AnAction(AllIcons.Actions.Edit) {
      override fun actionPerformed(e: AnActionEvent) {
        onEditClick()
      }
    },
  )

  init {
    background = AIPlaygroundColors.USER_MESSAGE_BACKGROUND_COLOR
    setActionGroup(object : ActionGroup("UserMessageBubbleActions", true) {
      override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        return actions
      }
    })
    messageLabel.component.addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        if (e.clickCount == 2) {
          onEditClick()
        }
      }
    })
    setHeaderLabel(JLabel(AIPlaygroundUIBundle.message("me")))
  }
}

class AssistantMessageBubble(
  private val onEditClick: () -> Unit,
  private val onRegenerateClick: () -> Unit,
  private val parentScope: CoroutineScope,
  project: Project,
) : MessageBubbleComponent<AssistantMessage>(project) {

  private var coroutineScope: CoroutineScope? = null

  override fun updateContent(message: AssistantMessage) {
    this.message = message
    messageFlow.value = message
    updateContent(message.content)
    footerLabel.text = message.tokenUsage?.let { tokenUsage ->
      AIPlaygroundUIBundle.message("tokens.count.input.output.0.1", tokenUsage.inputTokenCount, tokenUsage.outputTokenCount)
    } ?: ""
    ActivityTracker.getInstance().inc()
  }

  private val label = JLabel("").apply {
    font = font.deriveFont(Font.BOLD)
  }
  private val messageFlow = MutableStateFlow<AssistantMessage?>(null)

  private val footerLabel = JLabel("").apply {
    foreground = JBUI.CurrentTheme.Label.disabledForeground()
    horizontalAlignment = SwingConstants.RIGHT
  }

  private val actions = arrayOf(
    object : AnAction(AllIcons.Actions.Copy) {
      override fun actionPerformed(e: AnActionEvent) {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(StringSelection(message?.content ?: ""), null)
      }
    },
    object : AnAction(AllIcons.Actions.Edit) {
      override fun actionPerformed(e: AnActionEvent) {
        onEditClick()
      }
    },
    object : AnAction(AllIcons.Actions.Refresh) {

      override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

      override fun update(e: AnActionEvent) {
        if (message?.isStreaming ?: false) {
          e.presentation.disabledIcon = AnimatedIcon.Default()
          e.presentation.isEnabled = false
        }
        else {
          e.presentation.icon = AllIcons.Actions.Refresh
          e.presentation.isEnabled = true
        }
      }

      override fun actionPerformed(e: AnActionEvent) {
        onRegenerateClick()
      }
    }
  )

  init {
    background = AIPlaygroundColors.ASSISTANT_MESSAGE_BACKGROUND_COLOR
    setHeaderLabel(label)
    setActionGroup(object : ActionGroup("AssistantMessageBubbleActions", false) {
      override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        return actions
      }
    })
    setFooter(footerLabel)
  }

  override fun addNotify() {
    super.addNotify()
    coroutineScope = parentScope.childScope("AssistantMessageBubbleScope")
    coroutineScope?.launch {
      messageFlow.filterNotNull()
        .flatMapConcat {
          getModelName(project, it.model.instanceId, it.model.modelId)
        }.collectLatest {
          label.text = it
        }
    }
    coroutineScope?.launch {
      messageFlow.filterNotNull()
        .flatMapConcat {
          getModelIcon(project, it.model.instanceId, it.model.modelId)
        }.collectLatest {
          label.icon = it
        }
    }
  }

  override fun removeNotify() {
    super.removeNotify()
    coroutineScope?.cancel()
    coroutineScope = null
  }
}

