package com.intellij.gwt.refactorings.rename;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

public abstract class GwtAssociatedElementRenameHandler<T extends PsiElement> {
  private final Class<T> myElementClass;

  protected GwtAssociatedElementRenameHandler(Class<T> elementClass) {
    myElementClass = elementClass;
  }

  public String getNewAssociatedElementName(String newBaseElementName) {
    return newBaseElementName;
  }

  public final @NotNull Collection<? extends PsiElement> getAssociatedElements(@NotNull PsiElement baseElement) {
    if (myElementClass.isInstance(baseElement)) {
      //noinspection unchecked
      return findAssociatedElements((T)baseElement);
    }
    return Collections.emptyList();
  }

  protected abstract @NotNull Collection<? extends PsiElement> findAssociatedElements(@NotNull T baseElement);
}
