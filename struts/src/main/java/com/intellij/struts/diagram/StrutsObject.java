/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.diagram;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Base interface for web flow diagram objects.
 *
 * @author Dmitry Avdeev
 */
public interface StrutsObject {

  @Nullable
  Icon getIcon();

  @NotNull
  String getName();

  @Nullable
  PsiElement getPsiElement();
}
