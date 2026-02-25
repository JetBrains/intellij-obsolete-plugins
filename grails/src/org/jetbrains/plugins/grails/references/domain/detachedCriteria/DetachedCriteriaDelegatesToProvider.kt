/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.references.domain.detachedCriteria

import com.intellij.util.ProcessingContext
import groovy.lang.Closure
import org.jetbrains.plugins.groovy.lang.psi.api.GrFunctionalExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression
import org.jetbrains.plugins.groovy.lang.psi.patterns.closureCallKey
import org.jetbrains.plugins.groovy.lang.psi.patterns.groovyClosure
import org.jetbrains.plugins.groovy.lang.psi.patterns.psiMethod
import org.jetbrains.plugins.groovy.lang.resolve.delegatesTo.DelegatesToInfo
import org.jetbrains.plugins.groovy.lang.resolve.delegatesTo.GrDelegatesToProvider

class DetachedCriteriaDelegatesToProvider : GrDelegatesToProvider {

  private companion object {
    val closurePattern = groovyClosure().inMethod(
        psiMethod(
            "grails.gorm.DetachedCriteria",
            "and", "or", "not", // DetachedCriteria#handleJunction()
            "get", "list", "count", "exists", "find", // DetachedCriteria#withPopulatedQuery()
            "eqAll", "gtAll", "ltAll", "geAll", "leAll", // DetachedCriteria#buildQueryableCriteria()
            "build", "where" // DetachedCriteria#build()
        )
    )
  }

  override fun getDelegatesToInfo(expression: GrFunctionalExpression): DelegatesToInfo? {
    val context = ProcessingContext()
    if (!closurePattern.accepts(expression, context)) return null
    val call = context.get(closureCallKey) as? GrMethodCall
    val referenceExpression = call?.invokedExpression as? GrReferenceExpression
    return referenceExpression?.qualifierExpression?.type?.let {
      DelegatesToInfo(it, Closure.DELEGATE_FIRST)
    }
  }
}