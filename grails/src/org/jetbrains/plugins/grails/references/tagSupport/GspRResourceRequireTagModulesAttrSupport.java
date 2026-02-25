// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.tagSupport;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.pluginSupport.resources.GrailsResourcesReferenceProvider;
import org.jetbrains.plugins.grails.references.common.GspTagWrapper;

public class GspRResourceRequireTagModulesAttrSupport extends TagAttributeReferenceProvider {
  protected GspRResourceRequireTagModulesAttrSupport() {
    super("modules", "r", new String[]{"require", "use"});
  }

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                         @NotNull String text,
                                                         int offset,
                                                         @NotNull GspTagWrapper gspTagWrapper) {
    return GrailsResourcesReferenceProvider.ResourceModuleReferenceProvider.createManyModuleReferences(element, text, offset);
  }
}
