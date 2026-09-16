package com.intellij.bigdatatools.zeppelin.vcs

import com.intellij.bigdatatools.zeppelin.constants.ZeppelinConstants
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.project.stateStore
import java.nio.file.Path

object ZeppelinPluginFolder {
  fun getOrCreateDir(project: Project): VirtualFile =
    when {
      ApplicationManager.getApplication().isWriteAccessAllowed -> createDir(project)
      ApplicationManager.getApplication().isDispatchThread -> runWriteAction {
        createDir(project)
      }
      else -> invokeAndWaitIfNeeded {
        runWriteAction {
          createDir(project)
        }
      }
    }

  private fun createDir(project: Project) =
    VfsUtil.createDirectoryIfMissing(VfsUtil.findFile(getDir(project), true), ZeppelinConstants.REMOTE_NOTEBOOKS_PATH)

  fun getDir(project: Project): Path = getBaseDir(project).resolve(ZeppelinConstants.REMOTE_NOTEBOOKS_PATH)
  private fun getBaseDir(project: Project) = project.stateStore.projectFilePath.parent
}