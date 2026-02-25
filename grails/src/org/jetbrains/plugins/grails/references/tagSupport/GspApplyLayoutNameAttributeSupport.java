// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.tagSupport;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.references.common.GrailsLayoutFileReferenceSet;
import org.jetbrains.plugins.grails.references.common.GspTagWrapper;

public class GspApplyLayoutNameAttributeSupport extends TagAttributeReferenceProvider {
  protected GspApplyLayoutNameAttributeSupport() {
    super("name", "g", new String[]{"applyLayout"});
  }

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                         @NotNull String text,
                                                         int offset,
                                                         @NotNull GspTagWrapper gspTagWrapper) {

    return GrailsLayoutFileReferenceSet.createReferences(element);
  }
}
