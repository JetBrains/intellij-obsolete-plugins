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