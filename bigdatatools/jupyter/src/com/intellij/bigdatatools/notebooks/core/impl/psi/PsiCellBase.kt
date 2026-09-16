// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.bigdatatools.notebooks.core.impl.psi

import com.intellij.bigdatatools.notebooks.core.api.psi.PsiSource
import com.intellij.psi.PsiElement

interface PsiCellBase : PsiElement {
  val source: PsiSource
}