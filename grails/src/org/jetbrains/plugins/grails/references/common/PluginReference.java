// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.common;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.config.GrailsFramework;
import org.jetbrains.plugins.grails.util.GrailsUtils;

public class PluginReference extends PsiReferenceBase<PsiElement> {

  private final Module myModule;

  public PluginReference(@NotNull Module module, PsiElement element, boolean soft) {
    super(element, soft);
    myModule = module;
  }

  @Override
  public PsiElement resolve() {
    String text = getValue();
    VirtualFile virtualFile = GrailsFramework.getInstance().findPluginRoot(myModule, text, false);
    if (virtualFile == null) return null;
    return getElement().getManager().findDirectory(virtualFile);
  }

  @Override
  public Object @NotNull [] getVariants() {
    return GrailsUtils.createPluginVariants(myModule, false);
  }

  public static class Provider extends PsiReferenceProvider {
    @Override
    public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
      final Module module = ModuleUtilCore.findModuleForPsiElement(element);
      if (module == null) return PsiReference.EMPTY_ARRAY;

      return new PsiReference[]{new PluginReference(module, element, false)};
    }
  }
}
