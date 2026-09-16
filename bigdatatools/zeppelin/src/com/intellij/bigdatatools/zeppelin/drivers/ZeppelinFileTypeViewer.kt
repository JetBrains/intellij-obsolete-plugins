package com.intellij.bigdatatools.zeppelin.drivers

import com.intellij.bigdatatools.notebooks.core.impl.editor.NotebookEditorUtils
import com.intellij.bigdatatools.zeppelin.constants.ZeppelinConstants
import com.intellij.bigdatatools.zeppelin.controllers.editor.ZeppelinNoteLoadPromise.onNoteLoaded
import com.intellij.bigdatatools.zeppelin.drivers.fileinfo.ZeppelinFileInfo
import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.bigdatatools.zeppelin.editor.util.ZeppelinEditorUtil
import com.intellij.bigdatatools.zeppelin.file.NotebookFileUtil
import com.intellij.bigdatatools.zeppelin.file.ZeppelinRemoteFile
import com.intellij.bigdatatools.zeppelin.language.ZeppelinLanguage
import com.intellij.bigdatatools.zeppelin.rfs.path.ZeppelinRfsPath
import com.intellij.bigdatatools.zeppelin.statistics.ZeppelinFileLoadStatistic
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.jetbrains.bigdatatools.common.rfs.driver.Driver
import com.jetbrains.bigdatatools.common.rfs.driver.FileInfo
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.driver.fileinfo.SafeResult
import com.jetbrains.bigdatatools.common.rfs.localcache.analyzing.TableOffsetsMap
import com.jetbrains.bigdatatools.common.rfs.view.RfsSyncFileTypeViewer
import com.intellij.bigdatatools.coreUi.util.executeOnPooledThread
import com.jetbrains.bigdatatools.common.util.invokeLater
import java.io.File

class ZeppelinFileTypeViewer : RfsSyncFileTypeViewer() {
  override fun accept(driver: Driver, rfsPath: RfsPath): Boolean = driver is ZeppelinDriver

  override fun acceptDriver(driver: Driver) = driver is ZeppelinDriver

  override fun openViewer(fileInfo: FileInfo, project: Project, requestFocus: Boolean): Unit = invokeAndWaitIfNeeded {
    (fileInfo as ZeppelinFileInfo).openPsiFile(requestFocus, project)
  }

  override fun openSearchResult(project: Project, driver: Driver, rfsPath: RfsPath) {
    driver as ZeppelinDriver
    rfsPath as ZeppelinRfsPath
    val paragraphId = rfsPath.id.split("/").last()
    openFile(project, driver, rfsPath, true, paragraphId)
  }

  fun openFile(project: Project,
               driver: ZeppelinDriver,
               notePath: ZeppelinRfsPath,
               requestFocus: Boolean,
               paragraphId: String = "") = executeOnPooledThread {
    val psiFile = ZeppelinEditorUtil.getPsiOpenedFile(project, driver.connectionData.innerId, notePath.id)
                  ?: createPsiFile(notePath, driver, project)
                  ?: return@executeOnPooledThread
    invokeLater {
      val fileEditorManager = FileEditorManager.getInstance(project)
      val virtualFile = psiFile.virtualFile
      fileEditorManager.openFile(virtualFile, requestFocus, true)

      val zeppelinEditor = NotebookEditorUtils.getNotebookEditor(project, virtualFile) as? ZeppelinEditor
                           ?: return@invokeLater
      zeppelinEditor.onNoteLoaded {
        if (it != null)
          return@onNoteLoaded
        //This big construction is required go to paragraph if note is not open yet
        //because the next frame will be occupied by ZeppelinNoteLoadDecorator
        executeOnPooledThread {
          invokeLater {
            if (paragraphId.isNotBlank())
              ZeppelinEditorUtil.goToParagraph(zeppelinEditor.editor, zeppelinEditor.note, paragraphId)
            else
              NotebookEditorUtils.goToCell(zeppelinEditor.editor, zeppelinEditor.note.cells.first())
          }
        }
      }
    }
  }

  private fun createPsiFile(notePath: ZeppelinRfsPath, driver: ZeppelinDriver, project: Project): PsiFile? {
    val virtualFile = createVirtualFile(project, notePath.id, driver) ?: return null

    return (PsiFileFactory.getInstance(project) as PsiFileFactoryImpl)
             .trySetupPsiForFile(virtualFile, ZeppelinLanguage, true, false)
           ?: throw Exception("Cannot create Psi for Remote File")
  }

  private fun createVirtualFile(project: Project, notebookId: String, driver: ZeppelinDriver): ZeppelinRemoteFile? {
    //TODO why CONNECTING status is ok?
    if (driver.isAvailableBlocking().isFailed()) return null
    return Util.prepareVirtualFile(project,
                              name = (driver.getNotePathById(notebookId) ?: return null).name,
                              notebookId, driver.connectionData.innerId)
  }

  override suspend fun loadContent(fileInfo: FileInfo, project: Project): SafeResult<Pair<File, TableOffsetsMap?>> = error("Not used")

  override fun showViewer(fileInfo: FileInfo,
                          project: Project,
                          contentFile: File?,
                          requestFocus: Boolean) = error("Not used")


  object Util {
    val INSTANCE = ZeppelinFileTypeViewer()

    fun prepareVirtualFile(project: Project, name: String, notebookId: String, connId: String): ZeppelinRemoteFile {
      val template = FileTemplateManager
        .getInstance(project)
        .getInternalTemplate(ZeppelinConstants.TEMPLATE_FILE_NAME)

      val virtualFile = ZeppelinRemoteFile(name, template.text)
      setupVirtualFile(virtualFile, connId, notebookId)

      return virtualFile
    }


    private fun setupVirtualFile(virtualFile: ZeppelinRemoteFile, configId: String, noteId: String) {
      val currentTime = System.currentTimeMillis()

      NotebookFileUtil.setRemoteFlag(virtualFile)
      NotebookFileUtil.setConfigId(virtualFile, configId)
      NotebookFileUtil.setNotebookId(virtualFile, noteId)
      ZeppelinFileLoadStatistic.startCreateFile(virtualFile, currentTime)

      virtualFile.andCache()
    }
  }

}
