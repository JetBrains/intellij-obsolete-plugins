// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.psi.groovy.impl;

import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.templateLanguages.OuterLanguageElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.api.GspOuterGroovyElement;

public class GspOuterGroovyElementImpl extends LeafPsiElement implements OuterLanguageElement, GspOuterGroovyElement {

  public GspOuterGroovyElementImpl(@NotNull IElementType type, CharSequence text) {
    super(type, text);
  }

  @Override
  public String toString() {
    return "Outer: " + getElementType();
  }

}
