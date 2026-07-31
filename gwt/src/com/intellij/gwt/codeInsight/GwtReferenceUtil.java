package com.intellij.gwt.codeInsight;

import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nullable;

public final class GwtReferenceUtil {
  private GwtReferenceUtil() {
  }

  public static @Nullable GwtModule findGwtModule(PsiElement element) {
    return findGwtModule(element, GwtModulesManager.getInstance(element.getProject()));
  }

  public static @Nullable GwtModule findGwtModule(PsiElement element, final GwtModulesManager modulesManager) {
    final PsiFile psiFile = element.getContainingFile();
    if (psiFile == null) return null;

    VirtualFile virtualFile = psiFile.getOriginalFile().getVirtualFile();
    if (virtualFile == null) {
      return null;
    }

    return modulesManager.findGwtModuleByClientSourceFile(virtualFile);
  }
}
