package com.intellij.aiplayground.python

import com.intellij.aiplayground.models.statistic.PlaygroundCollector
import com.intellij.aiplayground.models.utils.AiPlaygroundCoroutine
import com.intellij.aiplayground.models.utils.AiPlaygroundPluginDisposable
import com.intellij.aiplayground.ui.AIPlaygroundColors
import com.intellij.aiplayground.ui.icons.AiplaygroundUIIcons
import com.intellij.aiplayground.ui.utils.isChinaRegion
import com.intellij.codeInsight.hints.ChangeListener
import com.intellij.codeInsight.hints.ImmediateConfigurable
import com.intellij.codeInsight.hints.InlayGroup
import com.intellij.codeInsight.hints.InlayHintsCollector
import com.intellij.codeInsight.hints.InlayHintsProvider
import com.intellij.codeInsight.hints.InlayHintsSettings
import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.InlayHintsUtils
import com.intellij.codeInsight.hints.InlayPresentationFactory
import com.intellij.codeInsight.hints.NoSettings
import com.intellij.codeInsight.hints.SettingsKey
import com.intellij.codeInsight.hints.presentation.ChangeOnHoverPresentation
import com.intellij.codeInsight.hints.presentation.InlayButtonPresentationFactory
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.PresentationFactory
import com.intellij.codeInsight.hints.presentation.StaticDelegatePresentation
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.event.BulkAwareDocumentListener
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderEx
import com.intellij.openapi.util.getAndUpdateUserData
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.util.childrenOfType
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.intellij.util.ui.UIUtil
import com.jetbrains.python.PythonLanguage
import com.jetbrains.python.psi.PyPlainStringElement
import kotlinx.coroutines.launch
import org.intellij.lang.annotations.Language
import java.awt.Point
import java.awt.event.MouseEvent
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor

private val KEY: SettingsKey<NoSettings> = SettingsKey<NoSettings>(AiPlaygroundInlayProvider.PROVIDER_ID)

class AiPlaygroundInlayProvider : InlayHintsProvider<NoSettings> {
  companion object {
    const val PROVIDER_ID: String = "aiplayground.inlay.hint.for.prompts"
  }

  override val isVisibleInSettings: Boolean
    get() = enabled

  private val enabled: Boolean
    get() {
      return Registry.`is`("aiplayground.intention.for.prompts.enabled")
             && !isChinaRegion()
    }

  override fun getCollectorFor(file: PsiFile, editor: Editor, settings: NoSettings, sink: InlayHintsSink): InlayHintsCollector? {
    if (!enabled) return null
    if (!isLanguageSupported(file.language)) return null
    if (isChinaRegion()) return null

    return AiPlaygroundInlayCollector()
  }

  override fun createConfigurable(settings: NoSettings): ImmediateConfigurable {
    return object : ImmediateConfigurable {
      override fun createComponent(listener: ChangeListener) = panel { }
    }
  }

  override fun createSettings(): NoSettings = NoSettings()

  override val name: String = AIPlaygroundPythonBundle.message("inlay.hints.settings.name.key")
  override val key: SettingsKey<NoSettings> = KEY
  @Suppress("PyInterpreter")
  override val previewText: String
    @Language("Python")
    get() = """
      prompt = ${"\"\"\""}
      Solve this math problem step by step:

      Problem: {problem}
      ${"\"\"\""}
    """.trimIndent()
  override val description: String = AIPlaygroundPythonBundle.message("inlay.hints.settings.text")

  override val group: InlayGroup = InlayGroup.OTHER_GROUP

  override fun isLanguageSupported(language: com.intellij.lang.Language): Boolean = language is PythonLanguage
}

private val AI_PLAYGROUND_HIGHLIGHTER_KEY = Key<List<RangeHighlighter>>("AI_PLAYGROUND_HIGHLIGHTER_KEY")
val perEditorListenerMap: ConcurrentHashMap<Editor, DocumentListener> = ConcurrentHashMap()

private class AiPlaygroundInlayCollector() : InlayHintsCollector, DocumentListener {
  data class AIPlaygroundHighlighter(
    val textAttributesKey: TextAttributesKey?,
    val startOffset: Int,
    val endOffset: Int,
    val layer: Int,
    val targetArea: HighlighterTargetArea,
  )

  fun isValidEditor(editor: Editor): Boolean {
    val editorProject = editor.getProject()
    return editorProject != null
           && !editorProject.isDisposed()
           && !editor.isDisposed() &&
           UIUtil.isShowing(editor.getContentComponent())
  }

  override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
    val promptElement = element.asPromptElementOrNull() ?: return true

    val inlayPresentation = addInlayPresentation(editor, element)

    sink.addInlineElement(
      promptElement.textRange.endOffset,
      true,
      inlayPresentation,
      false
    )

    perEditorListenerMap.computeIfAbsent(editor) {
      val listener = HighlightRemoverOnDocumentChange(editor)
      // it should be disposed with Inlay, but I can't access it, so I dispose it with either editor or plugin
      val disposable = Disposer.newDisposable()
      Disposer.register(AiPlaygroundPluginDisposable.getInstance(element.project), disposable)
      EditorUtil.disposeWithEditor(editor, disposable)
      editor.document.addDocumentListener(listener, disposable)
      listener
    }
    PlaygroundCollector.logInlayHintShown(editor)

    return true
  }

  inner class HighlightRemoverOnDocumentChange(val editor: Editor) : BulkAwareDocumentListener.Simple {
    override fun beforeDocumentChange(document: Document) {
      ensureNoHighlighting(editor)
    }
  }


  private fun createAndShowPopup(editor: Editor, project: Project?, actionGroup: DefaultActionGroup, e: MouseEvent) {
    val dataContext = SimpleDataContext.builder()
      .add(CommonDataKeys.EDITOR, editor)
      .add(CommonDataKeys.PROJECT, project)
      .build()
    val popup = JBPopupFactory.getInstance()
      .createActionGroupPopup(
        null,
        actionGroup,
        dataContext,
        JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
        false
      )
    popup.showInScreenCoordinates(editor.contentComponent, e.locationOnScreen)
  }

  private fun showContextMenu(
    e: MouseEvent,
    editor: Editor,
  ) {
    val popupActions = InlayHintsUtils.getDefaultInlayHintsProviderPopupActions(KEY) {
      AIPlaygroundPythonBundle.message("inlay.hints.name")
    }

    val actionGroup = DefaultActionGroup().apply { addAll(popupActions) }
    createAndShowPopup(editor, editor.project, actionGroup, e)
  }

  private fun addInlayPresentation(
    editor: Editor,
    element: PsiElement,
  ): InlayPresentation {
    val smartElement = SmartPointerManager
      .getInstance(element.project)
      .createSmartPsiElementPointer(element)
    val factory = PresentationFactory(editor)
    val inlayButtonPresentationFactory = InlayButtonPresentationFactory(editor, factory)

    val hintText = AIPlaygroundPythonBundle.message("inlay.hints.text")
    val tooltipText = AIPlaygroundPythonBundle.message("inlay.hints.tooltip.text")
    val iconToUse = AiplaygroundUIIcons.PurpleAIPlayground


    var defaultButton: InlayPresentation
    var hoveredButton: InlayPresentation
    if (hintText != "") {
      defaultButton = inlayButtonPresentationFactory.iconAndText(iconToUse, hintText).withTooltip(tooltipText).build()
      hoveredButton = inlayButtonPresentationFactory.iconAndText(iconToUse, hintText).withTooltip(tooltipText).buildHovered()
    }
    else {
      defaultButton = inlayButtonPresentationFactory.icon(iconToUse).withTooltip(tooltipText).build()
      hoveredButton = inlayButtonPresentationFactory.icon(iconToUse).withTooltip(tooltipText).buildHovered()
    }

    val buttonWithHoverEffect = ChangeOnHoverPresentation(defaultButton, { hoveredButton })

    val executor = Executor { task ->
      service<AiPlaygroundCoroutine>().coroutineScope.launch {
        task.run()
      }
    }

    val clickSupport = object : StaticDelegatePresentation(buttonWithHoverEffect) {
      override fun mousePressed(event: MouseEvent, translated: Point) {
        // The button also works if clicked in settings. Should it be disabled?
        when (event.button) {
          MouseEvent.BUTTON1 -> {
            val el = ReadAction.compute<PsiElement?, Throwable> { smartElement.element } ?: return
            openPlaygroundWithPrompt(element.project, el)
            PlaygroundCollector.logInlayHintClicked()
          }
          MouseEvent.BUTTON3 -> {
            showContextMenu(event, editor)
            PlaygroundCollector.logInlayHintRightClicked()
          }
        }
      }
    }

    val hoverListener = HoverListener(editor, smartElement, executor)

    val hoverHighlightSupport = factory.onHover(
      clickSupport,
      onHoverListener = hoverListener,
    )

    return hoverHighlightSupport
  }

  @RequiresEdt
  private fun cleanHighlightersIfAny(editor: Editor) {
    val highlighters = (editor as UserDataHolderEx).getAndUpdateUserData(AI_PLAYGROUND_HIGHLIGHTER_KEY) { null } ?: return
    val markupModel = editor.markupModel
    for (highlighter in highlighters) {
      markupModel.removeHighlighter(highlighter)
    }
  }

  private fun ensureNoHighlighting(editor: Editor) {
    ApplicationManager.getApplication().invokeLater {
      cleanHighlightersIfAny(editor)
    }
  }

  /** inspired by [com.intellij.codeInsight.highlighting.BackgroundHighlighterKt.highlightSelection] */
  inner class HoverListener(
    private val editor: Editor,
    private val smartElement: SmartPsiElementPointer<PsiElement>,
    private val executor: Executor,
  ) : InlayPresentationFactory.HoverListener {
    private fun ensureHighlighting() {
      val stamp = editor.document.modificationStamp

      ReadAction.nonBlocking<List<AIPlaygroundHighlighter>> {
        val element = smartElement.element ?: return@nonBlocking emptyList()
        buildList {
          for (stringElement in element.parent.childrenOfType<PyPlainStringElement>()) {
            add(
              AIPlaygroundHighlighter(
                AIPlaygroundColors.AI_INLAY_HINT_HIGHLIGHT_PROMPT,
                stringElement.textRange.startOffset,
                stringElement.textRange.endOffset,
                HighlighterLayer.SELECTION + 1,
                HighlighterTargetArea.EXACT_RANGE
              )
            )
            if (Registry.`is`("aiplayground.inlay.hint.for.prompts.highlight.variables")) {
              val placeholderPositions = MLPromptStringsDetector.getInstance().getPlaceholderPositions(stringElement.text)
              for (placeholderPosition in placeholderPositions) {
                add(
                  AIPlaygroundHighlighter(
                    AIPlaygroundColors.AI_INLAY_HINT_HIGHLIGHT_PROMPT_PLACEHOLDER_BRACES,
                    stringElement.textRange.startOffset + placeholderPosition.start,
                    stringElement.textRange.startOffset + placeholderPosition.start + 1,
                    HighlighterLayer.SELECTION + 2,
                    HighlighterTargetArea.EXACT_RANGE
                  )
                )
                add(
                  AIPlaygroundHighlighter(
                    AIPlaygroundColors.AI_INLAY_HINT_HIGHLIGHT_PROMPT_PLACEHOLDER_TEXT,
                    stringElement.textRange.startOffset + placeholderPosition.start + 1, // +1 because of the quotes around the placeholder
                    stringElement.textRange.startOffset + placeholderPosition.end - 1, // -1 because of the quotes around the placeholder
                    HighlighterLayer.SELECTION + 2,
                    HighlighterTargetArea.EXACT_RANGE
                  )
                )
                add(
                  AIPlaygroundHighlighter(
                    AIPlaygroundColors.AI_INLAY_HINT_HIGHLIGHT_PROMPT_PLACEHOLDER_BRACES,
                    stringElement.textRange.startOffset + placeholderPosition.end - 1,
                    stringElement.textRange.startOffset + placeholderPosition.end,
                    HighlighterLayer.SELECTION + 2,
                    HighlighterTargetArea.EXACT_RANGE
                  )
                )
              }
            }
          }
        }
      }
        .coalesceBy(AiPlaygroundInlayCollector::class.java, editor)
        .expireWhen { editor.document.modificationStamp != stamp || !isValidEditor(editor) || smartElement.element == null }
        .finishOnUiThread(ModalityState.nonModal()) { results ->
          val element = smartElement.element ?: return@finishOnUiThread
          if (!isValidEditor(editor) || editor.document.modificationStamp != stamp || !element.isValid || results.isEmpty()) return@finishOnUiThread
          cleanHighlightersIfAny(editor)
          val highlighters = results.map {
            editor.markupModel.addRangeHighlighter(it.textAttributesKey,
                                                   it.startOffset,
                                                   it.endOffset,
                                                   it.layer,
                                                   it.targetArea)
          }
          editor.putUserData(AI_PLAYGROUND_HIGHLIGHTER_KEY, highlighters)
          PlaygroundCollector.logInlayHintHovered(editor)
        }
        .submit(executor)
    }

    override fun onHover(event: MouseEvent, translated: Point) {
      ensureHighlighting()
    }

    override fun onHoverFinished() {
      ensureNoHighlighting(editor)
    }
  }

}

internal class AIPlaygroundSettingsListener(private val project: Project) : InlayHintsSettings.SettingsListener {
  override fun settingsChanged() {
    if (InlayHintsSettings.instance().hintsEnabled(KEY, PythonLanguage.INSTANCE)) {
      PlaygroundCollector.logInlayHintEnabled()
    }
    else {
      PlaygroundCollector.logInlayHintDisabled()
    }
  }
}
