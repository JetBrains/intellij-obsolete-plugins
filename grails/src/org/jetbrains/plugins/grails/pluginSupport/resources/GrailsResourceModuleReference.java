// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.pluginSupport.resources;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;

public class GrailsResourceModuleReference extends PsiReferenceBase<PsiElement> {

  public Map<String, PsiMethod> myResources;

  public GrailsResourceModuleReference(PsiElement element, TextRange range, boolean soft) {
    super(element, range, soft);
  }

  public GrailsResourceModuleReference(PsiElement element, boolean soft) {
    super(element, soft);
  }

  private @NotNull Map<String, PsiMethod> getResources() {
    Map<String, PsiMethod> res = myResources;
    if (res == null) {
      Module module = ModuleUtilCore.findModuleForPsiElement(getElement());
      if (module == null) {
        res = Collections.emptyMap();
      }
      else {
        res = GrailsResourcesUtil.getResources(module);
      }

      myResources = res;
    }

    return res;
  }

  @Override
  public PsiElement resolve() {
    String value = getValue();
    if (value.isEmpty()) return null;

    return getResources().get(getValue());
  }

  @Override
  public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
    TextRange rangeBefore = getRangeInElement();
    PsiElement result = super.handleElementRename(newElementName);
    //todo move this to PsiReferenceBase
    setRangeInElement(TextRange.from(rangeBefore.getStartOffset(), newElementName.length()));
    return result;
  }

  @Override
  public Object @NotNull [] getVariants() {
    return getResources().keySet().toArray();
  }
}
