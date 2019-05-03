/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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