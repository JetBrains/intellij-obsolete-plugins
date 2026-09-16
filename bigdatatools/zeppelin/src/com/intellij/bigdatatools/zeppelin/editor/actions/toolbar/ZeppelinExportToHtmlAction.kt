package com.intellij.bigdatatools.zeppelin.editor.actions.toolbar

import com.intellij.bigdatatools.visualization.inlays.settings.InlaysSettings
import com.intellij.bigdatatools.zeppelin.editor.NoteActionsIds
import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.editor.highlighter.EditorHighlighter
import com.intellij.openapi.editor.impl.DocumentMarkupModel
import com.intellij.openapi.editor.markup.MarkupModel
import com.intellij.openapi.editor.richcopy.SyntaxInfoBuilder
import com.intellij.openapi.editor.richcopy.model.SyntaxInfo
import com.intellij.openapi.editor.richcopy.settings.RichCopySettings
import com.intellij.openapi.editor.richcopy.view.HtmlTransferableData
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import java.io.IOException
import java.nio.file.Path
import kotlin.reflect.KFunction
import kotlin.reflect.full.functions
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaType

/**
 * Action saves all notebook cells code as formatted HTML.
 */
// TODO here we have big reflective hack in getReflectedMethods(), but the alternative was to write 2000 lines of code
//      with all of SyntaxInfoBuilder, MyMarkupIterator and SyntaxInfoBuilder.Context
//      or to copy 2000 lines of code from com.intellij.openapi.editor.richcopy
//      To ensure that this code will work in future, ZeppelinExportToHtmlActionTest exist.
class ZeppelinExportToHtmlAction(zeppelinEditor: ZeppelinEditor?) : ZeppelinEditorDumbAwareAction(zeppelinEditor,
                                                                                                 NoteActionsIds.EXPORT_TO_HTML,
                                                                                                 ZepMessagesBundle.message(
                                                                                                   "action.export.html"),
                                                                                                 null,
                                                                                                 AllIcons.FileTypes.Html) {

  constructor() : this(null)

  private fun getNoteAsHtml(actualEditor: ZeppelinEditor): String? {
    val methods = getReflectedMethods() ?: return null

    val notebookFile = actualEditor.file
    val cells = notebookFile.notebook.cells
    val editorColorsScheme: EditorColorsScheme = actualEditor.editor.colorsScheme
    val text = actualEditor.editor.document.text

    val schemeToUse = RichCopySettings.getInstance().getColorsScheme(editorColorsScheme)

    val highlighter = (actualEditor.editor as EditorEx).highlighter
    if (editorColorsScheme !== schemeToUse) {
      highlighter.setColorScheme(schemeToUse)
    }

    val markupModel = DocumentMarkupModel.forDocument(actualEditor.editor.document, actualEditor.editor.project!!, false)
    val indentSymbolsToStrip = 0
    // val context =  SyntaxInfoBuilder.Context(text, schemeToUse, indentSymbolsToStrip)
    val context = methods.contextConstructor.call(text, schemeToUse, indentSymbolsToStrip)

    cells.forEach {

      val startOffset = it.textRange.startOffset
      val endOffset = it.textRange.endOffset

      // val markupIterator = SyntaxInfoBuilder.createMarkupIterator(highlighter, text, schemeToUse, markupModel, startOffset, endOffset)
      val markupIterator = methods.createMarkupIterator.call(highlighter, text, schemeToUse, markupModel, startOffset, endOffset)
      try {
        //  context.iterate(markupIterator, endOffset)
        methods.contextIterate.call(context, markupIterator, endOffset)
      }
      finally {
        //  markupIterator.dispose()
        methods.markupIteratorDispose.call(markupIterator)
      }
    }

    val syntaxInfo = methods.contextFinish.call(context) as SyntaxInfo // context.finish()
    val data = HtmlTransferableData(syntaxInfo, EditorUtil.getTabSize(actualEditor.editor))
    data.setRawText(text)
    return data.buffer.toString().replace(">%##<", "><hr><")
  }

  override fun actionPerformed(e: AnActionEvent) {
    super.actionPerformed(e)

    val (actualEditor, project) = actualContext(e) ?: return
    val html = getNoteAsHtml(actualEditor) ?: return

    val descriptor = FileSaverDescriptor(ZepMessagesBundle.message("dialog.export.html.title"), "", "html")
    val chooser = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
    val fileWrapper = chooser.save(Path.of(project.basePath ?: "."), actualEditor.file.nameWithoutExtension + ".html") ?: return

    InlaysSettings.getInstance().chartExportPath = fileWrapper.file.path

    try {
      fileWrapper.file.writeText(html)

      BrowserUtil.open(fileWrapper.file.absolutePath)
    }
    catch (exception: IOException) {
      logger.warn("Failed to save notebook to html " + exception.message)
    }
  }

  data class Methods(
    val contextConstructor: KFunction<Any>,
    val contextFinish: KFunction<*>,
    val contextIterate: KFunction<*>,
    val createMarkupIterator: KFunction<*>,
    val markupIteratorDispose: KFunction<*>
  )

  companion object {
    private val logger = Logger.getInstance(this::class.java)

    fun getReflectedMethods(): Methods? {
      val contextClass = SyntaxInfoBuilder::class.nestedClasses.firstOrNull {
        it.qualifiedName == "com.intellij.openapi.editor.richcopy.SyntaxInfoBuilder.Context"
      } ?: return null

      val contextConstructor = contextClass.constructors.firstOrNull {
        it.parameters.size == 3 &&
        it.parameters[0].type.javaType == CharSequence::class.java &&
        it.parameters[1].type.javaType == EditorColorsScheme::class.java &&
        it.parameters[2].type.javaType == Int::class.java
      } ?: return null

      contextConstructor.isAccessible = true

      val contextFinish = contextClass.functions.firstOrNull {
        it.name == "finish" &&
        it.parameters.size == 1 &&
        it.parameters[0].type.javaType == contextClass.java
      } ?: return null
      contextFinish.isAccessible = true

      val myMarkupIteratorClass = SyntaxInfoBuilder::class.nestedClasses.firstOrNull {
        it.qualifiedName == "com.intellij.openapi.editor.richcopy.SyntaxInfoBuilder.MyMarkupIterator"
      } ?: return null

      val contextIterate = contextClass.functions.firstOrNull {
        it.name == "iterate" && it.parameters.size == 3 &&
        it.parameters[0].type.javaType == contextClass.java &&
        it.parameters[1].type.javaType == myMarkupIteratorClass.java &&
        it.parameters[2].type.javaType == Int::class.java
      } ?: return null
      contextIterate.isAccessible = true

      val createMarkupIterator = SyntaxInfoBuilder::class.functions.firstOrNull {
        it.name == "createMarkupIterator" &&
        it.parameters.size == 6
        it.parameters[0].type.javaType == EditorHighlighter::class.java &&
        it.parameters[1].type.javaType == CharSequence::class.java &&
        it.parameters[2].type.javaType == EditorColorsScheme::class.java &&
        it.parameters[3].type.javaType == MarkupModel::class.java &&
        it.parameters[4].type.javaType == Int::class.java &&
        it.parameters[5].type.javaType == Int::class.java
      } ?: return null

      createMarkupIterator.isAccessible = true

      val disposeMarkupIterator = myMarkupIteratorClass.functions.firstOrNull {
        it.name == "dispose" &&
        it.parameters.size == 1 &&
        it.parameters[0].type.javaType == myMarkupIteratorClass.java
      } ?: return null
      disposeMarkupIterator.isAccessible = true

      return Methods(contextConstructor, contextFinish, contextIterate, createMarkupIterator, disposeMarkupIterator)
    }
  }
}