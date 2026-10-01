package com.intellij.aiplayground.ui

import com.intellij.aiplayground.models.chat.Chat
import com.intellij.aiplayground.models.chat.ChatRepository
import com.intellij.aiplayground.models.statistic.PlaygroundCollector
import com.intellij.aiplayground.models.utils.AiPlaygroundCoroutine
import com.intellij.aiplayground.ui.actions.CHAT
import com.intellij.aiplayground.ui.actions.CHAT_LIST
import com.intellij.aiplayground.ui.actions.RemoveChat
import com.intellij.aiplayground.ui.actions.RemoveChatDialog
import com.intellij.aiplayground.ui.actions.RenameChat
import com.intellij.aiplayground.ui.chat.ChatUiProvider
import com.intellij.aiplayground.ui.chat.actions.CreateNewChatAction
import com.intellij.aiplayground.ui.chat.view.panel
import com.intellij.aiplayground.ui.history.ChatsHistoryViewModel
import com.intellij.aiplayground.ui.icons.AiplaygroundUIIcons
import com.intellij.aiplayground.ui.utils.getChatName
import com.intellij.aiplayground.ui.utils.isChinaRegion
import com.intellij.icons.AllIcons
import com.intellij.ide.DataManager
import com.intellij.ide.ui.laf.darcula.ui.DarculaButtonUI.DEFAULT_STYLE_KEY
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.UiDataProvider
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.impl.content.ToolWindowContentUi
import com.intellij.platform.util.coroutines.childScope
import com.intellij.ui.CommonActionsPanel
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.listCellRenderer.LcrInitParams
import com.intellij.ui.dsl.listCellRenderer.listCellRenderer
import com.intellij.util.IconUtil
import com.intellij.util.text.DateFormatUtil
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Component
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BoxLayout
import javax.swing.Icon
import javax.swing.JPanel
import javax.swing.SwingUtilities

/**
 * Factory for creating the AI Playground toolwindow.
 */
class AIPlaygroundToolWindowFactory : ToolWindowFactory {

  override val icon: Icon
    get() = AiplaygroundUIIcons.ToolWindowAIPlayground_20x20

  override fun isDumbAware(): Boolean {
    return true
  }

  override suspend fun isApplicableAsync(project: Project): Boolean {
    return !isChinaRegion()
  }

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    val contentFactory = ContentFactory.getInstance()
    val coroutineScope = service<AiPlaygroundCoroutine>().coroutineScope
    val chatHistoryViewModel = ChatsHistoryViewModel(project, coroutineScope)
    val viewScope = coroutineScope.childScope("AIPlaygroundToolWindowFactory")

    val list = JBList<Chat>()
    list.cellRenderer = listCellRenderer<Chat> {
      text(getChatName(value.title)) {
        align = LcrInitParams.Align.LEFT
        speedSearch {}
      }
      text(DateFormatUtil.formatPrettyDateTime(value.updatedAt)) {
        foreground = greyForeground
      }
    }
    list.addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        if (e.clickCount == 2) {
          val selectedIndex = list.locationToIndex(e.point)
          if (selectedIndex >= 0) {
            val selectedChat = list.model.getElementAt(selectedIndex)
            service<AiPlaygroundCoroutine>().coroutineScope.launch {
              project.service<ChatUiProvider>().openChat(selectedChat)
            }
          }
        }
      }
    })
    list.addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        if (SwingUtilities.isRightMouseButton(e)) {
          val popup = JBPopupFactory.getInstance().createActionGroupPopup(
            null,
            DefaultActionGroup(
              listOf(
                RenameChat(),
                RemoveChat(),
              )
            ),
            DataManager.getInstance().getDataContext(list),
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            true,
          )
          popup.show(RelativePoint(list, e.point))
        }
      }
    })
    
    object : DumbAwareAction() {
      override fun actionPerformed(e: AnActionEvent) {
        val selectedValues = list.selectedValuesList
        if (RemoveChatDialog(project, selectedValues).showAndGet()) {
          selectedValues.forEach {chat ->
            project.service<ChatRepository>().deleteChat(chat.id)
            PlaygroundCollector.logChatRemoved()
          }
        }
      }

      init {
        registerCustomShortcutSet(CommonActionsPanel.getCommonShortcut(CommonActionsPanel.Buttons.REMOVE), list)
      }
    }

    val panel = JPanel(CardLayout())
    val listRoot = JBScrollPane(UiDataProvider.wrapComponent(list) {
      it[CHAT_LIST] = list.selectedValuesList
      it[CHAT] = list.selectedValue
    }).apply {
      border = JBUI.Borders.empty()
    }
    val iconColor = AIPlaygroundColors.TOOL_WINDOW_ICON_COLOR
    val emptyRoot = JPanel(BorderLayout()).apply {
      add(JPanel().apply {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        border = JBUI.Borders.empty(32)
        add(panel {
          row {
            label(AIPlaygroundUIBundle.message("label.welcome.to.ai.playground")) {
              it.font = font.deriveFont(JBUI.scale(16).toFloat())
            }
          }
          row {
            cell(panel {
              row {
                icon(IconUtil.colorize(AllIcons.Nodes.Plugin, iconColor))
                text(AIPlaygroundUIBundle.message("label.create.new.playground"))
              }
              row {
                placeholder()
                text(AIPlaygroundUIBundle.message("label.connect.your.own.openai.anthropic.or.mistral.api.tokens")) {
                  it.foreground = JBUI.CurrentTheme.ContextHelp.FOREGROUND
                }
              }
              row {
                icon(IconUtil.colorize(AllIcons.Actions.OfflineMode, iconColor))
                text(AIPlaygroundUIBundle.message("label.run.local.models.with.ollama"))
              }
              row {
                placeholder()
                text(AIPlaygroundUIBundle.message("label.use.offline.llms.locally.for.secure.or.private.workflows")) {
                  it.foreground = JBUI.CurrentTheme.ContextHelp.FOREGROUND
                }
              }
              row {
                icon(IconUtil.colorize(AllIcons.Actions.Diff, iconColor))
                text(AIPlaygroundUIBundle.message("label.compare.multiple.models"))
              }
              row {
                placeholder()
                text(AIPlaygroundUIBundle.message("label.prompt.up.to.five.models.at.once.compare.responses.side.by.side")) {
                  it.foreground = JBUI.CurrentTheme.ContextHelp.FOREGROUND
                }
              }
              row {
                icon(IconUtil.colorize(AiplaygroundUIIcons.AudioToAudio, iconColor))
                text(AIPlaygroundUIBundle.message("label.tune.your.prompts"))
              }
              row {
                placeholder()
                text(AIPlaygroundUIBundle.message("label.customize.temperature.max.tokens.top.p.system.prompts")) {
                  it.foreground = JBUI.CurrentTheme.ContextHelp.FOREGROUND
                }
              }
            })
          }
          row {
            button(AIPlaygroundUIBundle.message("button.create.playground"),
                   customizer = {
                     it.putClientProperty(DEFAULT_STYLE_KEY, true)
                   }
            ) {
              chatHistoryViewModel.newChat()
            }
          }
        }.also {
          it.alignmentY = Component.CENTER_ALIGNMENT
        })
      }, BorderLayout.CENTER)
    }
    panel.add(emptyRoot, "empty")
    panel.add(listRoot, "list")
    val content = contentFactory.createContent(panel, "", false)
    toolWindow.contentManager.addContent(content)
    toolWindow.setTitleActions(listOf(CreateNewChatAction()))
    toolWindow.component.putClientProperty(ToolWindowContentUi.DONT_HIDE_TOOLBAR_IN_HEADER, true)
    PlaygroundCollector.logToolWindowOpened()

    viewScope.launch {
      chatHistoryViewModel.availableChats.collect { chats ->
        withContext(Dispatchers.EDT) {
          list.setListData(chats.toTypedArray())
          if (chats.isEmpty()) {
            (panel.layout as CardLayout).show(panel, "empty")
          }
          else {
            (panel.layout as CardLayout).show(panel, "list")
          }
        }
      }
    }
  }

}
