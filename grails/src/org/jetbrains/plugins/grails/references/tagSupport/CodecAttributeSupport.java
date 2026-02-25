// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.tagSupport;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.xml.XmlAttributeValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.references.common.CodecReference;
import org.jetbrains.plugins.grails.references.common.GspTagWrapper;

public class CodecAttributeSupport extends TagAttributeReferenceProvider {

  protected CodecAttributeSupport(@NotNull String attributeName, String @Nullable ... tagNames) {
    super(attributeName, "g", tagNames);
  }

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                         @NotNull String text,
                                                         int offset,
                                                         @NotNull GspTagWrapper gspTagWrapper) {
    return new PsiReference[]{new CodecReference(element, element instanceof XmlAttributeValue)};
  }
}
