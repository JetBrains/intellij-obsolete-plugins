package com.intellij.bigdatatools.zeppelin.notebook.interpreter

import com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.InterpreterSupport
import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile

object ZeppelinInterpreters {
  private val supportedInterpreters = InterpreterSupport.getInterpreters()
  val languages = supportedInterpreters.map { it.language }
  val languageToTemplates = supportedInterpreters.map { it.language.id to it.template }.toMap()

  fun getSourcesToHighlight(project: Project?, virtualFile: VirtualFile?) =
    ZeppelinSupportLanguages.languageTypes.mapNotNull { interpreterType ->
      val source = interpreterType.source
      val interpreterSupport = supportedInterpreters.firstOrNull { interpreterSupport ->
        interpreterSupport.id == interpreterType.id
      } ?: return@mapNotNull null

      val syntaxHighlighter = interpreterSupport.syntaxHighlighter(project, virtualFile) ?: return@mapNotNull null
      source to syntaxHighlighter
    }.distinctBy { it.first }

  fun ignoreInspections(file: PsiFile, language: Language) = supportedInterpreters.forEach {
    if (it.language == language) it.ignoreInspection(file)
  }
}