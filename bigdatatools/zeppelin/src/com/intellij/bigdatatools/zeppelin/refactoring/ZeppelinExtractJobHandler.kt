package com.intellij.bigdatatools.zeppelin.refactoring

import com.intellij.bigdatatools.zeppelin.refactoring.ZeppelinExtractRefactoringUtil.toPsiDirectory
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.ide.progress.ModalTaskOwner
import com.intellij.platform.ide.progress.TaskCancellation
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.psi.JavaDirectoryService
import com.intellij.psi.PsiFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal open class ZeppelinExtractJobHandler(private val parentAction: ZeppelinExtractJobAction) : ZeppelinExtractHandlerBase() {

  /** Title for the modal progress window. */
  open fun getTitle(): @NlsContexts.ProgressTitle String = ZepMessagesBundle.message("action.extract.text")

  override fun invoke(project: Project, editor: Editor?, file: PsiFile?, dataContext: DataContext?) {
    if (file == null) return

    runWithModalProgressBlocking(ModalTaskOwner.project(project), getTitle(), TaskCancellation.nonCancellable()) {
      val jobInfo = ApplicationManager.getApplication().runReadAction(Computable {
        extractInfoInner(editor, file)
      }) ?: return@runWithModalProgressBlocking

      withContext(Dispatchers.EDT) {
        val virtualFile = FileChooserFactory.getInstance().createSaveFileDialog(
          FileSaverDescriptor(ZepMessagesBundle.message("zeppelin.extract.job.file.saver.title"),
                              ZepMessagesBundle.message("zeppelin.extract.job.file.saver.title"),
                              "scala"), project).save(
          parentAction.initialBaseDir ?: project.projectFile?.parent?.parent,
          FileUtil.sanitizeFileName(
            "Job" + file.name.removeSuffix(".scala").removeSuffix(".zpln"), true, ""
          ) + ".scala"
        )?.getVirtualFile(true)

        if (virtualFile != null) {
          parentAction.initialBaseDir = virtualFile.parent
          createAndOpenNewFile(project, virtualFile, jobInfo, dataContext)
        }
      }
    }
  }

  private fun createAndOpenNewFile(project: Project,
                                   virtualFile: VirtualFile,
                                   initialContent: ZeppelinExtractRefactoringUtil.ExtractedJobInfo,
                                   dataContext: DataContext?) {
    val psiDir = virtualFile.parent.toPsiDirectory(project)
    val packName = psiDir?.let { JavaDirectoryService.getInstance().getPackageInSources(it)?.qualifiedName } ?: ""

    val fileText = createFileText(virtualFile.nameWithoutExtension,
                                  initialContent,
                                  packName,
                                  initialContent.defaultVarsUsed.map { it.name }.toSet())

    ApplicationManager.getApplication().runWriteAction {
      try {
        VfsUtil.saveText(virtualFile, fileText)
        ZeppelinExtractRefactoringUtil.postProcessFile(virtualFile, null, project, dataContext, false)
      }
      catch (_: Exception) {
      }
    }
  }

  private fun createFileText(fileName: String,
                             fileText: ZeppelinExtractRefactoringUtil.ExtractedJobInfo,
                             packName: String,
                             outerNamesInSignature: Set<String>): String {
    return """${if (packName.isEmpty()) "" else "package $packName"} 
        | 
        |${fileText.importsBlock}
        | 
        |object $fileName {
        |  ${ZeppelinExtractRefactoringUtil.createMethodText(fileText, outerNamesInSignature, "run", "")}
        |}""".trimMargin()
  }
}