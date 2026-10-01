package com.intellij.aiplayground.ui.chat.view

import com.intellij.aiplayground.ui.AIPlaygroundIconsHelper
import com.intellij.aiplayground.ui.AIPlaygroundUIBundle
import com.intellij.aiplayground.ui.chat.ChatModelLinkViewModel
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.application.EDT
import com.intellij.openapi.observable.util.whenTextChanged
import com.intellij.ui.SeparatorComponent
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.JLabelUtil
import com.intellij.util.ui.SwingTextTrimmer
import com.intellij.util.ui.UIUtil.getListBackground
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import kotlin.time.Duration.Companion.seconds

class ChatModelLinkView(
  private val coroutineScope: CoroutineScope,
  private val linkViewModel: ChatModelLinkViewModel,
  initiallyExpanded: Boolean,
) : JComponent() {

  private var bodyPanel: JComponent? = null
  private var arrowLabel: JBLabel? = null

  var expanded: Boolean = initiallyExpanded
    set(value) {
      if (field == value) return
      field = value
      arrowLabel?.icon = if (value) AllIcons.General.ArrowDown else AllIcons.General.ArrowRight
      ensureBodyPanel().isVisible = value
      revalidate()
      repaint()
    }

  private fun ensureBodyPanel(): JComponent {
    bodyPanel?.let { return it }
    val panel = createModelConfigBody(coroutineScope, linkViewModel).apply {
      alignmentX = LEFT_ALIGNMENT
      isVisible = expanded
    }
    // Insert before separator (last component)
    add(panel, componentCount - 1)
    bodyPanel = panel
    return panel
  }

  init {
    val link = linkViewModel.link.value
    if (link == null) {
      isVisible = false
    }
    else {
      layout = BoxLayout(this, BoxLayout.Y_AXIS)
      border = JBUI.Borders.empty(2, 0)

      val headerPanel = JPanel(BorderLayout()).apply {
        border = JBUI.Borders.empty(4, 12, 4, 8)
        isOpaque = true
        background = getListBackground()
        alignmentX = LEFT_ALIGNMENT
      }

      val leftHeader = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        isOpaque = false
      }

      arrowLabel = JBLabel(if (initiallyExpanded) AllIcons.General.ArrowDown else AllIcons.General.ArrowRight)
      leftHeader.add(arrowLabel)

      val providerIcon = AIPlaygroundIconsHelper.providerName2Icon(link.providerId.id)
      leftHeader.add(Box.createHorizontalStrut(4))
      leftHeader.add(JBLabel(providerIcon))

      leftHeader.add(Box.createHorizontalStrut(4))
      val nameLabel = JBLabel(link.modelId.id).apply {
        JLabelUtil.setTrimOverflow(this, true)
        putClientProperty(SwingTextTrimmer.KEY, SwingTextTrimmer.THREE_DOTS_IN_CENTER)
        if (!link.show) {
          foreground = JBUI.CurrentTheme.Label.disabledForeground()
        }
      }

      // Update display name from ViewModel
      coroutineScope.launch {
        linkViewModel.displayName.collectLatest { displayName ->
          withContext(Dispatchers.EDT) {
            nameLabel.text = displayName
            nameLabel.toolTipText = displayName
          }
        }
      }

      leftHeader.add(nameLabel)
      headerPanel.add(leftHeader, BorderLayout.CENTER)

      val rightHeader = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        isOpaque = false
      }

      val enableCheckbox = JBCheckBox().apply {
        isSelected = link.show
        toolTipText = if (link.show) AIPlaygroundUIBundle.message("disable.selected") else AIPlaygroundUIBundle.message("enable.selected")
        addActionListener {
          linkViewModel.toggleEnabled()
        }
      }
      rightHeader.add(enableCheckbox)

      val deleteButton = ActionButton(
        object : AnAction(AIPlaygroundUIBundle.message("popup.title.remove.model"), null, AllIcons.Actions.Close) {
          override fun actionPerformed(e: AnActionEvent) {
            linkViewModel.remove()
          }

          override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
        },
        Presentation().apply { icon = AllIcons.Actions.Close },
        ActionPlaces.UNKNOWN,
        JBUI.size(16)
      )
      rightHeader.add(deleteButton)

      headerPanel.add(rightHeader, BorderLayout.EAST)

      val headerClickListener = object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
          // Don't toggle when clicking checkbox or delete button
          val source = e.component
          if (source === enableCheckbox || source === deleteButton) return
          linkViewModel.toggleExpand()
        }
      }
      headerPanel.addMouseListener(headerClickListener)
      arrowLabel!!.addMouseListener(headerClickListener)
      nameLabel.addMouseListener(headerClickListener)

      add(headerPanel)

      // Body (config fields) — created lazily on first expand
      if (initiallyExpanded) {
        ensureBodyPanel()
      }

      add(SeparatorComponent(0, 0, JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground(), null).apply {
        alignmentX = LEFT_ALIGNMENT
      })
    }
  }
}

internal fun createModelConfigBody(
  coroutineScope: CoroutineScope,
  linkViewModel: ChatModelLinkViewModel,
): JComponent {
  val linkFlow = linkViewModel.link

  return panel {
    row {
      label(AIPlaygroundUIBundle.message("label.max.response.length"))
    }
    row {
      textField { component ->
        component.whenTextChanged { text ->
          if (text.isNotEmpty() && text.toIntOrNull() == null) return@whenTextChanged
          linkViewModel.updateRequestConfig { config ->
            config.copy(maxTokens = text.toIntOrNull())
          }
        }
        component.bind(coroutineScope, linkFlow.filterNotNull().map { it.parameters.maxTokens?.toString() ?: "" })
        component.emptyText.text = AIPlaygroundUIBundle.message("label.max.response.length.hint")
        component.toolTipText = AIPlaygroundUIBundle.message("tooltip.max.response.length")
      }
    }
    row {
      label(AIPlaygroundUIBundle.message("label.temperature"))
    }
    row {
      textField { component ->
        component.whenTextChanged { text ->
          if (text.isNotEmpty() && text.toDoubleOrNull() == null) return@whenTextChanged
          linkViewModel.updateRequestConfig { config ->
            config.copy(temperature = text.toDoubleOrNull())
          }
        }
        component.bind(coroutineScope, linkFlow.filterNotNull().map { it.parameters.temperature?.toString() ?: "" })
        component.emptyText.text = AIPlaygroundUIBundle.message("label.temperature.hint")
        component.toolTipText = AIPlaygroundUIBundle.message("tooltip.temperature")
      }
    }
    row {
      label(AIPlaygroundUIBundle.message("label.top.p"))
    }
    row {
      textField { component ->
        component.whenTextChanged { text ->
          if (text.isNotEmpty() && text.toDoubleOrNull() == null) return@whenTextChanged
          linkViewModel.updateRequestConfig { config ->
            config.copy(topP = text.toDoubleOrNull())
          }
        }
        component.bind(coroutineScope, linkFlow.filterNotNull().map { it.parameters.topP?.toString() ?: "" })
        component.emptyText.text = AIPlaygroundUIBundle.message("label.top.p.hint")
        component.toolTipText = AIPlaygroundUIBundle.message("tooltip.top.p")
      }
    }
    row {
      val appliedLabel = JBLabel(AIPlaygroundUIBundle.message("button.applied")).apply {
        icon = AllIcons.General.GreenCheckmark
        isVisible = false
      }

      val applyAllButton = JButton(AIPlaygroundUIBundle.message("button.apply.to.all")).apply {
        addActionListener {
          linkViewModel.applyConfigToAll()
          appliedLabel.isVisible = true
          coroutineScope.launch {
            delay(2.seconds)
            withContext(Dispatchers.EDT) {
              appliedLabel.isVisible = false
            }
          }
        }
      }

      // Enable only when more than one active model
      applyAllButton.isEnabled = linkViewModel.canApplyConfigToAll.value
      coroutineScope.launch {
        linkViewModel.canApplyConfigToAll.collectLatest { canApply ->
          withContext(Dispatchers.EDT) {
            applyAllButton.isEnabled = canApply
          }
        }
      }

      cell(JPanel(BorderLayout()).apply {
        isOpaque = false
        add(appliedLabel, BorderLayout.CENTER)
        add(applyAllButton, BorderLayout.EAST)
      })
    }
  }.apply { border = JBUI.Borders.empty(4, 24, 8, 12) }
}
