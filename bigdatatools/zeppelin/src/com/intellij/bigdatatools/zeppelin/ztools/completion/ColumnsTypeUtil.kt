package com.intellij.bigdatatools.zeppelin.ztools.completion

import com.intellij.bigdatatools.zeppelin.constants.ZeppelinIcons
import com.intellij.bigdatatools.zeppelin.utils.ScalaIntegrationUtil
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.AutoCompletionPolicy
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.ProcessingContext
import org.jetbrains.plugins.scala.lang.psi.api.base.literals.ScSymbolLiteral
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScArgumentExprList
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScParameter
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScParameterClause
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScTypeParam
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScTypeParamClause
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiManager
import org.jetbrains.plugins.scala.lang.psi.impl.expr.ScMethodCallImpl
import org.jetbrains.plugins.scala.lang.psi.impl.expr.ScReferenceExpressionImpl
import org.jetbrains.plugins.scala.lang.psi.types.ConstraintSystem
import org.jetbrains.plugins.scala.lang.psi.types.ConstraintsResult
import org.jetbrains.plugins.scala.lang.psi.types.Context
import org.jetbrains.plugins.scala.lang.psi.types.ScType
import org.jetbrains.plugins.scala.lang.psi.types.ScalaType
import org.jetbrains.plugins.scala.lang.psi.types.result.Typeable


interface PsiElementMatcher {
  fun accept(e: PsiElement?): Boolean
  fun isFixed(): Boolean
}

class FixedTypePsiElementMatcher(private val tpe: Class<out PsiElement>) : PsiElementMatcher {
  override fun accept(e: PsiElement?): Boolean = e != null && tpe.isAssignableFrom(e::class.java)
  override fun isFixed(): Boolean = false
}

class OrPsiElementMatcher(private val fxd: Boolean, vararg matchers: PsiElementMatcher?) : PsiElementMatcher {
  private val actualMatchers = matchers.filterNotNull()
  override fun accept(e: PsiElement?): Boolean = actualMatchers.any { it.accept(e) }
  override fun isFixed(): Boolean = fxd
}

class AndPsiElementMatcher(private val fxd: Boolean, vararg matchers: PsiElementMatcher?) : PsiElementMatcher {
  private val actualMatchers = matchers.filterNotNull()
  override fun accept(e: PsiElement?): Boolean = actualMatchers.all { it.accept(e) }
  override fun isFixed(): Boolean = fxd
}

class OrPsiMatcher(
  private val leftMatcher: PsiElementMatcher,
  private val rightMatcher: PsiElementMatcher,
  private val fxd: Boolean
) : PsiElementMatcher {
  override fun accept(e: PsiElement?): Boolean = leftMatcher.accept(e) || rightMatcher.accept(e)
  override fun isFixed(): Boolean = fxd
}

class SingleElementMatcher : PsiElementMatcher {
  override fun accept(e: PsiElement?): Boolean = e != null
  override fun isFixed(): Boolean = true
}

class SymbolElementMatcher : PsiElementMatcher {
  override fun accept(e: PsiElement?): Boolean = e is ScSymbolLiteral

  override fun isFixed(): Boolean = true
}

class TypedScElementMatcher(private val tpeBound: ScType) : PsiElementMatcher {
  companion object {
    fun fromString(fqn: String, project: Project): TypedScElementMatcher? {
      val classOpt = ScalaPsiManager.instance(project).getCachedClass(GlobalSearchScope.allScope(project), fqn)
      return if (classOpt.isDefined) TypedScElementMatcher(ScalaType.designator(classOpt.get())) else null
    }
  }

  override fun accept(e: PsiElement?): Boolean {
    if (e == null) return false
    if (isTransient(e)) return true

    val typeResult = (e as? Typeable)?.type()?.toOption()
    if (typeResult == null || typeResult.isEmpty) return false


    return tpeBound.typeSystem().conformsInner(
      tpeBound, typeResult.get(), ScalaIntegrationUtil.createEmptyScalaSet(), ConstraintSystem.empty(), true, Context.apply(e)
    ) != ConstraintsResult.`Left$`.`MODULE$`
  }

  override fun isFixed(): Boolean = false

  private fun isTransient(e: PsiElement) = when (e) {
    is ScArgumentExprList -> e.parent is Typeable
    else -> false
  }
}

class ScMethodCallMatcher(private val acceptedNames: Map<String, Set<String>>) : PsiElementMatcher {
  override fun accept(e: PsiElement?): Boolean = when (e) { //todo
    is ScArgumentExprList -> true
    is ScParameterClause -> true
    is ScParameter -> true
    is ScTypeParam -> true
    is ScTypeParamClause -> true
    is ScMethodCallImpl -> (e.effectiveInvokedExpr as? ScReferenceExpressionImpl)?.resolve()?.let {
      (it as? PsiMethod)?.let { resolvedMethod ->
        resolvedMethod.containingClass?.let { clazz ->
          acceptedNames[clazz.qualifiedName]?.contains(resolvedMethod.name)
        }
      }
    } ?: false
    else -> false
  }

  override fun isFixed(): Boolean = false
}

sealed class ExprMatchResult {
  abstract fun intersect(other: ExprMatchResult): ExprMatchResult
  abstract fun union(other: ExprMatchResult): ExprMatchResult
}
sealed class ExprMatchType {
  abstract fun intersect(other: ExprMatchType): ExprMatchType
  abstract fun union(other: ExprMatchType): ExprMatchType
}

object WeakMatch : ExprMatchType() {
  override fun intersect(other: ExprMatchType): ExprMatchType = this
  override fun union(other: ExprMatchType): ExprMatchType = other
}
object StrongMatch : ExprMatchType() {
  override fun intersect(other: ExprMatchType): ExprMatchType = other
  override fun union(other: ExprMatchType): ExprMatchType = this
}
object NoMatchFound : ExprMatchResult() {
  override fun intersect(other: ExprMatchResult): ExprMatchResult = NoMatchFound
  override fun union(other: ExprMatchResult): ExprMatchResult = other
}
data class ExprMatchFound(val element: PsiElement, val matchType: ExprMatchType) : ExprMatchResult() {
  override fun intersect(other: ExprMatchResult): ExprMatchResult = when (other) {
    is NoMatchFound -> NoMatchFound
    is ExprMatchFound -> ExprMatchFound(other.element, matchType.intersect(other.matchType))
  }

  override fun union(other: ExprMatchResult): ExprMatchResult = when (other) {
    is NoMatchFound -> this
    is ExprMatchFound -> ExprMatchFound(other.element, matchType.union(other.matchType))
  }
}

interface PsiElementMatcherChain {
  fun accept(e: PsiElement): ExprMatchResult
}

abstract class PsiElementMatcherChainBase : PsiElementMatcherChain {
  override fun accept(e: PsiElement): ExprMatchResult = (1 until getMatchersCount()).fold(runMatcher(0, e)) { state, idx ->
    when (state) {
      is NoMatchFound -> NoMatchFound
      is ExprMatchFound -> state.intersect(runMatcher(idx, state.element.parent))
    }
  }

  private fun runMatcher(idx: Int, e: PsiElement): ExprMatchResult {
    val matcher = getMatcher(idx) ?: return NoMatchFound

    if (!matcher.accept(e)) return NoMatchFound
    if (matcher.isFixed()) return ExprMatchFound(e, getMatchScore(e, idx, 0))

    var current: PsiElement? = e
    var depth = 0

    while (current != null) if (!matcher.accept(current.parent)) break else {
      current = current.parent
      depth += 1
    }

    return if (current == null) NoMatchFound else ExprMatchFound(current, getMatchScore(current, idx, depth))
  }

  protected abstract fun getMatchersCount(): Int

  protected abstract fun getMatcher(idx: Int): PsiElementMatcher?

  protected abstract fun getMatchScore(e: PsiElement?, idx: Int, depth: Int): ExprMatchType
}

class SimplePsiElementMatcherChain(private val matchers: Array<PsiElementMatcher>,
                                   private val matcherThreshold: Int) : PsiElementMatcherChainBase() {
  override fun getMatchersCount(): Int = matchers.size

  override fun getMatcher(idx: Int): PsiElementMatcher = matchers[idx]

  override fun getMatchScore(e: PsiElement?, idx: Int, depth: Int): ExprMatchType = if (depth < matcherThreshold) StrongMatch else WeakMatch
}

abstract class ZeppelinSchemaCompletionProviderBase : CompletionProvider<CompletionParameters>() {
  companion object {
    fun processCompletionPrefix(prefix: String): String = (if (prefix.startsWith('"')) prefix.substring(1) else prefix).let {
      when (val idx = it.lastIndexOf(' ')) {
        -1 -> it
        else -> it.substring(idx + 1)
      }
    }
  }

  protected abstract fun getMatcher(project: Project): PsiElementMatcherChain

  protected abstract fun findZeppelinSchema(fullText: String, file: PsiFile): Collection<ColumnInfo>?

  protected abstract fun extractEffectiveExpr(exprMatchFound: ExprMatchFound): PsiElement?
// PyStringLiteralExpression: PyArgumentList : PyCallExpression: df.withColumn
  override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
    val matcherChain = getMatcher(parameters.position.project)
    val matchingSet = result.withPrefixMatcher(processCompletionPrefix(result.prefixMatcher.prefix))

    (matcherChain.accept(parameters.position.parent) as? ExprMatchFound)?.let { mf ->
      val viewProvider = parameters.position.containingFile.viewProvider
      // we get all psi elements that don't belong to Dataset method call itself, i.e. all before dot: <extracted psi>.filter()
      extractEffectiveExpr(mf)?.let { foundElement ->
        findZeppelinSchema(foundElement.text, viewProvider.getPsi(viewProvider.baseLanguage)!!)?.let { schema ->
          schema.forEach { columnInfo ->
            val lookupElement = LookupElementBuilder.create(columnInfo.name).bold().withPresentableText(columnInfo.name).withTypeText(
              columnInfo.tpe.presentableName).withIcon(ZeppelinIcons.ZEPPELIN).withAutoCompletionPolicy(AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE)
            matchingSet.addElement(lookupElement)
          }
        }
      }
    }
  }
}
