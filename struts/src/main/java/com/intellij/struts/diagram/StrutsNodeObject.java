/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.diagram;

import com.intellij.openapi.paths.PathReference;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author Dmitry Avdeev
 */
public class StrutsNodeObject implements StrutsObject {

  private final Icon myIcon;
  @NotNull
  private final String myName;
  private final PsiElement myElement;

  public StrutsNodeObject(final PathReference webPath) {
    this(webPath.getPath(), webPath.getIcon(), webPath.resolve());
  }

  public StrutsNodeObject(@NotNull final String name, final Icon icon, final PsiElement element) {
    myElement = element;
    myIcon = icon;
    myName = name;
  }

  public StrutsNodeObject(@NotNull final String name) {
    this(name, null, null);
  }

  @Override
  public Icon getIcon() {
    return myIcon;
  }

  @Override
  @NotNull
  public String getName() {
    return myName;
  }

  @Override
  public PsiElement getPsiElement() {
    return myElement;
  }

  public int hashCode() {
    return myName.hashCode();
  }

  public boolean equals(final Object obj) {
    return obj instanceof StrutsNodeObject && ((StrutsNodeObject) obj).myName.equals(myName);
  }

  @Override
  public String toString() {
    return "StrutsNodeObject{" +
           "myIcon=" + myIcon +
           ", myName='" + myName + '\'' +
           ", myElement=" + myElement +
           '}';
  }
}