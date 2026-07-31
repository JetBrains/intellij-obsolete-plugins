package com.intellij.gwt.codeInsight.navigation;

import com.intellij.navigation.GotoRelatedItem;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class GutterIconMultipleNavigationHandler<T extends PsiElement> {
  public abstract @NotNull @NlsContexts.PopupTitle String getPopupChooserTitle(T source, List<? extends GotoRelatedItem> targets);

  public abstract @Nullable String getGutterTooltip(T source, List<? extends GotoRelatedItem> targets);
}
