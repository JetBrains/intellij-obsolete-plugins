package com.intellij.gwt.module;

import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.sdk.GwtSdk;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.AllowedApiFilterExtension;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class GwtAllowedApiFilter extends AllowedApiFilterExtension {
  @Override
  public boolean isClassForbidden(@NotNull String fqn, @NotNull PsiElement place) {
    final PsiFile containingFile = place.getContainingFile();
    if (containingFile == null) return false;

    final VirtualFile virtualFile = containingFile.getVirtualFile();
    if (virtualFile == null) return false;

    final GwtFacet facet = GwtFacet.findFacetByPsiElement(place);
    if (facet == null) return false;

    final GwtModulesManager modulesManager = GwtModulesManager.getInstance(facet.getModule().getProject());
    List<GwtModule> gwtModules = modulesManager.findGwtModulesByClientSourceFile(virtualFile);
    if (gwtModules.isEmpty()) return false;

    final GwtSdk sdk = facet.getConfiguration().getSdk();
    return sdk.isValid() && !sdk.containsJreEmulationClass(gwtModules, fqn);
  }
}
