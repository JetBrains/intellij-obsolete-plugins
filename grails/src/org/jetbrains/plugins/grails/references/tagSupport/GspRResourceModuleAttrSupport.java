// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.tagSupport;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.pluginSupport.resources.GrailsResourceModuleReference;
import org.jetbrains.plugins.grails.references.common.GspTagWrapper;

public class GspRResourceModuleAttrSupport extends TagAttributeReferenceProvider {
  protected GspRResourceModuleAttrSupport(String attrName, String... tagNames) {
    super(attrName, "r", tagNames);
  }

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                         @NotNull String text,
                                                         int offset,
                                                         @NotNull GspTagWrapper gspTagWrapper) {
    return new PsiReference[]{new GrailsResourceModuleReference(element, false)};
  }
}
