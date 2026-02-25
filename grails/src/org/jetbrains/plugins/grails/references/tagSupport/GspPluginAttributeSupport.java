// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.tagSupport;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.references.common.GspTagWrapper;
import org.jetbrains.plugins.grails.references.common.PluginReference;

public class GspPluginAttributeSupport extends TagAttributeReferenceProvider {

  public GspPluginAttributeSupport(String namespace) {
    super("plugin", namespace, null);
  }

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                         @NotNull String text,
                                                         int offset,
                                                         @NotNull GspTagWrapper gspTagWrapper) {
    final Module module = ModuleUtilCore.findModuleForPsiElement(element);
    if (module == null) return PsiReference.EMPTY_ARRAY;

    return new PsiReference[]{new PluginReference(module, element, false)};
  }
}
