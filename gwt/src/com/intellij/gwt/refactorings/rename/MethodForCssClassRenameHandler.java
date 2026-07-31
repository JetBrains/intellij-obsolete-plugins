package com.intellij.gwt.refactorings.rename;

import com.intellij.gwt.clientBundle.ClientBundleUtil;
import com.intellij.gwt.clientBundle.css.GwtCssDeclarationsManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.css.StylesheetFile;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class MethodForCssClassRenameHandler extends GwtAssociatedElementRenameHandler<PsiMethod> {
  public MethodForCssClassRenameHandler() {
    super(PsiMethod.class);
  }

  @Override
  protected @NotNull Collection<? extends PsiElement> findAssociatedElements(@NotNull PsiMethod baseElement) {
    final PsiClass psiClass = baseElement.getContainingClass();
    if (psiClass == null) return Collections.emptyList();

    final Set<StylesheetFile> files = ClientBundleUtil.getStylesheetFiles(psiClass, true, true);
    if (files.isEmpty()) return Collections.emptyList();

    final String name = baseElement.getName();
    final Collection<PsiElement> result = new SmartList<>();
    for (StylesheetFile file : files) {
      result.addAll(GwtCssDeclarationsManager.findDeclarations(file, name));
    }
    return result;
  }
}
