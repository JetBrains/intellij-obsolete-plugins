package com.intellij.aiplayground.ui.chat.view

import com.intellij.aiplayground.ui.AIPlaygroundUIBundle
import com.intellij.aiplayground.ui.chat.ChatViewModel
import com.intellij.ide.ActivityTracker
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.NlsContexts
import com.intellij.platform.util.coroutines.childScope
import com.intellij.ui.awt.AnchoredPoint
import com.intellij.ui.awt.AnchoredPoint.Anchor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.Wrapper
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import java.awt.BorderLayout.SOUTH
import java.awt.Dimension
import java.awt.event.AdjustmentEvent
import java.awt.event.AdjustmentListener
import javax.swing.JComponent
import javax.swing.JPanel
import kotlin.math.max
import kotlin.math.min

class ChatScreenComponent(
  private val viewModel: ChatViewModel,
  parentScope: CoroutineScope,
  val project: Project,
) : JPanel(BorderLayout()) {
  private val coroutineScope = parentScope.childScope("ChatScreenComponent")

  private var balloon: Balloon? = null

  // Flag to control auto-scrolling behavior (enabled by default and when streaming starts)
  private var shouldAutoScroll = true

  // Flag to distinguish between user-initiated scrolling and programmatic scrolling
  // Used to prevent false detection of user scrolling when we programmatically scroll to the bottom
  private var isAutoScrolling = false

  private val input: MessageInput = MessageInput(
    project,
    onUpdateMessage = { text ->
      viewModel.updateInput(text)
    },
    onSendMessage = {
      if (balloon != null) {
        balloon?.hide(true)
        balloon = null
      }
      if (it.isBlank()) {
        showError(AIPlaygroundUIBundle.message("popup.content.you.must.enter.text.before.sending"))
      }
      else if (viewModel.activeModels.value.isEmpty()) {
        showError(AIPlaygroundUIBundle.message("popup.content.error.no.models.selected"))
      }
      else {
        input.text = ""
        viewModel.sendMessage(it)
      }
    },
    onStop = {
      viewModel.stopStreaming()
    }
  ).apply {
    addDefaultBottomToolbar()
  }

  private fun showError(@NlsContexts.PopupContent message: String) {
    balloon = JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(
      message,
      null,
      JBUI.CurrentTheme.NotificationError.backgroundColor(),
      null)
      .setDisposable { input }
      .setShowCallout(false)
      .createBalloon()

    balloon?.show(AnchoredPoint(Anchor.TOP, input), Balloon.Position.above)
  }

  init {
    val messageHistory = MessageHistory(viewModel, coroutineScope, project)
    val scrollPane = JBScrollPane(messageHistory).apply {
      border = JBUI.Borders.empty()
      horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
    }

    // Add scroll listener to detect when user manually scrolls
    scrollPane.verticalScrollBar.addAdjustmentListener(object : AdjustmentListener {
      private var lastMaximum = 0
      private var lastValue = 0

      override fun adjustmentValueChanged(e: AdjustmentEvent) {
        val verticalBar = scrollPane.verticalScrollBar

        if (e.value < lastValue) {
          shouldAutoScroll = false
        }
        else if (e.value >= verticalBar.maximum - verticalBar.visibleAmount) {
          shouldAutoScroll = true
        }

        // Store the current value for next comparison
        lastValue = e.value

        // If the content size changed (new messages added)
        if (verticalBar.maximum != lastMaximum) {
          lastMaximum = verticalBar.maximum

          // Auto-scroll to bottom only if auto-scrolling is enabled
          if (shouldAutoScroll) {
            isAutoScrolling = true
            verticalBar.value = verticalBar.maximum
            isAutoScrolling = false
          }
        }
      }
    })


    add(scrollPane, BorderLayout.CENTER)
    add(createInput(), SOUTH)

    // Auto-scroll when messages are updated (e.g., when a new message is added or when streaming)
    coroutineScope.launch {
      viewModel.chatHistory.collectLatest {
        withContext(Dispatchers.EDT) {
          // Scroll to bottom after messages are updated only if auto-scrolling is enabled
          // (user hasn't scrolled up or has scrolled back to bottom)
          if (shouldAutoScroll) {
            isAutoScrolling = true
            val verticalBar = scrollPane.verticalScrollBar
            verticalBar.value = verticalBar.maximum
            isAutoScrolling = false
          }
        }
      }
    }

    // Monitor streaming state to control auto-scrolling behavior
    coroutineScope.launch {
      viewModel.isStreaming.collectLatest {
        input.isStreaming = it
        ActivityTracker.getInstance().inc()

        // When streaming starts (new message generation), always enable auto-scrolling
        // This ensures that the user sees the newly generated content
        if (it) {
          shouldAutoScroll = true
        }
      }
    }

    coroutineScope.launch {
      viewModel.input.collectLatest { text ->
        withContext(Dispatchers.EDT) {
          input.text = text
        }
      }
    }
  }

  fun getPreferredFocusedComponent(): JComponent {
    return input.getPreferredFocusedComponent()
  }

  private fun createInput(): BorderLayoutPanel = object : BorderLayoutPanel() {
    init {
      add(wrapWithBorder(input, JBUI.Borders.emptyTop(6)), BorderLayout.CENTER)
    }

    override fun getPreferredSize(): Dimension {
      val preferredSize = super.getPreferredSize()
      return Dimension(max(preferredSize.width, JBUIScale.scale(500)), min(150, preferredSize.height))
    }
  }
}

fun wrapWithBorder(input: JComponent, border: JBEmptyBorder): Wrapper = Wrapper(input).apply {
  this.border = border
}