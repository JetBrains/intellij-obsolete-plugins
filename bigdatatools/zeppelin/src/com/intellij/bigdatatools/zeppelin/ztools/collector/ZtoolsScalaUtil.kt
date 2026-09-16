package com.intellij.bigdatatools.zeppelin.ztools.collector

import com.intellij.bigdatatools.notebooks.core.impl.editor.getNotebookPsiFileFile
import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.bigdatatools.notebooks.core.impl.psi.getPsiFileForLanguage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.scala.ScalaLanguage
import org.jetbrains.plugins.scala.lang.psi.api.ScalaElementVisitor
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile

object ZtoolsScalaUtil {
  fun collectFromScalaRoots(project: Project, file: NotebookVirtualFile, visitor: ScalaElementVisitor) {
    ApplicationManager.getApplication().runReadAction {
      val scalaFile = file.getNotebookPsiFileFile(project).getPsiFileForLanguage(ScalaLanguage.INSTANCE) as? ScalaFile
      scalaFile?.acceptScala(visitor)
    }
  }
}