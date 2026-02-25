/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.artefact.impl.controllers

import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.ResolveState
import com.intellij.psi.SyntheticElement
import com.intellij.psi.scope.ElementClassHint
import com.intellij.psi.scope.ElementClassHint.DeclarationKind.FIELD
import com.intellij.psi.scope.ElementClassHint.DeclarationKind.METHOD
import com.intellij.psi.scope.PsiScopeProcessor
import org.jetbrains.plugins.grails.structure.GrailsApplication
import org.jetbrains.plugins.grails.util.GrailsUtils
import org.jetbrains.plugins.grails.util.version.Version.GRAILS_2_0
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition

fun getActions(controller: GrTypeDefinition, grailsApplication: GrailsApplication): Map<String, PsiMember> {
  val processor = ActionProcessor(grailsApplication.grailsVersion >= GRAILS_2_0)
  controller.processDeclarations(processor, ResolveState.initial(), null, controller)
  return processor.actions
}

class ActionProcessor(private val atLeast14: Boolean) : PsiScopeProcessor, ElementClassHint {

  val actions: MutableMap<String, PsiMember> = mutableMapOf()

  override fun shouldProcess(kind: ElementClassHint.DeclarationKind): Boolean {
    return kind == FIELD || kind == METHOD
  }

  override fun execute(element: PsiElement, state: ResolveState): Boolean {
    if (element is SyntheticElement) return true
    val name = GrailsUtils.getActionName0(element, false, atLeast14)
    if (name != null && !actions.containsKey(name)) {
      actions.put(name, element as PsiMember)
    }
    return true
  }

  @Suppress("UNCHECKED_CAST")
  override fun <T : Any?> getHint(hintKey: Key<T>): T? {
    if (hintKey == ElementClassHint.KEY) return this as T
    return null
  }
}