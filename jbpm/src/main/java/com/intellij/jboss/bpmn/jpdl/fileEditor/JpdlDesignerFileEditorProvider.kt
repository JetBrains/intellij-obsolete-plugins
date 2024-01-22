package com.intellij.jboss.bpmn.jpdl.fileEditor

import com.intellij.jboss.bpmn.jpdl.model.JpdlDomModelManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile
import com.intellij.util.xml.ui.PerspectiveFileEditor
import com.intellij.util.xml.ui.PerspectiveFileEditorProvider

private class JpdlDesignerFileEditorProvider : PerspectiveFileEditorProvider() {
  override fun accept(project: Project, file: VirtualFile): Boolean {
    val psiManager = PsiManager.getInstance(project)
    return ApplicationManager.getApplication().runReadAction(Computable {
      try {
        val psiFile = psiManager.findFile(file)
        psiFile is XmlFile && JpdlDomModelManager.getInstance(project).isJpdl((psiFile as XmlFile?)!!)
      }
      catch (e: ProcessCanceledException) {
        false
      }
    })
  }

  override fun createEditor(project: Project, file: VirtualFile): PerspectiveFileEditor = JpdlDesignerFileEditor(project, file)

  override fun getWeight() = 0.0
}
