// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config

import com.intellij.microservices.jvm.config.AbstractMetaConfigKey
import com.intellij.microservices.jvm.config.MetaConfigKey.*
import com.intellij.microservices.jvm.config.MetaConfigKeyManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType

class HelidonMetaConfigKey(
  myName: String,
  declaration: PsiElement,
  private val myDeclarationResolveResult: DeclarationResolveResult,
  type: PsiType?,
  private val myDescriptionText: DescriptionText,
  private val myDeprecation: Deprecation,
  private val myDefaultValue: String?,
  accessType: AccessType,
  val subKeys: List<HelidonMetaConfigKey> = emptyList(),
) : AbstractMetaConfigKey(myName, declaration, type, accessType) {
  override fun getManager(): MetaConfigKeyManager = HelidonMetaConfigKeyManager.getInstance()

  override fun getDescriptionText(): DescriptionText = myDescriptionText

  override fun getDeclarationResolveResult(): DeclarationResolveResult = myDeclarationResolveResult

  override fun getDeprecation(): Deprecation = myDeprecation

  override fun getDefaultValue(): String? = myDefaultValue

  override fun toString(): String {
    return "HelidonMetaConfigKey{name='$name', descriptionText='$descriptionText', defaultValue='$defaultValue', configKeyType=$keyType}"
  }
}