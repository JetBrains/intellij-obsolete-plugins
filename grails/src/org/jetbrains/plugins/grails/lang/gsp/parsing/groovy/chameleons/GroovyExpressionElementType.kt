/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.lang.gsp.parsing.groovy.chameleons

import com.intellij.lang.ASTNode
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.impl.GrGspExprInjectionImpl

class GroovyExpressionElementType(debugName: String) : GspGroovyLazyElementType(debugName) {

  override fun createNode(text: CharSequence?): ASTNode = GrGspExprInjectionImpl(this, text)
}
