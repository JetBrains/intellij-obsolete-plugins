// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.lexer;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.plugins.grails.lang.gsp.GspLanguage;

public class IGspElementType extends IElementType {
  public IGspElementType(String debugName) {
    super(debugName, GspLanguage.INSTANCE);
  }
}