package com.intellij.bigdatatools.zeppelin.editor

import com.intellij.bigdatatools.notebooks.core.api.NotebookKeys
import com.intellij.bigdatatools.notebooks.core.impl.controllers.NoteCellsDelimiterController
import com.intellij.bigdatatools.notebooks.core.impl.controllers.NoteGuardBlockHandler
import com.intellij.bigdatatools.notebooks.core.impl.controllers.NoteLineNumberingController
import com.intellij.bigdatatools.notebooks.core.impl.controllers.NoteLineWrapController
import com.intellij.bigdatatools.notebooks.core.impl.controllers.NoteUsedInterpreterCollector
import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.bigdatatools.visualization.inlays.InlaysManager
import com.intellij.bigdatatools.zeppelin.components.containers.LocalNoteContainer
import com.intellij.bigdatatools.zeppelin.components.containers.ZeppelinRemoteNoteContainer
import com.intellij.bigdatatools.zeppelin.components.containers.editor.connection.ZeppelinNoteLoadDecorator
import com.intellij.bigdatatools.zeppelin.components.containers.service.ZeppelinParagraphCompletedListener
import com.intellij.bigdatatools.zeppelin.constants.ZeppelinConstants
import com.intellij.bigdatatools.zeppelin.controllers.editor.BlockRunCellController
import com.intellij.bigdatatools.zeppelin.controllers.editor.ZepPythonSdkListener
import com.intellij.bigdatatools.zeppelin.controllers.editor.ZeppelinNoteEventLogger
import com.intellij.bigdatatools.zeppelin.controllers.editor.ZeppelinNoteLoadPromise
import com.intellij.bigdatatools.zeppelin.controllers.editor.ZeppelinNoteLoadPromise.onNoteLoaded
import com.intellij.bigdatatools.zeppelin.controllers.editor.ZeppelinParagraphFoldingController
import com.intellij.bigdatatools.zeppelin.controllers.editor.ZeppelinParagraphTitleController
import com.intellij.bigdatatools.zeppelin.drivers.ZeppelinDriverManager
import com.intellij.bigdatatools.zeppelin.editor.actions.ZeppelinSelectAllAction
import com.intellij.bigdatatools.zeppelin.editor.gutter.ZeppelinNoteGutterController
import com.intellij.bigdatatools.zeppelin.file.NotebookFileUtil
import com.intellij.bigdatatools.zeppelin.file.ZeppelinFileType
import com.intellij.bigdatatools.zeppelin.file.ZeppelinRemoteFile
import com.intellij.bigdatatools.zeppelin.file.isNoteFile
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinNotebook
import com.intellij.bigdatatools.zeppelin.statistics.ZeppelinFileLoadStatistic
import com.intellij.bigdatatools.zeppelin.statistics.ZeppelinNotebookUsageCollector
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.scratch.ScratchUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.impl.FileDocumentManagerBase
import com.intellij.openapi.fileEditor.impl.FileDocumentManagerImpl
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorImpl
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.bigdatatools.common.table.editor.LoadingLightVirtualFile
import com.jetbrains.bigdatatools.common.table.editor.NotebookProviderMarker
import com.jetbrains.bigdatatools.common.util.invokeAndWaitWriteAction
import com.jetbrains.bigdatatools.common.util.invokeLater
import com.jetbrains.bigdatatools.common.util.toPresentableText
import java.io.OutputStreamWriter
import java.lang.ref.WeakReference

internal class ZeppelinEditorProvider : FileEditorProvider, DumbAware, NotebookProviderMarker {
  override fun getPolicy() = FileEditorPolicy.HIDE_DEFAULT_EDITOR

  override fun createEditor(project: Project, file: VirtualFile): FileEditor {
    invokeAndWaitWriteAction {
      file.refresh(false, false)
    }

    val template = FileTemplateManager
      .getInstance(project)
      .getInternalTemplate(ZeppelinConstants.TEMPLATE_FILE_NAME)


    if (ScratchUtil.isScratch(file))
      invokeAndWaitWriteAction {
        OutputStreamWriter(file.getOutputStream(this), Charsets.UTF_8).use {
          it.write(template.text)
        }
      }

    val notebookSourceFile = try {
      createNotebookSourceFile(file)
    }
    catch (t: Throwable) {
      invokeLater {
        ZeppelinNoteLoadDecorator.closeWithError(project, file, Throwable(t.toPresentableText()))
      }
      return TextEditorProvider.getInstance().createEditor(project, file) as PsiAwareTextEditorImpl
    }

    val note = notebookSourceFile.notebook as ZeppelinNotebook
    val textEditor = TextEditorProvider.getInstance().createEditor(project, notebookSourceFile) as PsiAwareTextEditorImpl

    val zeppelinEditor = ZeppelinEditor(textEditor, "zeppelin-editor")
    textEditor.putUserData(PARENT_ZEPPELIN_EDITOR, WeakReference(zeppelinEditor))

    if (note.cells.isEmpty()) {
      invokeLater {
        ZeppelinNoteLoadDecorator.closeWithError(zeppelinEditor.project, zeppelinEditor.file.originFile,
                                                 Throwable(ZepMessagesBundle.message("filesystem.open.corrupted.note.message")))
      }
      return zeppelinEditor
    }

    installRequiredServices(zeppelinEditor, note)

    val originalAction = ActionManager.getInstance().getAction("\$SelectAll")
    val selectAllAction = ZeppelinSelectAllAction()
    selectAllAction.registerCustomShortcutSet(originalAction.shortcutSet, textEditor.editor.component)

    file.putUserData(NotebookKeys.NOTEBOOK_VIRTUAL_FILE, notebookSourceFile)
    ZeppelinFileLoadStatistic.fileEditorCreated(file)
    return zeppelinEditor
  }

  private fun installRequiredServices(zeppelinEditor: ZeppelinEditor, notebook: ZeppelinNotebook) {
    ZeppelinNoteLoadPromise.init(zeppelinEditor)

    Disposer.register(zeppelinEditor, NoteUsedInterpreterCollector.getOrSetup(zeppelinEditor))

    Disposer.register(zeppelinEditor, ZeppelinNoteEventLogger(zeppelinEditor.note))
    Disposer.register(zeppelinEditor, ZeppelinNoteGutterController(zeppelinEditor))
    Disposer.register(zeppelinEditor, NoteCellsDelimiterController(zeppelinEditor.editor, notebook))

    ZeppelinParagraphFoldingController.update(zeppelinEditor.editor, notebook)
    NoteLineNumberingController.update(zeppelinEditor.editor, notebook)
    NoteLineWrapController.updateEditor(zeppelinEditor.editor)

    Disposer.register(zeppelinEditor, ZeppelinParagraphTitleController(zeppelinEditor))
    Disposer.register(zeppelinEditor, BlockRunCellController(zeppelinEditor))
    Disposer.register(zeppelinEditor, getContainer(zeppelinEditor))
    Disposer.register(zeppelinEditor, ZeppelinNoteLoadDecorator(zeppelinEditor))
    Disposer.register(zeppelinEditor, ZeppelinParagraphCompletedListener(zeppelinEditor))
    Disposer.register(zeppelinEditor, ZepPythonSdkListener(zeppelinEditor))

    NoteGuardBlockHandler.setupFor(zeppelinEditor.editor)

    initInlays(zeppelinEditor)
  }

  private fun initInlays(zeppelinEditor: ZeppelinEditor) {
    zeppelinEditor.onNoteLoaded {
      if (it == null) {
        invokeLater {
          InlaysManager.getInstance().onNotebookOpened(zeppelinEditor.editor)
        }
        ZeppelinNotebookUsageCollector.logNoteOpened(zeppelinEditor.project,
                                                     zeppelinEditor.note, zeppelinEditor.file, true)
      }
    }
  }

  private fun getContainer(zeppelinEditor: ZeppelinEditor): Disposable {
    return if (NotebookFileUtil.isRemote(zeppelinEditor.file)) {
      val configId = NotebookFileUtil.getConfigId(zeppelinEditor.file) ?: error("Config id is not found")
      val driver = ZeppelinDriverManager.getDriver(zeppelinEditor.project, configId) ?: error("Driver for editor is not found")
      ZeppelinRemoteNoteContainer(zeppelinEditor, driver.connectionManager)
    }
    else {
      LocalNoteContainer(zeppelinEditor)
    }
  }

  private fun createNotebookSourceFile(file: VirtualFile): NotebookVirtualFile {
    file.refresh(false, true)
    val notebookContentReader = getCachedDocumentContent(file)?.reader() ?: file.inputStream.reader()
    val zeppelinNotebook = ZeppelinNotebook(notebookContentReader)
    notebookContentReader.close()
    val isRequiredSaveOriginalFile = file !is ZeppelinRemoteFile
    val notebookSourceFile = NotebookVirtualFile(file, zeppelinNotebook, isRequiredSaveOriginalFile)
    notebookSourceFile.setCustomFileType(ZeppelinFileType)
    registerDocument(file)
    return notebookSourceFile
  }

  private fun registerDocument(originalFile: VirtualFile) {
    val document = FileDocumentManagerImpl.getInstance().getDocument(originalFile)
    originalFile.putUserData(FileDocumentManagerBase.HARD_REF_TO_DOCUMENT_KEY, document)
  }

  private fun getCachedDocumentContent(file: VirtualFile): String? {
    val fileDocumentManager = FileDocumentManager.getInstance()
    val document = fileDocumentManager.getDocument(file) ?: return null
    if (fileDocumentManager.isPartialPreviewOfALargeFile(document)) return null
    return document.text
  }

  override fun accept(project: Project, file: VirtualFile): Boolean {
    return file !is LoadingLightVirtualFile && file.isNoteFile && file !is NotebookVirtualFile
  }

  override fun getEditorTypeId(): String = "zeppelin-editor"
}

internal val PARENT_ZEPPELIN_EDITOR: Key<WeakReference<ZeppelinEditor>> = Key<WeakReference<ZeppelinEditor>>("ZeppelinFileEditorParent")