// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config

import com.intellij.ide.presentation.Presentation
import com.intellij.microservices.jvm.config.ConfigKeyDeclarationPsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType

private const val CONFIGURATION_KEY: String = "Helidon Configuration Key"

@Presentation(typeName = CONFIGURATION_KEY)
internal class HelidonConfigKeyDeclarationPsiElement(private val myConfigKey: String,
                                                     private val myConfigType: String,
                                                     private val myNavigationTarget: PsiElement,
                                                     private val myNavigationParent: PsiElement,
                                                     private val myLibraryName: String?,
                                                     type: PsiType?) : ConfigKeyDeclarationPsiElement(type) {

  override fun getParent(): PsiElement = myNavigationParent

  override fun getNavigationElement(): PsiElement = myNavigationTarget

  override fun getPresentableText(): String = myConfigType

  override fun getName(): String = myConfigKey

  override fun getLocationString(): String? = myLibraryName
}