package com.intellij.dataWrangler.impl.view

import com.intellij.dataWrangler.executor.CodePreviewProvider
import com.intellij.dataWrangler.executor.DataWranglerContext
import com.intellij.dataWrangler.operations.DataWranglerCommand
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import javax.swing.JPanel

internal class CodePreviewPanel<C : DataWranglerContext>(
  private val context: C,
  private val command: DataWranglerCommand<C>,
  private val codeProvider: CodePreviewProvider<C>,
  parentDisposable: Disposable,
) : JPanel(BorderLayout()) {

  private var editor = createAndSetupEditorWithHighlighting(context.getProject(), codeProvider.getFileType(), parentDisposable)

  init {
    val panel = RoundedCornersJBPanel().apply { border = JBUI.Borders.empty(10) }
    panel.add(editor.component)
    add(panel)
  }

  suspend fun updateCodePreview() {
    withContext(Dispatchers.EDT) {
      //TODO: wrong, global context is used for previous operation preview
      val newCode = codeProvider.getCodePreview(context, command)
      edtWriteAction {
        editor.document.setText(newCode.trimEnd())
      }
      editor.component.repaint() // needed when command code is changed
      editor.component.revalidate()
    }
  }

  private fun createAndSetupEditorWithHighlighting(project: Project, fileType: FileType = PlainTextFileType.INSTANCE, parentDisposable: Disposable): Editor {
    val document = EditorFactory.getInstance().createDocument("")
    val editorFactory = EditorFactory.getInstance()
    val editor = editorFactory.createViewer(document, project) as EditorEx
    Disposer.register(parentDisposable) {
      editorFactory.releaseEditor(editor)
    }

    editor.getSettings().apply {
      isVirtualSpace = false
      isLineMarkerAreaShown = false
      isIndentGuidesShown = false
      isLineNumbersShown = false
      isFoldingOutlineShown = false
      isCaretRowShown = false
      isUseSoftWraps = true
      additionalLinesCount = 0
      lineCursorWidth = 1
      isVirtualSpace = false
    }

    editor.backgroundColor = UIUtil.getListSelectionBackground(false)
    editor.setVerticalScrollbarVisible(false)
    editor.setHorizontalScrollbarVisible(false)

    val highlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(project, fileType)
    editor.setHighlighter(highlighter)

    return editor
  }
}