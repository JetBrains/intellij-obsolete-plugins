package com.intellij.bigdatatools.zeppelin.ztools.collector

import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.bigdatatools.zeppelin.ztools.collector.ZtoolsScalaUtil.collectFromScalaRoots
import com.intellij.lang.Language
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.util.parents
import org.jetbrains.plugins.scala.ScalaLanguage
import org.jetbrains.plugins.scala.lang.psi.api.ScalaRecursiveElementVisitor
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScMethodCall
import org.jetbrains.plugins.scala.lang.psi.impl.expr.ScReferenceExpressionImpl

class ZtoolsScalaTablesCollector : ZtoolsWordsCollector {
  override fun collectTableNames(project: Project, file: NotebookVirtualFile) = runReadAction {
    if (DumbService.isDumb(project)) emptySet()
    else {
      val wordsVisitor = ScalaLiteralWordsVisitor()
      collectFromScalaRoots(project, file, wordsVisitor)
      wordsVisitor.tables
    }
  }

  override fun supportsLanguage(language: Language): Boolean = language == ScalaLanguage.INSTANCE

  private class ScalaLiteralWordsVisitor : ScalaRecursiveElementVisitor() {
    val tables = mutableSetOf<ZtoolsRefSqlTableInfo>()

    override fun visitMethodCallExpression(call: ScMethodCall?) {
      val scReference = (call?.args()?.callExpression() as? ScReferenceExpressionImpl)?.element
      val methodName = scReference?.canonicalText ?: return
      if (!ZtoolsCollectorHelpers.isCreateTableMethod(methodName))
        return

      val dataFrameName = (scReference as ScReferenceExpressionImpl).parents(false).firstOrNull()
                            ?.children?.lastOrNull()?.children?.firstOrNull()?.text ?: return

      if (dataFrameName.startsWith("s"))
        return
      val clearDfName = dataFrameName.removePrefix("\"").removeSuffix("\"")
      tables.add(ZtoolsCollectorHelpers.getTableInfo(methodName, clearDfName))
    }
  }
}