package com.intellij.gwt.uiBinder;

import com.intellij.gwt.uiBinder.mapping.UiBinderMappingService;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.navigation.GotoRelatedProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public final class UiXmlFileToJavaClassItemProvider extends GotoRelatedProvider {
  @Override
  public @NotNull List<? extends GotoRelatedItem> getItems(@NotNull PsiElement context) {
    PsiFile file = context.getContainingFile();
    if (file instanceof XmlFile && UiBinderUtil.isUiXmlFile((XmlFile)file)) {
      return GotoRelatedItem.createItems(UiBinderMappingService.getBoundClassesForFile(file));
    }
    return Collections.emptyList();
  }
}
