package com.intellij.seam.model;

import com.intellij.jam.model.common.CommonModelElement;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.psi.PsiType;
import com.intellij.util.xml.NameValue;
import com.intellij.util.xml.PrimaryKey;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;

public interface CommonSeamComponent extends CommonModelElement {
  @NotNull
  @NameValue
  @NlsSafe
  String getComponentName();

  @Nullable
  SeamComponentScope getComponentScope();

  @Nullable
  @PrimaryKey
  PsiType getComponentType();
}
