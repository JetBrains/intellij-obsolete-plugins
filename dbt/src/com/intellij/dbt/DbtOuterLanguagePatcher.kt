package com.intellij.dbt

import com.intellij.jinja.template.psi.Jinja2TemplateElementTypes
import com.intellij.psi.templateLanguages.DefaultOuterLanguagePatcher
import com.intellij.psi.templateLanguages.TemplateDataElementType

class DbtOuterLanguagePatcher : TemplateDataElementType.OuterLanguageRangePatcher {
  override fun getTextForOuterLanguageInsertionRange(templateDataElementType: TemplateDataElementType,
                                                     outerElementText: CharSequence): String? {
    if (templateDataElementType == Jinja2TemplateElementTypes.TEMPLATE_DATA) {
      return DefaultOuterLanguagePatcher.OUTER_EXPRESSION_PLACEHOLDER
    }
    return null
  }
}