package com.intellij.gwt.refactorings.rename;

import com.intellij.gwt.clientBundle.ClientBundleUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.css.CssClass;
import com.intellij.psi.css.StylesheetFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

class CssClassRenameHandler extends GwtAssociatedElementRenameHandler<CssClass> {
  CssClassRenameHandler() {
    super(CssClass.class);
  }

  @Override
  public @NotNull Collection<? extends PsiElement> findAssociatedElements(@NotNull CssClass baseElement) {
    final PsiFile file = baseElement.getContainingFile();
    if (!(file instanceof StylesheetFile)) return Collections.emptyList();

    final Set<PsiClass> classes = ClientBundleUtil.getCssInterfaces((StylesheetFile)file);
    if (classes.isEmpty()) return Collections.emptyList();

    final List<PsiElement> result = new ArrayList<>();
    for (PsiClass psiClass : classes) {
      Collections.addAll(result, psiClass.findMethodsByName(baseElement.getName(), false));
    }
    return result;
  }
}
