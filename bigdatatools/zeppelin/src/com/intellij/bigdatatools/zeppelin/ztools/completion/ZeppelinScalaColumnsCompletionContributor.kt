package com.intellij.bigdatatools.zeppelin.ztools.completion

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScMethodCall
import org.jetbrains.plugins.scala.lang.psi.impl.base.ScInterpolatedStringLiteralImpl
import org.jetbrains.plugins.scala.lang.psi.impl.base.ScStringLiteralImpl
import org.jetbrains.plugins.scala.lang.psi.impl.base.literals.ScSymbolLiteralImpl

class ZeppelinScalaColumnsCompletionContributor : ZeppelinColumnCompletionContributor() {
  companion object {
    val PATTERN_NAMES = mapOf(Pair("org.apache.spark.sql.Dataset", setOf (
      "filter", "where", "groupBy", "rollUp", "agg", "withColumn", "drop", "repartition", "join", "joinWith", "sortWithinPartitions",
      "sort", "orderBy", "apply", "col", "select"
    )))
  }

  init {
    extendWithClass(ScInterpolatedStringLiteralImpl::class.java, StubTypeCompletionProvider)
    extendWithClass(ScStringLiteralImpl::class.java, StubTypeCompletionProvider)
    extendWithClass(ScSymbolLiteralImpl::class.java, StubTypeCompletionProvider)
  }

  private object StubTypeCompletionProvider : ZeppelinSchemaCompletionProviderBase() {
    override fun getMatcher(project: Project): PsiElementMatcherChain = SimplePsiElementMatcherChain(arrayOf (
      OrPsiElementMatcher(
        false,
        TypedScElementMatcher.fromString("java.lang.String", project),
        TypedScElementMatcher.fromString("org.apache.spark.sql.Column", project),
        SymbolElementMatcher()
      ),
      ScMethodCallMatcher(PATTERN_NAMES)
    ), 3)

    override fun findZeppelinSchema(fullText: String, file: PsiFile): Collection<ColumnInfo> = schemaCommon(fullText, file)

    override fun extractEffectiveExpr(exprMatchFound: ExprMatchFound): PsiElement? =
      ((exprMatchFound.element as? ScMethodCall)?.effectiveInvokedExpr as? PsiElement)?.firstChild
  }
}

