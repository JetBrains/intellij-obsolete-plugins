/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.gson

import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.PsiReference
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import org.jetbrains.plugins.groovy.findUsages.GroovyReadWriteAccessDetector
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentLabel
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression
import org.jetbrains.plugins.groovy.lang.psi.controlFlow.ControlFlowBuilderUtil
import org.jetbrains.plugins.groovy.lang.psi.patterns.GroovyElementPattern
import org.jetbrains.plugins.groovy.lang.psi.patterns.GroovyPatterns

class GsonControllerReference(element: GrArgumentLabel) : PsiPolyVariantReferenceBase<GrArgumentLabel>(element, true) {

  override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> {
    val resolved = resolve() ?: return emptyArray()
    return arrayOf(PsiElementResolveResult(resolved))
  }

  override fun resolve(): PsiElement? = getModelFields(element).find { it.name == element.name }
}

class GsonRWAccessDetector : GroovyReadWriteAccessDetector() {

  override fun isReadWriteAccessible(element: PsiElement): Boolean = isModelVariable(element)

  override fun isDeclarationWriteAccess(element: PsiElement): Boolean {
    return if (isModelVariable(element)) false else super.isDeclarationWriteAccess(element)
  }

  override fun getReferenceAccess(referencedElement: PsiElement, reference: PsiReference): Access {
    return if (reference is GsonControllerReference) Access.Write else super.getReferenceAccess(referencedElement, reference)
  }

  override fun getExpressionAccess(expression: PsiElement): Access {
    return if (CONTROLLER_REFERENCE_PLACE.accepts(expression)) Access.Write else super.getExpressionAccess(expression)
  }
}

private val RETURN_STATEMENT = object : PatternCondition<PsiElement>("is in return statement") {

  override fun accepts(element: PsiElement, context: ProcessingContext?): Boolean {
    val statement = PsiTreeUtil.getParentOfType(element, GrListOrMap::class.java) ?: return false
    return ControlFlowBuilderUtil.isCertainlyReturnStatement(statement)
  }
}

/*
 * def index() {
 *   return [<place>]
 * }
 *
 * def index() {
 *   [<place>]
 * }
 */
val CONTROLLER_REFERENCE_PLACE: GroovyElementPattern.Capture<GrArgumentLabel> = GroovyPatterns.namedArgumentLabel(null).with(RETURN_STATEMENT)

private val COMPLETION_PLACE = PsiJavaPatterns.psiElement().withParent(
    PlatformPatterns.psiElement(GrReferenceExpression::class.java).withParent(
        PlatformPatterns.psiElement(GrListOrMap::class.java)
    )
).with(RETURN_STATEMENT)
