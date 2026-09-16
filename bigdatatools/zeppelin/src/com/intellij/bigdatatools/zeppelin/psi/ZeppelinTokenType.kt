// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.bigdatatools.zeppelin.psi

import com.intellij.psi.tree.IElementType
import com.intellij.bigdatatools.zeppelin.language.ZeppelinLanguage

class ZeppelinTokenType(debugName: String) : IElementType(debugName, ZeppelinLanguage) {
  override fun toString() = "ZeppelinTokenType.${super.toString()}"
}