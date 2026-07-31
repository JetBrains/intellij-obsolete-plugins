package com.intellij.gwt.uiBinder.references;

import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.Nullable;

public interface UiXmlElementReference extends PsiReference {
  @Nullable
  PsiType resolveVariableType();
}
