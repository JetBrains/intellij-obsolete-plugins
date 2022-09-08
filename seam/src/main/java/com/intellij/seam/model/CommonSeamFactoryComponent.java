package com.intellij.seam.model;

import com.intellij.jam.model.common.CommonModelElement;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.Nullable;

public interface CommonSeamFactoryComponent extends CommonModelElement {
  @Nullable
  String getFactoryName();

  @Nullable
  SeamComponentScope getFactoryScope();

  @Nullable
  PsiType getFactoryType();
}
