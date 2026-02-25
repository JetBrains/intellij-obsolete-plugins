// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.common;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface GspAttributeWrapper {

  @NotNull
  GspTagWrapper getTag();

  @Nullable
  String getName();

  @Nullable
  PsiElement getValue();

}
