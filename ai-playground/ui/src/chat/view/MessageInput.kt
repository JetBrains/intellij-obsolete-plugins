package com.intellij.aiplayground.ui.chat.view

import com.intellij.aiplayground.ui.AIPlaygroundUIBundle
import com.intellij.aiplayground.ui.icons.AiplaygroundUIIcons
import com.intellij.icons.AllIcons
import com.intellij.ide.HelpTooltip
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.FocusChangeListener
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.EditorTextField
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.KeyStroke

class MessageInput(
  project: Project,
  text: String = "",
  val onUpdateMessage: ((String) -> Unit),
  val onSendMessage: ((String) -> Unit),
  val onStop: (() -> Unit),
) : JPanel(BorderLayout()), Disposable {

  var isStreaming: Boolean = false

  var text: String
    get() = textArea.text
    set(value) {
      textArea.text = value
    }

  private val textArea: EditorTextField

  private val roundedBorder = createRoundedBorder()

  init {
    isOpaque = false
    textArea = createTextArea()
    textArea.text = text
    textArea.setPlaceholder(AIPlaygroundUIBundle.message("input.placeholder.text"))
    border = roundedBorder
    val mainPanel = JPanel(BorderLayout()).apply {
      border = JBUI.Borders.empty()
      isOpaque = false
      add(textArea, BorderLayout.CENTER)
    }
    add(mainPanel, BorderLayout.CENTER)
    val documentListener = object : DocumentListener {
      override fun documentChanged(event: DocumentEvent) {
        if (textArea.preferredSize.height != textArea.height) {
          revalidate()
        }
        onUpdateMessage(textArea.text)
      }
    }
    textArea.document.addDocumentListener(documentListener, this)
    DumbAwareAction.create {
      textArea.editor?.let {
        CommandProcessor.getInstance().executeCommand(project, {
          ApplicationManager.getApplication().runWriteAction {
            val eol = "\n"
            val caretOffset = it.caretModel.offset
            textArea.document.insertString(caretOffset, eol)
            it.caretModel.moveToOffset(caretOffset + eol.length)
            textArea.editor?.getScrollingModel()?.scrollToCaret(ScrollType.RELATIVE)
          }
        }, null, null)
      }
    }.registerCustomShortcutSet(CustomShortcutSet(
      KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), null),
      KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), null)
    ), textArea)

    DumbAwareAction.create {
      sendMessage()
    }.registerCustomShortcutSet(KeyEvent.VK_ENTER, 0, textArea)
  }

  private fun createTextArea(): EditorTextField = EditorTextField().apply {
    border = JBUI.Borders.empty()
    setOneLineMode(false)
    isOpaque = false
    setFontInheritedFromLAF(true)
    ensureWillComputePreferredSize()
    setDisposedWith(this@MessageInput)
    addSettingsProvider {
      it.colorsScheme.lineSpacing = 1f
      it.settings.isUseSoftWraps = true
      it.settings.isPaintSoftWraps = false
      it.isEmbeddedIntoDialogWrapper = true
      border = JBUI.Borders.empty()
      it.setVerticalScrollbarVisible(true)
      it.scrollPane.verticalScrollBar.isOpaque = false
      it.addFocusListener(object : FocusChangeListener {
        override fun focusGained(editor: Editor) = roundedBorder.applyFocused(true, textArea)
        override fun focusLost(editor: Editor) = roundedBorder.applyFocused(false, textArea)
      })
    }
  }

  fun sendMessage() {
    val message = textArea.text.trim()
    onSendMessage(message)
  }

  fun replaceBottomToolbar(toolbar: JComponent) {
    (layout as BorderLayout).getLayoutComponent(BorderLayout.SOUTH)?.let { old ->
      remove(old)
    }
    add(toolbar, BorderLayout.SOUTH)
  }

  fun addDefaultBottomToolbar() {
    val service = ActionManager.getInstance()
    val buttonPanel = service.createActionToolbar("MessageInputToolbar", DefaultActionGroup(
      object : AnAction(AiplaygroundUIIcons.Send) {
        override fun actionPerformed(e: AnActionEvent) {
          sendMessage()
        }

        override fun update(e: AnActionEvent) {
          e.presentation.isEnabledAndVisible = !isStreaming
          e.presentation.putClientProperty(ActionButton.CUSTOM_HELP_TOOLTIP, HelpTooltip().apply {
            setTitle(AIPlaygroundUIBundle.message("tooltip.title.send"))
            setShortcut("Enter")
          })
        }

        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
      },
      object : AnAction(AllIcons.Run.Stop) {
        override fun actionPerformed(e: AnActionEvent) {
          onStop()
        }

        override fun update(e: AnActionEvent) {
          e.presentation.isEnabledAndVisible = isStreaming
        }

        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
      }
    ), true)
    buttonPanel.component.apply {
      isOpaque = false
      border = JBUI.Borders.empty()
    }
    buttonPanel.targetComponent = this
    replaceBottomToolbar(JPanel(BorderLayout()).apply {
      isOpaque = false
      border = JBUI.Borders.empty()
      add(buttonPanel.component, BorderLayout.EAST)
    })
  }

  override fun dispose() {
  }

  fun getPreferredFocusedComponent(): JComponent {
    return textArea
  }

  override fun removeNotify() {
    super.removeNotify()
    Disposer.dispose(this)
  }
}