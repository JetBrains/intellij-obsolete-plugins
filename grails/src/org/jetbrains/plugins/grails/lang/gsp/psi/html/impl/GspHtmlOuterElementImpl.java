// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.psi.html.impl;

import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.psi.html.api.GspHtmlOuterElement;

public class GspHtmlOuterElementImpl extends LeafPsiElement implements GspHtmlOuterElement {

  public GspHtmlOuterElementImpl(@NotNull IElementType type, CharSequence text) {
    super(type, text);
  }

  @Override
  public String toString() {
    return "Outer: " + getElementType();
  }

}
