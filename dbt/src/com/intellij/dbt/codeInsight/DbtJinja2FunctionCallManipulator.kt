package com.intellij.dbt.codeInsight

import com.intellij.jinja.psi.Jinja2StringLiteral
import com.intellij.jinja.tags.Jinja2FunctionCall
import com.intellij.jinja.template.Jinja2TemplateElementGenerator
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.psi.AbstractElementManipulator
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException


class DbtJinja2FunctionCallManipulator : AbstractElementManipulator<Jinja2FunctionCall>() {
  @Throws(IncorrectOperationException::class)
  override fun handleContentChange(call: Jinja2FunctionCall, range: TextRange, newContent: String): Jinja2FunctionCall {
    val argument = PsiTreeUtil.findChildOfType(call, Jinja2StringLiteral::class.java) ?: return call
    val newArgumentValue = "\"" + FileUtilRt.getNameWithoutExtension(newContent) + "\""
    val newArgument = Jinja2TemplateElementGenerator.getInstance(call.project).createStringLiteral(newArgumentValue)
    argument.replace(newArgument)
    return call
  }
}
