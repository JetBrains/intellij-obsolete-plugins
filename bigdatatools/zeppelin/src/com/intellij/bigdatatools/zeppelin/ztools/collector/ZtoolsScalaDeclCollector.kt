package com.intellij.bigdatatools.zeppelin.ztools.collector

import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.bigdatatools.zeppelin.ztools.collector.ZtoolsScalaUtil.collectFromScalaRoots
import com.intellij.lang.Language
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.scala.ScalaLanguage
import org.jetbrains.plugins.scala.lang.psi.api.ScalaRecursiveElementVisitor
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.ScReferencePattern
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScReferenceExpression
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScValue
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScVariable

class ZtoolsScalaDeclCollector : ZtoolsDeclCollector {
  override fun collectDeclNames(project: Project, file: NotebookVirtualFile): Set<String>? {
    return runReadAction {
      if (DumbService.isDumb(project)) null
      else {
        val declsVisitor = ScalaDeclNamesVisitor()
        collectFromScalaRoots(project, file, declsVisitor)
        declsVisitor.names
      }
    }
  }

  override fun supportsLanguage(language: Language): Boolean = language == ScalaLanguage.INSTANCE

  private class ScalaDeclNamesVisitor : ScalaRecursiveElementVisitor() {
    val names = mutableSetOf<String>()

    override fun visitValue(v: ScValue?) {
      v?.declaredNames()?.foreach { names.add(it) }
      super.visitValue(v)
    }

    override fun visitVariable(varr: ScVariable?) {
      varr?.declaredNames()?.foreach { names.add(it) }
      super.visitVariable(varr)
    }

    override fun visitReferenceExpression(ref: ScReferenceExpression?) {
      val tryResolve = ref?.bind()
      if (tryResolve != null && tryResolve.isDefined) {
        val namedElement = tryResolve.get().element
        if (namedElement is ScReferencePattern) {
          val name = tryResolve.get().name()
          names.add(name)
        }
      }
      super.visitReferenceExpression(ref)
    }
  }
}