package com.jetbrains.bigdatatools.common.ui

import com.intellij.lang.Language
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.components.JBScrollPane
import com.jetbrains.bigdatatools.common.rfs.client.SchemaInfoPart
import com.jetbrains.bigdatatools.common.rfs.localcache.MetaInfo
import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JPanel
import kotlin.math.max
import kotlin.math.min

class ParquetDDLDialog(private val project: Project, private val fileName: String, private val data: MetaInfo?) : DialogWrapper(null,
                                                                                                                                false) {
  init {
    init()
    title = MessagesBundle.message("parquet.ddl.dialog.title")
  }

  override fun createActions(): Array<Action> {
    return arrayOf(okAction)
  }

  private fun appendToStringBuilder(sb: StringBuilder, info: SchemaInfoPart) {
    val split = info.text.split('\n')
    var shift = 1
    split.forEach {
      if (it.endsWith("}")) {
        shift--
      }
      sb.append(" ".repeat(shift * 4)).append(it).append('\n')
      if (it.endsWith("{")) {
        shift++
      }
    }
  }

  private fun getJsonDdl(): String {
    val data = data ?: return ""

    val sb = StringBuilder()

    if (data.errors.isNotEmpty()) {
      sb.append(MessagesBundle.message("parquet.ddl.dialog.errors")).append('\n')
      data.errors.forEach {
        sb.append(it).append('\n')
      }
    }

    sb.append("message ${fileName}_scheme {").append('\n')
    data.scheme.toList().forEach { appendToStringBuilder(sb, it) }
    sb.append("}")
    return sb.toString()
  }

  override fun createCenterPanel(): JComponent {

    val editor = EditorFactory.getInstance().createViewer(EditorFactory.getInstance().createDocument(""), project, EditorKind.PREVIEW)
    if (SystemInfo.isWindows) (editor.document as? DocumentImpl)?.setAcceptSlashR(true)

    Disposer.register(disposable) {
      EditorFactory.getInstance().releaseEditor(editor)
    }

    editor.settings.apply {
      isLineMarkerAreaShown = false
      isIndentGuidesShown = false
      isLineNumbersShown = false
      isFoldingOutlineShown = false
    }

    editor.setBorder(null)

    val result = editor.component

    showSchemeText(editor as EditorEx)

    val panel = JPanel(BorderLayout())

    val scrollPane = JBScrollPane(result).apply {
      preferredSize = Dimension(min(800, max(350, (result.preferredSize.width * 1.2).toInt())),
                                min(600, max(250, (result.preferredSize.height * 1.1).toInt())))
    }

    return JPanel(BorderLayout()).apply {
      add(panel, BorderLayout.NORTH)
      add(scrollPane, BorderLayout.CENTER)
    }
  }

  private fun showSchemeText(editor: EditorEx) {
    runWriteAction {
      if (!editor.isDisposed) {
        val fileType = Language.findLanguageByID("protobuf")?.associatedFileType
        if (fileType != null)
          editor.highlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(project, fileType)
        editor.document.setText(getJsonDdl())
      }
    }
  }

  override fun getDimensionServiceKey() = "com.intellij.bigdatatools.visualization.inlays.components.ddl.bounds"
}