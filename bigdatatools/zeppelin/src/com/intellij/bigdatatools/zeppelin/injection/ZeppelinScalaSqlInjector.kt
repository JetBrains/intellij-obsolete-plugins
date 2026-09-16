package com.intellij.bigdatatools.zeppelin.injection

import com.intellij.bigdatatools.zeppelin.psi.ZeppelinScalaPsiFile
import com.intellij.lang.Language
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.util.RecursionManager
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.sql.dialects.spark.SparkDialect
import com.intellij.util.Processor
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiUtil
import org.jetbrains.plugins.scala.lang.psi.api.base.literals.ScStringLiteral
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.ScBindingPattern
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScAssignment
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScValueOrVariableDefinition
import org.jetbrains.plugins.scala.lang.psi.types.nonvalue.Parameter
import scala.Option

class ZeppelinScalaSqlInjector : MultiHostInjector {
  override fun elementsToInjectIn(): List<Class<out PsiElement>> {
    return listOf(ScStringLiteral::class.java)
  }
  override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
    if (context !is ScStringLiteral || !context.isValid || context.textRange.isEmpty) return
    val file: PsiFile = context.containingFile
    if (file !is ZeppelinScalaPsiFile) return
    val sql = SparkDialect.INSTANCE

    if (shouldInject(context, file)) {
      inject(registrar, context, sql)
    }
  }

  private fun shouldInject(context: ScStringLiteral, file: PsiFile): Boolean {
    if (isSqlParameter(context)) {
      return true
    }
    for (definition in getVariableDefinition(context)) {
      if (isSqlParamName(definition.name ?: "")) {
        return true
      }
      if (shouldInjectByVariable(file, definition)) {
        return true
      }
    }
    return false
  }

  private fun shouldInjectByVariable(file: PsiFile, definition: PsiElement): Boolean {
    if (definition.containingFile != file) { return false }
    val searchScope = LocalSearchScope(arrayOf(file), "", true)
    var shouldInject = false
    RecursionManager.doPreventingRecursion(this, false) {
      ReferencesSearch.search(definition, searchScope, true).forEach(Processor {
        val reference = it.element
        val shouldInjectRef = reference is ScExpression && isSqlParameter(reference)
        shouldInject = shouldInject || shouldInjectRef
        !shouldInject
      })
      Unit
    }
    return shouldInject
  }

  private fun getVariableDefinition(stringLiteral: PsiElement): List<ScBindingPattern> {
    when (val parent = stringLiteral.parent) {
      is ScValueOrVariableDefinition -> {
        if (stringLiteral == parent.expr().getOrNull()) {
          val variables = parent.bindings().toList()
          return if (variables.size() == 1) listOfNotNull(variables.find { true }.getOrNull()) else emptyList()
        }
      }
      is ScAssignment -> {
        val leftExpression = parent.leftExpression()
        val result = (leftExpression as? PsiReference)?.resolve() as? ScBindingPattern
        if (result != null) return listOf(result)
        return (leftExpression as? PsiPolyVariantReference)?.multiResolve(false)?.mapNotNull {
          it.element as? ScBindingPattern
        } ?: emptyList()
      }
    }
    return emptyList()
  }

  private fun <T> Option<T>.getOrNull(): T? = this.getOrElse<T> { null }

  private fun isSqlParamName(string: String): Boolean {
    return string in listOf("sqlText", "sql")
  }

  private fun isSqlParameter(target: ScExpression): Boolean {
    val parameter: Parameter? = ScalaPsiUtil.parameterOf(target).getOrNull()
    return parameter != null && isSqlParamName(parameter.name())
  }

  private fun inject(registrar: MultiHostRegistrar, host: ScStringLiteral, sql: Language) {
    registrar.startInjecting(sql).addPlace(null, null, host, ElementManipulators.getValueTextRange(host)).doneInjecting()
  }
}