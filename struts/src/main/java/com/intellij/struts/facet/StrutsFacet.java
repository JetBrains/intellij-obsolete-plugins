/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.facet;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetType;
import com.intellij.javaee.web.WebUtil;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author nik
 */
public class StrutsFacet extends Facet<StrutsFacetConfiguration> {
  public StrutsFacet(@NotNull final FacetType facetType,
                     @NotNull final Module module,
                     final String name,
                     @NotNull final StrutsFacetConfiguration configuration,
                     @NotNull final Facet underlyingFacet) {
    super(facetType, module, name, configuration, underlyingFacet);
  }

  @Nullable
  public static StrutsFacet getInstance(@NotNull final WebFacet webFacet) {
    Module module = webFacet.getModule();
    if (module.isDisposed()) return null;
    
    return FacetManager.getInstance(module).getFacetByType(webFacet, StrutsFacetType.ID);
  }

  /**
   * Checks whether the Struts-facet is present for the Web facet containing the given PsiElement.
   *
   * @param psiElement element to get the containing web facet.
   * @return true if yes.
   */
  public static boolean isPresentForContainingWebFacet(@NotNull final PsiElement psiElement) {
    final WebFacet webFacet = WebUtil.getWebFacet(psiElement);
    return webFacet != null && StrutsFacet.getInstance(webFacet) != null;
  }

  public WebFacet getWebFacet() {
    return (WebFacet)getUnderlyingFacet();
  }
}
