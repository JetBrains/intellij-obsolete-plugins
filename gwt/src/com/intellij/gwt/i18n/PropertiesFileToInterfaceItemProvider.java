package com.intellij.gwt.i18n;

import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.psi.Property;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.navigation.GotoRelatedProvider;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public final class PropertiesFileToInterfaceItemProvider extends GotoRelatedProvider {
  @Override
  public @NotNull List<? extends GotoRelatedItem> getItems(@NotNull PsiElement context) {
    PsiFile file = context.getContainingFile();
    if (file instanceof PropertiesFile) {
      GwtI18nManager manager = GwtI18nManager.getInstance(context.getProject());
      IProperty property = PsiTreeUtil.getParentOfType(context, Property.class, false);
      if (property != null) {
        PsiMethod method = manager.getMethod(property);
        if (method != null) {
          return Collections.singletonList(new GotoRelatedItem(method));
        }
      }

      PsiClass anInterface = manager.getPropertiesInterface((PropertiesFile)file);
      if (anInterface != null) {
        return Collections.singletonList(new GotoRelatedItem(anInterface));
      }
    }
    return Collections.emptyList();
  }
}
