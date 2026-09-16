package com.intellij.bigdatatools.zeppelin.utils


import com.intellij.bigdatatools.zeppelin.file.ZeppelinFileType
import com.intellij.codeInsight.daemon.impl.JavaProjectSdkSetupValidator
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdkType
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.projectRoots.impl.UnknownSdkTracker
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.ui.configuration.SdkPopupBuilder
import com.intellij.openapi.roots.ui.configuration.SdkPopupFactory.Companion.newBuilder
import com.intellij.openapi.util.Condition
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.ui.EditorNotificationPanel
import java.util.function.Consumer
import javax.swing.event.HyperlinkEvent

class ZeppelinJavaProjectSdkSetupValidator : JavaProjectSdkSetupValidator() {
  override fun isApplicableFor(project: Project, file: VirtualFile): Boolean =
    file.fileType == ZeppelinFileType

  override fun getErrorMessage(project: Project, file: VirtualFile): String? {
    val sdk = ProjectRootManager.getInstance(project).projectSdk
    if (sdk != null)
      return null
    return ZepMessagesBundle.message("project.sdk.not.defined")
  }

  override fun getFixHandler(project: Project, file: VirtualFile): EditorNotificationPanel.ActionHandler {
    val builder = preparePopup(project) ?: return NOOP
    return builder.buildEditorNotificationPanelHandler()
  }

  private fun preparePopup(project: Project): SdkPopupBuilder? {
    if (ProjectRootManager.getInstance(project).projectSdk != null)
      return null
    return newBuilder()
      .withProject(project)
      .withSdkTypeFilter(Condition { type: SdkTypeId? -> type is JavaSdkType })
      .onSdkSelected(Consumer { newSdk: Sdk ->
        runWriteAction {
          ProjectRootManager.getInstance(project).projectSdk = newSdk
        }

        UnknownSdkTracker.getInstance(project).updateUnknownSdks()
      })
  }

  private val NOOP: EditorNotificationPanel.ActionHandler = object : EditorNotificationPanel.ActionHandler {
    override fun handlePanelActionClick(panel: EditorNotificationPanel, event: HyperlinkEvent) {}
    override fun handleQuickFixClick(editor: Editor, psiFile: PsiFile) {}
  }
}