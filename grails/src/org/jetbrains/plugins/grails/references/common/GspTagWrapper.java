// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.common;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface GspTagWrapper {

  @NotNull
  String getTagName();

  boolean hasAttribute(@NotNull String name);

  @Nullable
  PsiElement getAttributeValue(@NotNull String name);

  @Nullable
  PsiType getAttributeValueType(@NotNull String name);

  @Nullable
  String getAttributeText(@NotNull PsiElement attributeValue);

  List<String> getAttributeNames();

}
