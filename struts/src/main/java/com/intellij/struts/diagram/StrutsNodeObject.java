/*
 * Copyright 2000-2006 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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