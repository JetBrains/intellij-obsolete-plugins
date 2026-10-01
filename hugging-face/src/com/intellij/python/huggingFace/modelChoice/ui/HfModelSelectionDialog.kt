// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.python.huggingFace.modelChoice.ui

import com.intellij.codeInsight.documentation.DocumentationHtmlUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.OnePixelDivider
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.python.community.impl.huggingFace.HuggingFaceConstants
import com.intellij.python.community.impl.huggingFace.api.HuggingFaceEntityBasicApiData
import com.intellij.python.community.impl.huggingFace.api.HuggingFaceModelSortKey
import com.intellij.python.community.impl.huggingFace.service.HuggingFaceCardsUsageCollector
import com.intellij.python.community.impl.huggingFace.service.HuggingFaceCoroutine
import com.intellij.python.huggingFace.HuggingFaceProBundle
import com.intellij.python.huggingFace.modelChoice.modelHandling.HfModelChoiceHtmlContentGenerator
import com.intellij.python.huggingFace.modelChoice.modelHandling.HfModelInserter
import com.intellij.python.huggingFace.modelChoice.search.HfSearchService
import com.intellij.python.huggingFace.service.HfDialogTimeTracker
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.HelpIdAwareLinkListener
import com.intellij.ui.JBColor
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.SearchTextField
import com.intellij.ui.border.CustomLineBorder
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.TextComponentEmptyText
import com.intellij.util.ui.HTMLEditorKitBuilder
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.launch
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.Nls
import java.awt.BorderLayout
import java.awt.Dimension
import java.util.function.Consumer
import javax.swing.Action
import javax.swing.BorderFactory
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JEditorPane
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.border.Border
import javax.swing.event.DocumentEvent
import javax.swing.text.Element
import javax.swing.text.View
import javax.swing.text.html.ImageView
import javax.swing.text.html.ParagraphView


/**
 * Dialog scheme:
 * 11 | 111 111 111 111
 * ---|----------------
 * 22 | 333 333 333 333
 * 22 | ---------------
 * 22 | 444 | 5555 5555
 * 22 | 444 | 5555 5555
 * 22 | 444 | 5555 5555
 * 22 | 444 | 5555 5555
 * --------------------
 * 66 | 666 | 6666 6666

 * Where
 * 1 - search field
 * 2 - pipeline tags (tasks) panel
 * 3 - filters panel
 * 4 - search results scroll view
 * 5 - selected model card
 * 6 - south panel -> cancel and use model buttons
 */
@ApiStatus.Internal
class HfModelSelectionDialog(
  private val project: Project, private val fileCalledExtension: HuggingFaceCardsUsageCollector.ActiveFileType,
) : DialogWrapper(project) {
  // See PY-63671, figma:
  // https://www.figma.com/file/NpIZPvqFVhctYmd8UB93iM/Hugging-face?type=design&node-id=582-15710&mode=design
  // HuggingFaceApi playground:
  // https://huggingface.co/spaces/enzostvs/hub-api-playground
  // todo: process the internet absence scenario
  private val htmlContentGenerator = HfModelChoiceHtmlContentGenerator(project)
  private val searchService = HfSearchService()
  private val viewModel = HfModelSearchWindowModel()
  private val listModel = DefaultListModel<HuggingFaceEntityBasicApiData>()
  private val timeTracker = HfDialogTimeTracker.getInstance(project)

  private var pipelineTagsPanel = HfPipelineTagPanel(viewModel, this::searchAndUpdate)  // panel 2
  private var modelCardPanel = createDescriptionComponent(null)  // panel 5
  private val modelInserter = HfModelInserter(project) {
    trackDialogClosed(HuggingFaceCardsUsageCollector.ModelChoiceDialogClosedResultType.USE_MODEL, viewModel.selectedModel?.pipelineTag)
    close(OK_EXIT_CODE)
  }
  private val searchTextField = SearchTextField(false).apply {
    textEditor.apply {
      border = CustomLineBorder(OnePixelDivider.BACKGROUND, 0, 0, 1, 0)
      emptyText.text = HuggingFaceProBundle.message("python.hugging.face.model.choice.search.bar.placeholder")
      accessibleContext.accessibleName = HuggingFaceProBundle.message("python.hugging.face.model.choice.search.bar.accessible")
      TextComponentEmptyText.setupPlaceholderVisibility(this)
      document.addDocumentListener(object : DocumentAdapter() {
        override fun textChanged(e: DocumentEvent) {
          viewModel.searchFieldString = text
          searchAndUpdate()
        }
      })
    }
  }

  private val hfListModel = HfModelList(
    listModel,
    htmlContentGenerator,
    { selectedModel ->
      viewModel.selectedModel = selectedModel
      updateOkButtonStatus()
    },
    this::updateModelCardPanel,
    { doubleClickedModel -> modelInserter.insertModel(doubleClickedModel) }
  )

  init {
    super.init()
    title = HuggingFaceProBundle.message("hugging.face.model.choice.dialog.window.title")
    myPreferredFocusedComponent = searchTextField
    setOKButtonText(HuggingFaceProBundle.message("hugging.face.model.choice.dialog.window.button.use.model"))
    trackDialogOpen()
    searchAndUpdate()
  }

  /**
   * Panel (1)
   * search field
   */
  override fun createNorthPanel() = searchTextField

  /**
   * Panel (5)
   * selected model card
   */
  private fun updateModelCardPanel(@Nls htmlContent: String) {
    modelCardPanel.text = htmlContent
    modelCardPanel.revalidate()
    modelCardPanel.repaint()
    modelCardPanel.caretPosition = 0
  }

  private fun createDescriptionComponent(imageViewHandler: Consumer<in View?>?): JEditorPane {
    // todo: adaptive width of the content
    // todo: sync with documentation styles
    // todo: empty results placeholder
    // todo: fix internal links
    val kit = HTMLEditorKitBuilder()
      .withViewFactoryExtensions({ e: Element?, view: View? ->
        if (view is ParagraphView) {
          return@withViewFactoryExtensions object : ParagraphView(e) {
            init { super.setLineSpacing(0.3f) }
            override fun setLineSpacing(ls: Float) { }
        }}
        if (imageViewHandler != null && view is ImageView) {
          imageViewHandler.accept(view)
        }
        view
    }).build()

    val sheet = kit.styleSheet
    sheet.addStyleSheet(DocumentationHtmlUtil.getDocumentationPaneAdditionalCssRules())

    val editorPane = JEditorPane()
    editorPane.isEditable = false
    editorPane.isOpaque = false
    editorPane.border = null
    editorPane.contentType = "text/html"
    editorPane.editorKit = kit
    editorPane.background = JBColor.WHITE
    editorPane.addHyperlinkListener(HelpIdAwareLinkListener.getInstance())

    editorPane.border = BorderFactory.createEmptyBorder(MODEL_INFO_PANEL_TOP_PADDING,
                                                        MODEL_INFO_PANEL_HORIZONTAL_PADDING,
                                                        0,
                                                        MODEL_INFO_PANEL_HORIZONTAL_PADDING)
    return editorPane
  }

  /**
   * Center panel combines panels from 2 to 5
   * 1 is managed by [createNorthPanel]
   * 6 is managed by [createSouthPanel]
   */
  override fun createCenterPanel(): JComponent {
    // Tags search panel (3)
    val tagsSearchPanel = HfTagSearchPanel(viewModel, this::searchAndUpdate).getPanel()
    // Search results table (4)
    val searchResultsScrollPane = JBScrollPane(hfListModel).apply {
      border = null
      background = JBColor.WHITE
      verticalScrollBar.background = JBColor.WHITE
      verticalScrollBar.isOpaque = false
      horizontalScrollBar.background = JBColor.WHITE
      horizontalScrollBar.isOpaque = false
    }

    // Model card window (5)
    val modelCardScrollPane = JBScrollPane(modelCardPanel).apply {
      border = null
      verticalScrollBar.background = JBColor.WHITE
      verticalScrollBar.isOpaque = false
      horizontalScrollBar.background = JBColor.WHITE
      horizontalScrollBar.isOpaque = false
    }

    // Combine search results and model card into a split pane (4 & 5)
    val innerSplitter = OnePixelSplitter(INNER_SPLITTER_PROPORTION).apply {
      firstComponent = searchResultsScrollPane
      secondComponent = modelCardScrollPane
      isShowDividerControls = true
    }

    // Attach tags panel (3) on top of the combined (4+5)
    val rightSidePanel = JPanel(BorderLayout()).apply {
      add(tagsSearchPanel, BorderLayout.NORTH)
      add(innerSplitter, BorderLayout.CENTER)
    }

    // Outer splitter combines pipeline panel (2) with (3+4+5) on the right
    val outerSplitter = OnePixelSplitter(OUTER_SPLITTER_PROPORTION).apply {
      firstComponent = pipelineTagsPanel
      secondComponent = rightSidePanel
    }
    return outerSplitter
  }

  /**
   * Panel 6
   * The lowest panel with three buttons
   * (?) --- [cancel][use model]
   */
  override fun createJButtonForAction(action: Action?): JButton =  super.createJButtonForAction(action).apply { background = JBColor.WHITE }
  override fun createSouthPanel(): JComponent = super.createSouthPanel().apply { background = JBColor.WHITE }
  override fun doOKAction() = modelInserter.insertModel(viewModel.selectedModel)

  override fun doCancelAction() {
    trackDialogClosed(HuggingFaceCardsUsageCollector.ModelChoiceDialogClosedResultType.CLOSE, null)
    close(CANCEL_EXIT_CODE)
  }

  private fun updateOkButtonStatus() {
    isOKActionEnabled = viewModel.isInsertPossible.get()
    setOKButtonTooltip(when (isOKActionEnabled) {
      true -> null
      false -> DISABLED_OK_BUTTON_TOOLTIP
    })
  }

  private fun trackDialogOpen() {
    timeTracker.startTimer()
    HuggingFaceCardsUsageCollector.HF_MODEL_CHOICE_DIALOG_OPENED.log(
      HuggingFaceCardsUsageCollector.ModelChoiceEntryPointType.CONTEXT_MENU,
      fileCalledExtension
    )
  }

  private fun trackDialogClosed(
    closedResult: HuggingFaceCardsUsageCollector.ModelChoiceDialogClosedResultType,
    selectedTask: String?,
  ) {
    val sessionDuraton = HfDialogTimeTracker.getInstance(project).stopAndReportTimer()

    HuggingFaceCardsUsageCollector.HF_MODEL_CHOICE_DIALOG_CLOSED.log(
      closedResult,
      selectedTask ?: HuggingFaceConstants.UNDEFINED_PIPELINE_TAG,
      sessionDuraton
    )
  }

  private fun searchAndUpdate() {
    val query = viewModel.searchFieldString ?: ""
    val tagsString = viewModel.createTagString()
    val sortKey = viewModel.sortKey ?: HuggingFaceModelSortKey.DOWNLOADS

    HuggingFaceCoroutine.Utils.ioScope.launch {
      val results = searchService.performSearch(query, tagsString, sortKey)
      updateUIWithSearchResults(results)
    }
  }

  private fun updateUIWithSearchResults(results: Array<HuggingFaceEntityBasicApiData>) {
    SwingUtilities.invokeLater {
      listModel.removeAllElements()
      results.forEach { listModel.addElement(it) }
    }

    if (viewModel.selectedModel == null && results.isNotEmpty()) {

      viewModel.selectedModel = results[0]
      HuggingFaceCoroutine.Utils.ioScope.launch {
        val htmlContent = htmlContentGenerator.generateHtmlContent(results[0])
        SwingUtilities.invokeLater { updateModelCardPanel(htmlContent) }
      }
    }
  }

  override fun createContentPaneBorder(): Border? = null
  override fun getPreferredSize(): Dimension = DEFAULT_WINDOW_SIZE
  override fun getInitialSize(): Dimension = DEFAULT_WINDOW_SIZE
  override fun getStyle(): DialogStyle = DialogStyle.COMPACT

  companion object {
    private val DEFAULT_WINDOW_SIZE = Dimension(JBUI.scale(1280), JBUI.scale(640))

    private val MODEL_INFO_PANEL_HORIZONTAL_PADDING = JBUI.scale(20)
    private val MODEL_INFO_PANEL_TOP_PADDING = JBUI.scale(10)
    private const val INNER_SPLITTER_PROPORTION = 0.45f
    private const val OUTER_SPLITTER_PROPORTION = 0.2f

    @NlsSafe private val DISABLED_OK_BUTTON_TOOLTIP = StringBuilder().append(
      HuggingFaceProBundle.message("python.hugging.face.model.choice.no.template.for.model"),
      HtmlChunk.br(),
      HuggingFaceProBundle.message("python.hugging.face.model.choice.please.visit.model.page")
    ).toString()
  }
}
