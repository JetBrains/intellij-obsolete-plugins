package com.intellij.gwt.uiBinder.declarations;

import com.intellij.psi.PsiType;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface UiXmlVariableDeclaration {
  @Nullable
  String getVariableName();

  @Nullable
  String getType();

  @Nullable
  PsiType resolveType();

  @NotNull
  XmlTag getTag();
}
