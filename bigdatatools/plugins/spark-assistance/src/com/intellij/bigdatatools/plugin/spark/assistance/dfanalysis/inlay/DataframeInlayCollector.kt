package com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.inlay

import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.ParseUtil
import com.intellij.bigdatatools.plugin.spark.assistance.statistic.SparkStatisticHelper
import com.intellij.bigdatatools.plugin.spark.assistance.util.SAMessagesBundle
import com.intellij.codeInsight.hints.FactoryInlayHintsCollector
import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.presentation.DynamicDelegatePresentation
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer

abstract class DataframeInlayCollector(editor: Editor) : FactoryInlayHintsCollector(editor) {

  fun getInlayText(inlayData: SchemaSampleFileData): String {
    return ".schema(\"${inlayData.schemaDdlString}\")"
  }

  abstract fun getReadCallArguments(reference: PsiElement): DataframeInlayPosition?

  override fun collect(element: PsiElement,
                       editor: Editor,
                       sink: InlayHintsSink): Boolean {
    val inlayPosition = getReadCallArguments(element) ?: return true
    val offset = inlayPosition.textRange.endOffset
    val schemaSampleFileService = element.project.service<DataframeSampleFileService>()

    val presentationWrapper = DynamicDelegatePresentation(factory.text(""))
    fun computePresentation(): InlayPresentation {
      val key = inlayPosition.key
      val inlayData = schemaSampleFileService.getAttached(key)
      inlayPosition.statisticHelper.logInlay(element, inlayPosition.pathIsConstant, inlayData != null)
      val innerPresentation = if (inlayData != null) {
        // TODO
        //  statistics
        //  persisting
        //  support other than parquet,
        //  reuse scala utilities from Dima's code,
        //  dialog contains no drivers at first shown after IDE restart (if BDT panel collapsed),
        //  dialog toolbar disappears (make 'refresh' always visible)
        //  double-click in dialog should select instead of opening
        //  open dialog with pre-selected previous path
        val inlayText = getInlayText(inlayData)
        factory.withTooltip(
          ParseUtil.schemaToHtml(ParseUtil.parseDdlString(inlayText)),
          factory.roundWithBackgroundAndSmallInset(
            factory.folding(
              factory.text(if (inlayText.length <= 50) inlayText else inlayText.take(35) + "…")
            ) {
              factory.text(inlayText)
            }
          )
        )
      }
      else {
        factory.roundWithBackground(
          factory.text(SAMessagesBundle.message("inlay.text.dataframe.schema.choose"))
        )
      }
      return factory.referenceOnHover(innerPresentation) { mouseEvent, _ ->
        val selectAndAttachAction = object : AnAction() {
          override fun actionPerformed(e: AnActionEvent) {
            schemaSampleFileService.selectAndAttach(key)
            presentationWrapper.delegate = computePresentation()
          }

          override fun getActionUpdateThread() = ActionUpdateThread.BGT
          override fun update(e: AnActionEvent) {
            e.presentation.text = if (inlayData == null) {
              SAMessagesBundle.message("action.dataframe.schema.select.text")
            }
            else {
              SAMessagesBundle.message("action.dataframe.schema.select.overwrite.text")
            }
          }
        }
        val onClickAction = if (inlayData == null) {
          selectAndAttachAction
        }
        else {
          DefaultActionGroup().apply {
            val insertIntoCodeAction = object : AnAction() {
              override fun actionPerformed(e: AnActionEvent) {
                ApplicationManager.getApplication().runWriteAction {
                  CommandProcessor.getInstance().executeCommand(editor.project, {
                    editor.document.insertString(offset, getInlayText(inlayData))
                  }, SAMessagesBundle.message("command.name.dataframe.schema.insert.into.code"), this@DataframeInlayCollector)
                }
              }

              override fun getActionUpdateThread() = ActionUpdateThread.BGT
              override fun update(e: AnActionEvent) {
                e.presentation.text = SAMessagesBundle.message("action.dataframe.schema.insert.into.code.text")
              }
            }
            add(insertIntoCodeAction)
            add(selectAndAttachAction)
          }
        }
        ActionManager.getInstance().tryToExecute(onClickAction, mouseEvent, mouseEvent.component, ActionPlaces.EDITOR_INLAY, true)
      }
    }
    presentationWrapper.delegate = computePresentation()
    sink.addInlineElement(offset, true, presentationWrapper, false)
    return true
  }
}

class DataframeInlayPosition(
  val textRange: TextRange,
  val key: SmartPsiElementPointer<*>,
  val statisticHelper: SparkStatisticHelper,
  val pathIsConstant: Boolean
)
