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

package com.intellij.struts;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.model.DomModel;
import com.intellij.util.xml.model.impl.DomModelFactory;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Dmitry Avdeev
 */
public abstract class WebDomFactory<T extends DomElement, M extends DomModel<T>> extends DomModelFactory<T, M, PsiElement> {

  private final Object[] myDependencies;

  protected WebDomFactory(final Class<T> aClass, final Project project, final @NonNls String name) {
    super(aClass, project, name);
    myDependencies = new Object[] {
      PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT,
      ProjectRootManager.getInstance(project)
    };
  }

  @Override
  @NotNull
  public Object[] computeDependencies(@Nullable M model, @Nullable final Module module) {
    return myDependencies;
  }
}
