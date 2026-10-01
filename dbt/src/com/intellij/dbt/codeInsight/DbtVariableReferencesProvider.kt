package com.intellij.dbt.codeInsight

import com.intellij.codeInsight.completion.util.ParenthesesInsertHandler
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.dbt.DbtUtils
import com.intellij.dbt.codeInsight.DbtTagLibrary.Companion.dbtParameterizedTags
import com.intellij.dbt.codeInsight.DbtTagLibrary.Companion.dbtUnparameterizedTags
import com.intellij.jinja.template.psi.impl.Jinja2VariableReferenceImpl
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext

class DbtModelReferencesProvider : PsiReferenceProvider() {
  override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
    val module = ModuleUtilCore.findModuleForPsiElement(element.containingFile) ?: return PsiReference.EMPTY_ARRAY
    if (!DbtUtils.isDbtModule(module)) {
      return PsiReference.EMPTY_ARRAY
    }
    if (element is Jinja2VariableReferenceImpl) {
      return arrayOf(DbtVariableReference(element))
    }
    return emptyArray()
  }
}

class DbtVariableReference(element: Jinja2VariableReferenceImpl) : PsiReferenceBase<Jinja2VariableReferenceImpl>(element) {
  override fun getRangeInElement(): TextRange {
    return TextRange(0, element.textLength)
  }

  override fun resolve(): PsiElement? {
    return null
  }

  override fun getVariants(): Array<Any> {
    val result = ArrayList<LookupElement>()

    for (tag in dbtUnparameterizedTags) {
      result.add(LookupElementBuilder.create(tag))
    }

    for (tag in dbtParameterizedTags) {
      result.add(LookupElementBuilder.create(tag).withInsertHandler(ParenthesesInsertHandler.WITH_PARAMETERS))
    }

    return result.toTypedArray()
  }
}