/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.core;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;

import javax.swing.*;

public interface PsiBeanProperty {

  PsiBeanProperty[] EMPTY_ARRAY = new PsiBeanProperty[0];

  PsiElement[] getPsiElements();

  Icon getIcon();

  String getName();

  String getType();

  boolean hasSetter();

  boolean hasGetter();

  PsiMethod getGetter();
}
