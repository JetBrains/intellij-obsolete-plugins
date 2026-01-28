// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.hints

import com.intellij.microservices.jvm.config.ConfigKeyPathReference
import com.intellij.microservices.jvm.config.MetaConfigKey
import com.intellij.microservices.jvm.config.hints.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.paths.GlobalPathReferenceProvider
import com.intellij.openapi.paths.WebReference.isWebReferenceWorthy
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.beanProperties.BeanPropertyElement
import com.intellij.psi.util.PsiTypesUtil
import com.intellij.util.ArrayUtil
import com.intellij.util.ProcessingContext

@Service(Service.Level.APP)
class HelidonHintReferencesProvider {
  companion object {
    fun getInstance(): HelidonHintReferencesProvider = ApplicationManager.getApplication().service()
  }

  private val myByTypeProviders: Map<String, HintReferenceProvider> = mapOf(
    CommonClassNames.JAVA_LANG_BOOLEAN to BooleanHintReferenceProvider(),
    CommonClassNames.JAVA_LANG_INTEGER to IntegerHintReferenceProvider(),
    CommonClassNames.JAVA_LANG_LONG to LongHintReferenceProvider(),
  )

  fun getValueReferences(key: MetaConfigKey,
                         keyPsiElement: PsiElement?,
                         valuePsiElement: PsiElement,
                         valueTextRanges: List<TextRange>,
                         context: ProcessingContext): Array<PsiReference> {
    // generic by type
    val byTypeProvider = getByTypeProvider(key.effectiveValueElementType)
    if (byTypeProvider != null) {
      return byTypeProvider.getReferences(valuePsiElement, valueTextRanges, context)
    }

    // value provider if key ends with POJO property path
    if (keyPsiElement == null) return PsiReference.EMPTY_ARRAY

    if (!MetaConfigKey.MAP_OR_INDEXED_WITHOUT_KEY_HINTS_CONDITION.value(key)) {
      return getEraseOtherReferences(valuePsiElement)
    }

    val pojoReferenceProvider = getPojoValueProvider(keyPsiElement)
    return if (pojoReferenceProvider != null) {
      pojoReferenceProvider.getReferences(valuePsiElement, valueTextRanges, context)
    }
    else {
      getEraseOtherReferences(valuePsiElement)
    }
  }

  // override any other references (e.g. PropertiesReferenceContributor)
  private fun getEraseOtherReferences(valuePsiElement: PsiElement): Array<PsiReference> {
    if (isWebReferenceWorthy(valuePsiElement) && valuePsiElement.textContains(':')) {
      val valueText = ElementManipulators.getValueText(valuePsiElement)
      if (GlobalPathReferenceProvider.isWebReferenceUrl(valueText)) {
        // do not override implicit WebReferences with LOWER priority
        return PsiReference.EMPTY_ARRAY
      }
    }
    return arrayOf(PsiReferenceBase.createSelfReference(valuePsiElement, valuePsiElement))
  }

  private fun getByTypeProvider(valueType: PsiType?): HintReferenceProvider? {
    if (valueType == null) return null

    if (valueType is PsiPrimitiveType) return myByTypeProviders[valueType.boxedTypeName]

    val typeClass = PsiTypesUtil.getPsiClass(valueType) ?: return null
    if (typeClass.isEnum) return EnumHintReferenceProvider(typeClass)

    val typeFqn = typeClass.qualifiedName ?: return null
    // 'Class<? extends X>'
    if (CommonClassNames.JAVA_LANG_CLASS == typeFqn) {
      val parameters = (valueType as PsiClassType).parameters
      if (parameters.size != 1) {
        return ClassHintReferenceProvider(null, false, true) // plain 'Class'
      }
      val typeParameter = parameters[0] as? PsiWildcardType ?: return null
      return ClassHintReferenceProvider(typeParameter.extendsBound.canonicalText, false, true)
    }
    return myByTypeProviders[typeFqn]
  }

  private fun getPojoValueProvider(keyPsiElement: PsiElement): HintReferenceProvider? {
    val lastReference = ArrayUtil.getLastElement(keyPsiElement.references) as? ConfigKeyPathReference ?: return null
    if (lastReference.pathType != ConfigKeyPathReference.PathType.BEAN_PROPERTY) return null

    val resolve = lastReference.resolve() as? BeanPropertyElement ?: return null
    return getByTypeProvider(resolve.propertyType!!)
  }
}