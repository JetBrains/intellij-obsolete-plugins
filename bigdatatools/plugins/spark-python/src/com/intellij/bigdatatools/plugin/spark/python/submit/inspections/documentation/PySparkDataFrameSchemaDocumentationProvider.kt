package com.intellij.bigdatatools.plugin.spark.python.submit.inspections.documentation

import com.intellij.bigdatatools.plugin.spark.python.submit.inspections.resolver.PySparkDataFrameResolver
import com.intellij.psi.PsiElement
import com.jetbrains.python.documentation.PythonDocumentationQuickInfoProvider
import com.jetbrains.python.psi.PyTypedElement
import com.jetbrains.python.psi.types.TypeEvalContext
import org.jetbrains.annotations.Nls

class PySparkDataFrameSchemaDocumentationProvider : PythonDocumentationQuickInfoProvider {
  override fun getHoverAdditionalQuickInfo(context: TypeEvalContext,
                                           originalElement: PsiElement?): @Nls String? {
    if (originalElement !is PyTypedElement)
      return null

    val schema = PySparkDataFrameResolver(context).resolve(originalElement) ?: return null

    return schema.toPresentable()
  }
}
