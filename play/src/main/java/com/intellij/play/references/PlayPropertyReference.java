package com.intellij.play.references;

import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.play.utils.PlayPathUtils;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public final class PlayPropertyReference extends PsiReferenceBase<PsiElement> {
  private final String myKey;

  public PlayPropertyReference(@NotNull String value, @NotNull PsiElement element) {
    super(element);
    myKey = value;
  }

  @Override
  public PsiElement resolve() {
    final Module module = ModuleUtilCore.findModuleForPsiElement(getElement());
    if (module == null) {
      return null;
    }

    final Set<com.intellij.psi.PsiDirectory> directories = PlayPathUtils.getConfigDirectories(module);
    for (com.intellij.psi.PsiDirectory directory : directories) {
      final PsiFile[] files = directory.getFiles();
      for (PsiFile file : files) {
        if (file instanceof PropertiesFile && file.getName().startsWith("message")) {
          PropertiesFile propertiesFile = (PropertiesFile) file;
          IProperty property = propertiesFile.findPropertyByKey(myKey);
          if (property != null) {
            return property.getPsiElement();
          }
        }
      }
    }

    return null;
  }

  @Override
  public @NotNull Object[] getVariants() {
    return PsiReference.EMPTY_ARRAY;
  }

  @NotNull
  public ResolveResult[] multiResolve(boolean incompleteCode) {
    PsiElement resolved = resolve();
    if (resolved != null) {
      return new ResolveResult[]{new PsiElementResolveResult(resolved)};
    }
    return ResolveResult.EMPTY_ARRAY;
  }

  @NotNull
  public String getUnresolvedMessagePattern() {
    return "Cannot resolve property '" + myKey + "'";
  }
}
