/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.lang.gsp.parsing.groovy.chameleons

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilderFactory
import com.intellij.psi.ParsingDiagnostics
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.ILazyParseableElementType
import org.jetbrains.plugins.grails.lang.gsp.parsing.groovy.GspAwareGroovyParser
import org.jetbrains.plugins.groovy.GroovyLanguage

abstract class GspGroovyLazyElementType(debugName: String) : ILazyParseableElementType(debugName, GroovyLanguage) {

  override fun doParseContents(chameleon: ASTNode, psi: PsiElement): ASTNode {
    val builder = PsiBuilderFactory.getInstance().createBuilder(psi.project, chameleon)
    val startTime = System.nanoTime()
    val result = GspAwareGroovyParser().parse(this, builder).firstChildNode
    ParsingDiagnostics.registerParse(builder, language, System.nanoTime() - startTime)
    return result
  }
}
