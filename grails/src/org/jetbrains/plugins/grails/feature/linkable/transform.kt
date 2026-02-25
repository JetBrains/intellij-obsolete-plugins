/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.feature.linkable

import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiTypes
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.GrModifierFlags
import org.jetbrains.plugins.groovy.transformations.AstTransformationSupport
import org.jetbrains.plugins.groovy.transformations.TransformationContext

class LinkableTransformationSupport : AstTransformationSupport {

  override fun applyTransformation(context: TransformationContext) {
    addLinkMethods(context, "grails.rest.Linkable")
    addLinkMethods(context, "grails.rest.Resource")
  }
}

private fun addLinkMethods(context: TransformationContext, fqn: String) {
  context.getAnnotation(fqn)?.let {
    // public void link(Map)
    context.addMethod(context.memberBuilder.method("link") {
      addModifier(GrModifierFlags.PUBLIC_MASK)
      returnType = PsiTypes.voidType()
      addParameter("link", CommonClassNames.JAVA_UTIL_MAP)
      navigationElement = it
      putUserData(LINK_METHOD_KEY, LINK_METHOD_MARKER)
    })

    // public void link(Link)
    context.addMethod(context.memberBuilder.method("link") {
      addModifier(GrModifierFlags.PUBLIC_MASK)
      returnType = PsiTypes.voidType()
      addParameter("link", LINK_FQN)
      navigationElement = it
    })
  }
}