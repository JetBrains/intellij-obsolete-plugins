/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.gson

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiType
import org.jetbrains.plugins.groovy.extensions.impl.NamedArgumentDescriptorBase
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElement
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentLabel
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil

class GsonModelFieldArgumentDescriptor(val field: GrField) : NamedArgumentDescriptorBase() {

  override fun createReference(label: GrArgumentLabel): GsonControllerReference = GsonControllerReference(label)

  override fun checkType(type: PsiType, context: GroovyPsiElement): Boolean = TypesUtil.isAssignable(field.type, type, context)

  override fun customizeLookupElement(lookupElement: LookupElementBuilder): LookupElementBuilder =
      lookupElement.appendTailText(" Gson Model Field", true).withTypeText(field.type.presentableText)
}