/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.gson

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.plugins.grails.GsonConstants
import org.jetbrains.plugins.grails.util.GrailsUtils
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrVariable
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrMethodCallExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMember
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrScriptField
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GroovyScriptClass

fun isGsonFile(file: PsiFile): Boolean = file.name.endsWith(GsonConstants.FILE_SUFFIX)

internal fun getModelFields(element: PsiElement?): List<GrScriptField> {
  return getModelFields(getScriptClass(element))
}

fun getScriptClass(element: PsiElement?): GroovyScriptClass? {
  val action = PsiTreeUtil.getParentOfType(element, GrField::class.java, GrMethod::class.java) as? GrMember ?: return null
  val gsonView = GrailsUtils.getViewPsiByAction(action).find { isGsonFile(it) }
  return (gsonView as? GroovyFile)?.scriptClass as? GroovyScriptClass
}

fun getModelFields(scriptClass: GroovyScriptClass?): List<GrScriptField> {
  if (scriptClass == null) return emptyList()
  return scriptClass.fields.filterIsInstance(GrScriptField::class.java)
}

fun isModelVariable(variable: PsiElement): Boolean {
  return variable is GrVariable && isModelVariable(variable)
}

fun isModelVariable(variable: GrVariable): Boolean {
  val closure = variable.parent?.parent as? GrClosableBlock ?: return false
  val file = variable.containingFile as? GroovyFile ?: return false
  return closure == findModelClosure(file)
}

fun findModelClosure(file: GroovyFile): GrClosableBlock? {
  for (statement in file.topStatements) {
    if (statement !is GrMethodCallExpression) continue
    if ("model" != (statement.invokedExpression as? GrReferenceExpression)?.referenceName) continue
    val arguments = statement.closureArguments
    return if (arguments.size == 1) arguments[0] else null
  }
  return null
}

fun isGsonTemplate(element: PsiElement?): Boolean = element is GroovyFile && element.isScript && element.virtualFile?.let {
  it.nameSequence.startsWith("_") && it.nameSequence.endsWith(GsonConstants.FILE_SUFFIX)
} ?: false

fun getGsonTemplateName(file: GroovyFile): String? = if (isGsonTemplate(file)) getGsonTemplateName(file.name) else null

fun getGsonTemplateName(fileName: String): String = fileName.removePrefix("_").removeSuffix(
  GsonConstants.FILE_SUFFIX)