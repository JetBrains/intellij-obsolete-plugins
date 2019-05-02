/*
 * Copyright 2000-2006 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
