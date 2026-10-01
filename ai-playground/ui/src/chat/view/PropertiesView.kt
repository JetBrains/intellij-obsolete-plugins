package com.intellij.aiplayground.ui.chat.view

import com.intellij.BundleBase
import com.intellij.aiplayground.models.chat.ChatModelLinkId
import com.intellij.aiplayground.models.chat.ChatRepository
import com.intellij.aiplayground.models.utils.AiPlaygroundCoroutine
import com.intellij.aiplayground.ui.AIPlaygroundUIBundle
import com.intellij.aiplayground.ui.actions.ClearChat
import com.intellij.aiplayground.ui.actions.RemoveChat
import com.intellij.aiplayground.ui.actions.RenameChat
import com.intellij.aiplayground.ui.chat.ChatUiProvider
import com.intellij.aiplayground.ui.chat.ChatViewModel
import com.intellij.aiplayground.ui.chat.actions.showModelsPopup
import com.intellij.icons.AllIcons
import com.intellij.ide.DataManager
import com.intellij.ide.ui.laf.darcula.DarculaUIUtil
import com.intellij.ide.ui.laf.darcula.ui.DarculaButtonUI
import com.intellij.ui.scale.JBUIScale
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.actionSystem.impl.ActionButtonWithText
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.components.service
import com.intellij.openapi.observable.util.whenTextChanged
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.RelativeFont
import com.intellij.ui.SeparatorComponent
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.tabs.JBTabsFactory
import com.intellij.ui.tabs.TabInfo
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.TextComponentEmptyText
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.predicate
import com.intellij.ui.paint.LinePainter2D
import com.intellij.ui.paint.RectanglePainter2D
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.CardLayout
import java.awt.Dimension
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.RenderingHints
import java.awt.event.ActionEvent
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.GroupLayout
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.border.AbstractBorder
import javax.swing.text.JTextComponent

class PropertiesView(
  project: Project,
  coroutineScope: CoroutineScope,
  viewModel: ChatViewModel,
) : JComponent() {

  init {
    layout = BorderLayout()
    add(createHeader(project, viewModel), BorderLayout.NORTH)
    add(createTabbedContent(project, coroutineScope, viewModel), BorderLayout.CENTER)
  }

}

private fun createHeader(project: Project, viewModel: ChatViewModel): JPanel = BorderLayoutPanel().apply {
  border = JBUI.Borders.empty(12, 12, 12, 8)
  val header = JLabel(AIPlaygroundUIBundle.message("playground.settings"))
  RelativeFont.HUGE.install(header)
  RelativeFont.BOLD.install(header)
  addToLeft(header)

  val duplicateAction = object : AnAction(
    AIPlaygroundUIBundle.message("action.duplicate.playground.text"),
    AIPlaygroundUIBundle.message("action.duplicate.playground.description"),
    AllIcons.Actions.Copy
  ) {
    override fun actionPerformed(e: AnActionEvent) {
      val chat = viewModel.activeChat.value
      val baseName = chat.title
                     ?: chat.generateSummaryTitle()
                     ?: AIPlaygroundUIBundle.message("new.name")
      val copySuffix = AIPlaygroundUIBundle.message("name.copy.suffix")
      val newTitle = "$baseName $copySuffix"
      val repo = project.service<ChatRepository>()
      val newChat = repo.createChat(newTitle)
      val updated = repo.updateChat(newChat.id) {
        it.copy(
          title = newTitle,
          systemPrompt = chat.systemPrompt,
          activeModels = chat.activeModels.map { link ->
            link.copy(
              id = ChatModelLinkId(),
              parameters = link.parameters.copy()
            )
          }
        )
      }
      service<AiPlaygroundCoroutine>().coroutineScope.launch {
        project.service<ChatUiProvider>().openChat(updated)
      }
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
  }

  val moreAction = object : AnAction(null, null, AllIcons.Actions.More) {
    override fun actionPerformed(e: AnActionEvent) {
      val popup = JBPopupFactory.getInstance().createActionGroupPopup(
        null,
        DefaultActionGroup(
          listOf(
            RenameChat(),
            duplicateAction,
            ActionManager.getInstance().getAction("AIPlayground.ManageProviders"),
            Separator(),
            ClearChat(),
            RemoveChat()
          )
        ),
        DataManager.getInstance().getDataContext(this@apply),
        JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
        true,
      )
      popup.showInBestPositionFor(e.dataContext)
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
  }

  val moreButton = ActionButton(
    moreAction,
    null,
    ActionPlaces.UNKNOWN,
    ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
  ).apply {
    border = JBUI.Borders.empty()
  }

  addToRight(moreButton)
}

private fun createTabbedContent(project: Project, coroutineScope: CoroutineScope, viewModel: ChatViewModel): JComponent {
  val tabs = JBTabsFactory.createTabs(project)
  tabs.addTab(TabInfo(createModelsTab(project, coroutineScope, viewModel)).setText(AIPlaygroundUIBundle.message("tab.models")))
  tabs.addTab(TabInfo(createSystemPromptTab(coroutineScope, viewModel)).setText(AIPlaygroundUIBundle.message("tab.system.prompt")))
  return tabs.component
}

private fun createModelsTab(project: Project, coroutineScope: CoroutineScope, viewModel: ChatViewModel): JComponent {
  val emptyStatePanel = createEmptyStatePanel(project, viewModel)
  val accordionPanel = ModelAccordionPanel(coroutineScope, viewModel)

  val countLabel = JBLabel(AIPlaygroundUIBundle.message("models.added.count", 0))

  val addModelButton = ActionButtonWithText(
    object : AnAction(AIPlaygroundUIBundle.message("popup.title.add.model"),
                      AIPlaygroundUIBundle.message("popup.title.add.model.description"), AllIcons.General.Add) {
      override fun actionPerformed(e: AnActionEvent) {
        showModelsPopup(project, viewModel) { popup ->
          popup.showInBestPositionFor(e.dataContext)
        }
      }

      override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
    },
    Presentation().apply {
      icon = AllIcons.General.Add
      text = AIPlaygroundUIBundle.message("popup.title.add.model")
    },
    ActionPlaces.UNKNOWN,
    ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
  )

  // Hide the Add Model button when there are already 5 or more models
  run {
    val predicate = viewModel.activeModels.predicate(coroutineScope) { it.size < 5 }
    addModelButton.isVisible = predicate()
    predicate.addListener { visible -> addModelButton.isVisible = visible }
  }

  val subHeader = BorderLayoutPanel().apply {
    border = JBUI.Borders.empty(8, 12)
    addToLeft(countLabel)
    addToRight(addModelButton)
  }

  val cardLayout = CardLayout()
  val cardPanel = JPanel(cardLayout).apply {
    add(emptyStatePanel, "empty")
    add(accordionPanel, "content")
  }

  coroutineScope.launch {
    viewModel.activeModels.collectLatest { models ->
      withContext(Dispatchers.EDT) {
        countLabel.text = AIPlaygroundUIBundle.message("models.added.count", models.size)
        cardLayout.show(cardPanel, if (models.isEmpty()) "empty" else "content")
      }
    }
  }

  return BorderLayoutPanel().apply {
    addToTop(subHeader)
    addToCenter(cardPanel)
  }
}

private fun createEmptyStatePanel(project: Project, viewModel: ChatViewModel): JPanel {
  val innerPanel = JPanel().apply {
    layout = BoxLayout(this, BoxLayout.Y_AXIS)
    isOpaque = false

    val titleLabel = JBLabel(AIPlaygroundUIBundle.message("empty.state.title")).apply {
      alignmentX = Component.CENTER_ALIGNMENT
      RelativeFont.LARGE.install(this)
      RelativeFont.BOLD.install(this)
    }
    add(titleLabel)
    add(Box.createVerticalStrut(8))

    val descLabel = JBLabel(AIPlaygroundUIBundle.message("empty.state.description")).apply {
      alignmentX = Component.CENTER_ALIGNMENT
      foreground = JBUI.CurrentTheme.Label.disabledForeground()
    }
    add(descLabel)
    add(Box.createVerticalStrut(16))

    val addModelButton = JButton(AIPlaygroundUIBundle.message("popup.title.add.model"))
    addModelButton.putClientProperty(DarculaButtonUI.DEFAULT_STYLE_KEY, true)
    addModelButton.addActionListener {
      showModelsPopup(project, viewModel) { popup ->
        popup.showUnderneathOf(addModelButton)
      }
    }

    val manageProvidersButton = JButton(AIPlaygroundUIBundle.message("button.manage.providers")).apply {
      addActionListener {
        ActionManager.getInstance().tryToExecute(
          ActionManager.getInstance().getAction("AIPlayground.ManageProviders"),
          null, null, null, true
        )
      }
    }

    val buttonsRow = JPanel().apply {
      layout = BoxLayout(this, BoxLayout.X_AXIS)
      alignmentX = Component.CENTER_ALIGNMENT
      isOpaque = false
      add(addModelButton)
      add(Box.createHorizontalStrut(8))
      add(manageProvidersButton)
    }
    add(buttonsRow)
  }

  return JPanel(GridBagLayout()).apply {
    border = JBUI.Borders.empty(20)
    add(innerPanel)
  }
}

private class ModelAccordionPanel(
  private val coroutineScope: CoroutineScope,
  private val viewModel: ChatViewModel,
) : JBScrollPane() {

  private val modelsContainer = JPanel().apply {
    layout = BoxLayout(this, BoxLayout.Y_AXIS)
  }

  private val warningLabel = JBLabel(AIPlaygroundUIBundle.message("label.model.limit.reached.only.models.are.allowed")).apply {
    foreground = JBUI.CurrentTheme.Label.disabledForeground()
    border = JBUI.Borders.empty(4, 12)
    isVisible = false
  }

  private val viewsByLinkId = mutableMapOf<ChatModelLinkId, ChatModelLinkView>()

  init {
    border = JBUI.Borders.empty()
    setViewportView(BorderLayoutPanel().apply {
      addToTop(modelsContainer)
      addToBottom(warningLabel)
    })

    coroutineScope.launch {
      viewModel.activeModels
        .map { models -> models.map { it.id } }
        .distinctUntilChanged()
        .collectLatest { modelIds ->
          withContext(Dispatchers.EDT) {
            syncModelViews(modelIds)
          }
        }
    }

    coroutineScope.launch {
      viewModel.expandedModelLinkId.collectLatest { expandedId ->
        withContext(Dispatchers.EDT) {
          for ((id, view) in viewsByLinkId) {
            view.expanded = (id == expandedId)
          }
          modelsContainer.revalidate()
          modelsContainer.repaint()
        }
      }
    }
  }

  private fun syncModelViews(modelIds: List<ChatModelLinkId>) {
    val currentIds = modelIds.toSet()

    viewsByLinkId.keys.toList().forEach { id ->
      if (id !in currentIds) {
        viewsByLinkId.remove(id)?.let { modelsContainer.remove(it) }
      }
    }

    val expandedId = viewModel.expandedModelLinkId.value
    for (id in modelIds) {
      if (id !in viewsByLinkId) {
        val linkViewModel = viewModel.getModelLinkViewModel(id)
        val view = ChatModelLinkView(coroutineScope, linkViewModel, id == expandedId)
        modelsContainer.add(view)
        viewsByLinkId[id] = view
      }
    }

    warningLabel.isVisible = modelIds.size >= 5
    modelsContainer.revalidate()
    modelsContainer.repaint()
  }
}

private fun createSystemPromptTab(coroutineScope: CoroutineScope, viewModel: ChatViewModel): JComponent {
  val outerPanel = JPanel(BorderLayout()).apply {
    border = JBUI.Borders.empty(12)
  }

  val textArea = JBTextArea().apply {
    rows = 8
    lineWrap = true
    wrapStyleWord = true
    text = viewModel.systemPromptText.value
    emptyText.text = AIPlaygroundUIBundle.message("system.prompt.placeholder")
    TextComponentEmptyText.setupPlaceholderVisibility(this)
  }

  val scrollPane = JBScrollPane(textArea).apply {
    val roundedBorder = createRoundedBorder()
    textArea.addFocusListener(object : FocusListener {
      override fun focusGained(e: FocusEvent) = roundedBorder.applyFocused(true, textArea)
      override fun focusLost(e: FocusEvent) = roundedBorder.applyFocused(false, textArea)
    })
    border = roundedBorder
  }

  // Track saved vs current text for dirty state
  var savedText = viewModel.systemPromptText.value

  val cancelButton = JButton(AIPlaygroundUIBundle.message("cancel")).apply {
    isEnabled = false
    addActionListener {
      textArea.text = savedText
    }
  }

  val saveButton = JButton(AIPlaygroundUIBundle.message("button.save.changes")).apply {
    isEnabled = false
    addActionListener {
      viewModel.updateSystemPromptText(textArea.text)
      savedText = textArea.text
      this.isEnabled = false
      cancelButton.isEnabled = false
    }
  }

  textArea.whenTextChanged {
    val isDirty = textArea.text != savedText
    saveButton.isEnabled = isDirty
    cancelButton.isEnabled = isDirty
  }

  // Sync saved text from model when it changes externally
  coroutineScope.launch {
    viewModel.systemPromptText.collectLatest { prompt ->
      withContext(Dispatchers.EDT) {
        savedText = prompt
        if (textArea.text != prompt) {
          textArea.text = prompt
        }
        saveButton.isEnabled = false
        cancelButton.isEnabled = false
      }
    }
  }

  val buttonsPanel = JPanel().apply {
    layout = BoxLayout(this, BoxLayout.X_AXIS)
    border = JBUI.Borders.emptyTop(8)
    add(Box.createHorizontalGlue())
    add(cancelButton)
    add(Box.createRigidArea(Dimension(8, 0)))
    add(saveButton)
  }

  val contentPanel = JPanel(BorderLayout()).apply {
    isOpaque = false
    add(scrollPane, BorderLayout.CENTER)
    add(buttonsPanel, BorderLayout.SOUTH)
  }
  outerPanel.add(contentPanel, BorderLayout.NORTH)

  return outerPanel
}

class RoundedBorder : AbstractBorder() {

  var color: Color = JBUI.CurrentTheme.Button.buttonOutlineColorStart(false)
  var thickness: Int = 1
  var arcDiameterSupplier: (Component) -> Int = { c -> c.size.height }
  var insets: Insets = JBUI.insets(1)

  fun applyFocused(focused: Boolean, repaintTarget: JComponent) {
    thickness = if (focused) JBUIScale.scale(2) else JBUIScale.scale(1)
    color = if (focused) JBUI.CurrentTheme.Focus.focusColor()
    else DarculaUIUtil.getOutlineColor(repaintTarget.isEnabled, false)
    repaintTarget.repaint()
  }

  override fun paintBorder(c: Component, g: Graphics?, x: Int, y: Int, width: Int, height: Int) {
    val g2d = g?.create() as? Graphics2D ?: return
    g2d.color = color
    val arcDiameter = arcDiameterSupplier(c)
    RectanglePainter2D.DRAW.paint(g2d,
                                  x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble(),
                                  arcDiameter.toDouble(), LinePainter2D.StrokeType.CENTERED, thickness.toDouble(),
                                  RenderingHints.VALUE_ANTIALIAS_ON)
  }

  override fun getBorderInsets(c: Component, insets: Insets): Insets {
    return insets.also {
      it.left = this.insets.left
      it.top = this.insets.top
      it.right = this.insets.right
      it.bottom = this.insets.bottom
    }
  }
}

fun createRoundedBorder(): RoundedBorder = RoundedBorder().apply {
  arcDiameterSupplier = { 12 }
  insets = JBUI.insets(12, 8, 6, 8)
}

fun JTextComponent.bind(coroutineScope: CoroutineScope, flow: Flow<String>) {
  coroutineScope.launch {
    flow.collectLatest { text ->
      withContext(Dispatchers.EDT + ModalityState.any().asContextElement()) {
        if (this@bind.text != text) {
          this@bind.text = text
        }
      }
    }
  }
}

fun panel(block: PanelBuilder.() -> Unit): JComponent {
  return JPanel().apply {
    val groupLayout = GroupLayout(this)
    groupLayout.autoCreateGaps = true
    layout = groupLayout
    block(PanelBuilder(groupLayout))
  }
}

class PanelBuilder(private val layout: GroupLayout) {

  private val verticalGroup = layout.createSequentialGroup()
  private val horizontalGroup = layout.createSequentialGroup()

  private val columnGroups = mutableMapOf<Int, GroupLayout.ParallelGroup>()

  init {
    layout.setVerticalGroup(verticalGroup)
    layout.setHorizontalGroup(horizontalGroup)
  }

  fun row(block: RowBuilder.() -> Unit) {
    val rowGroup = layout.createParallelGroup()
    RowBuilder(rowGroup) { column ->
      getOrCreateColumn(column)
    }
      .apply(block)
    verticalGroup.addGroup(rowGroup)
  }

  private fun getOrCreateColumn(column: Int): GroupLayout.ParallelGroup =
    columnGroups.getOrPut(column) { layout.createParallelGroup().also { horizontalGroup.addGroup(it) } }

  fun separator() {
    val component = SeparatorComponent(0, 0, JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground(), null)
    verticalGroup.addComponent(component)
    getOrCreateColumn(0).addComponent(component)
  }

}

class RowBuilder(private val rowGroup: GroupLayout.ParallelGroup, private val columnGroupProvider: (Int) -> GroupLayout.ParallelGroup) {

  private val components = mutableListOf<JComponent>()
  private var column = 0

  fun label(@NlsContexts.Label text: String, customizer: (JLabel) -> Unit = {}) {
    addComponent(JLabel(text).apply(customizer))
  }

  fun text(@NlsContexts.Label text: String, customizer: (JLabel) -> Unit = {}) {
    addComponent(JLabel("<html>$text</html>")
                   .apply(customizer))
  }

  fun icon(icon: Icon) {
    val label = JBLabel(icon)
    label.disabledIcon = com.intellij.util.IconUtil.desaturate(icon)
    addComponent(label)
  }

  fun cell(component: JComponent) {
    addComponent(component)
  }

  fun placeholder() {
    column++
  }

  fun link(@NlsContexts.LinkLabel text: String, action: (e: ActionEvent) -> Unit) {
    addComponent(ActionLink(text, action))
  }

  fun button(@NlsContexts.Button text: String, customizer: (JButton) -> Unit = {}, actionListener: (event: ActionEvent) -> Unit) {
    val button = JButton(BundleBase.replaceMnemonicAmpersand(text))
    button.addActionListener(actionListener)
    button.isOpaque = false
    customizer(button)
    return addComponent(button)
  }

  fun textField(customizer: (JBTextField) -> Unit = {}) {
    addComponent(JBTextField().apply(customizer))
  }

  fun visibleIf(predicate: ComponentPredicate) {
    visible(predicate())
    predicate.addListener { visible(it) }
  }

  private fun visible(predicate: Boolean) {
    for (it in components) {
      it.isVisible = predicate
    }
  }

  private fun addComponent(component: JComponent) {
    components.add(component)
    rowGroup.addComponent(component)
    columnGroupProvider(column++).addComponent(component)
  }
}
