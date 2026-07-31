package com.intellij.gwt.clientBundle;

import com.intellij.navigation.GotoRelatedItem;
import com.intellij.navigation.GotoRelatedProvider;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.css.StylesheetFile;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class CssFileToJavaClassItemProvider extends GotoRelatedProvider {
  @Override
  public @NotNull List<? extends GotoRelatedItem> getItems(@NotNull PsiElement context) {
    PsiFile file = context.getContainingFile();
    if (file instanceof StylesheetFile) {
      Set<PsiClass> cssInterfaces = ClientBundleUtil.getCssInterfaces((StylesheetFile)file);
      return GotoRelatedItem.createItems(cssInterfaces);
    }
    return Collections.emptyList();
  }
}
