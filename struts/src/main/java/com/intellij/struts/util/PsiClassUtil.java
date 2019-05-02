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

package com.intellij.struts.util;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiManager;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.search.ProjectScope;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utilities for working with {@link PsiClass}.
 *
 * @author Dmitry Avdeev
 */
public class PsiClassUtil {

  private PsiClassUtil() {
  }

  /**
   * Locate the class with the given name in project scope.
   *
   * @param className Name of the class.
   * @param project   Project.
   * @return null if class could not be found in project scope.
   */
  @Nullable
  public static PsiClass findClassInProjectScope(@NonNls @NotNull final String className, @NotNull final Project project) {
    return JavaPsiFacade.getInstance(project).findClass(className, ProjectScope.getAllScope(project));
  }

  /**
   * Checks if the given class is the same as / inheritor of the given super-class.
   *
   * @param clazz      Name of the class.
   * @param superClass Name of the super class.
   * @return false if not or super class could not be resolved in project scope.
   */
  public static boolean isSuper(@NonNls @NotNull final PsiClass clazz, @NonNls @NotNull final String superClass) {
    final PsiClass superClazz = findClassInProjectScope(superClass, clazz.getProject());
    return superClazz != null && (clazz.equals(superClazz) || clazz.isInheritor(superClazz, true));
  }

}