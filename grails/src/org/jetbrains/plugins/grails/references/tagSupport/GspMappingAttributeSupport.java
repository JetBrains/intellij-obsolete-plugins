// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.tagSupport;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.references.common.GspTagWrapper;
import org.jetbrains.plugins.grails.references.urlMappings.UrlMappingUtil;

import java.util.Map;

public class GspMappingAttributeSupport extends TagAttributeReferenceProvider {

  protected GspMappingAttributeSupport() {
    super("mapping", "g", null);
  }

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                         @NotNull String text,
                                                         int offset,
                                                         @NotNull GspTagWrapper gspTagWrapper) {
    final Module module = ModuleUtilCore.findModuleForPsiElement(element);
    if (module == null) return PsiReference.EMPTY_ARRAY;

    PsiReference ref = new PsiPolyVariantReferenceBase<>(element, false) {

      @Override
      public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
        Map<String, UrlMappingUtil.NamedUrlMapping> map = UrlMappingUtil.getNamedUrlMappings(module);
        UrlMappingUtil.NamedUrlMapping mapping = map.get(getValue());

        if (mapping != null) {
          return new ResolveResult[]{new PsiElementResolveResult(mapping.getElement())};
        }

        return ResolveResult.EMPTY_ARRAY;
      }

      @Override
      public Object @NotNull [] getVariants() {
        Map<String, UrlMappingUtil.NamedUrlMapping> map = UrlMappingUtil.getNamedUrlMappings(module);
        return map.keySet().toArray();
      }
    };

    return new PsiReference[]{ref};
  }

}
