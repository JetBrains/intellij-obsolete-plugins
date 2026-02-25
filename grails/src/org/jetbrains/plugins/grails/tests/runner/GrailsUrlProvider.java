// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.grails.tests.runner;

import com.intellij.execution.Location;
import com.intellij.execution.PsiLocation;
import com.intellij.execution.testframework.sm.runner.SMTestLocator;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Maxim.Medvedev
 */
public class GrailsUrlProvider implements SMTestLocator {
  private static final String PROTOCOL_ID = "grails";
  private static final String METHOD_PREF = "methodName";
  private static final String CLASS_PREF = "className";

  public static final GrailsUrlProvider INSTANCE = new GrailsUrlProvider();

  @Override
  public @NotNull List<Location> getLocation(@NotNull String protocol, @NotNull String path, @NotNull Project project, @NotNull GlobalSearchScope scope) {
    if (!PROTOCOL_ID.equals(protocol)) return Collections.emptyList();

    final String className = extractFullClassName(path);
    if (className == null) return Collections.emptyList();
    final PsiClass testClass = JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.allScope(project));
    if (testClass == null) return Collections.emptyList();

    final String methodName = extractMethodName(path);
    if (methodName == null) {
      return Collections.singletonList(new PsiLocation<>(project, testClass));
    }

    final PsiMethod[] methods = testClass.findMethodsByName(methodName, false);
    final List<Location> list = new ArrayList<>(methods.length);
    for (PsiMethod method : methods) {
      list.add(new PsiLocation<>(project, method));
    }
    return list;
  }

  private static @Nullable String extractFullClassName(String locationData) {
    final int i = locationData.indexOf("::");
    final String pref = locationData.substring(0, i);
    final String qualifiedName = locationData.substring(i + 2);
    if (METHOD_PREF.equals(pref)) {
      final int dot = qualifiedName.lastIndexOf('.');
      return qualifiedName.substring(0, dot);
    }
    else if (CLASS_PREF.equals(pref)) {
      return qualifiedName;
    }
    return null;
  }

  private static @Nullable String extractMethodName(String locationData) {
    final int i = locationData.indexOf("::");
    final String pref = locationData.substring(0, i);
    final String qualifiedName = locationData.substring(i + 2);
    if (METHOD_PREF.equals(pref)) {
      final int dot = qualifiedName.lastIndexOf('.');
      return qualifiedName.substring(dot+1);
    }
    return null;
  }
}
