/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.diagram;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author Dmitry Avdeev
 */
public class StrutsEdgeObject implements StrutsObject {

  @NotNull
  private final String myName;
  private final String myId;

  public StrutsEdgeObject(@NotNull final String parent, @NotNull final String name) {
    myName = name;
    myId = parent + name;
  }

  @Override
  @Nullable
  public Icon getIcon() {
    return null;
  }

  @Override
  @NotNull
  public String getName() {
    return myName;
  }

  @Override
  @Nullable
  public PsiElement getPsiElement() {
    return null;
  }

  public int hashCode() {
    return myId.hashCode();
  }

  public boolean equals(final Object obj) {
    return obj instanceof StrutsEdgeObject && ((StrutsEdgeObject) obj).myId.equals(myId);
  }

}