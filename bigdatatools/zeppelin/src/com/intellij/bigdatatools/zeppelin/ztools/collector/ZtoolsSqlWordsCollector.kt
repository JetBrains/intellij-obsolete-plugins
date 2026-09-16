package com.intellij.bigdatatools.zeppelin.ztools.collector

import com.intellij.bigdatatools.notebooks.core.impl.editor.getNotebookPsiFileFile
import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.bigdatatools.notebooks.core.impl.psi.getPsiFileForLanguage
import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.util.elementType
import com.intellij.sql.dialects.hive.HiveDialect
import com.intellij.sql.dialects.spark.SparkDialect
import com.intellij.sql.psi.SqlCompositeElementTypes
import com.intellij.sql.psi.SqlElement
import com.intellij.sql.psi.SqlFile
import com.intellij.sql.psi.SqlReferenceExpression
import com.intellij.sql.psi.SqlVisitor

class ZtoolsSqlWordsCollector : ZtoolsWordsCollector {
  override fun collectTableNames(project: Project, file: NotebookVirtualFile) = if (DumbService.isDumb(project))
    emptySet()
  else {
    val wordsVisitor = SqlITableNameVisitor()
    collectFromSqlRoots(project, file, wordsVisitor)
    wordsVisitor.identifiers.toSet()
  }

  private fun collectFromSqlRoots(project: Project, file: NotebookVirtualFile, visitor: SqlVisitor) {
    ApplicationManager.getApplication().runReadAction {
      val sqlFile = file.getNotebookPsiFileFile(project).getPsiFileForLanguage(SparkDialect.INSTANCE) as? SqlFile
      sqlFile?.accept(visitor)
    }
  }

  //TODO check if this works, probably different sql dialects are different language instances
  override fun supportsLanguage(language: Language): Boolean = SparkDialect.INSTANCE == language || HiveDialect.INSTANCE == language

  private class SqlITableNameVisitor : SqlVisitor() {
    val identifiers = mutableSetOf<ZtoolsRefSqlTableInfo>()

    override fun visitSqlElement(o: SqlElement?) {
      o?.acceptChildren(this)
      super.visitSqlElement(o)
    }

    override fun visitSqlReferenceExpression(o: SqlReferenceExpression?) {
      super.visitSqlReferenceExpression(o)
      o ?: return
      val elementType = o.elementType
      if (elementType != SqlCompositeElementTypes.SQL_TABLE_REFERENCE)
        return

      val database = o.children.firstOrNull { it.elementType == SqlCompositeElementTypes.SQL_REFERENCE }?.text ?: ""
      val table = o.children.lastOrNull { it.elementType == SqlCompositeElementTypes.SQL_IDENTIFIER }?.text ?: ""

      identifiers.add(ZtoolsRefSqlTableInfo(database = database, table = table))
    }
  }
}