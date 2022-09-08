package com.intellij.seam.model.xml.components;

import org.jetbrains.annotations.Nullable;
import com.intellij.psi.PsiType;
import com.intellij.util.xml.DomElement;

public interface BasicProperty extends DomElement {

  @Nullable
  String getPropertyName();

  @Nullable
  PsiType getPropertyType();
}
