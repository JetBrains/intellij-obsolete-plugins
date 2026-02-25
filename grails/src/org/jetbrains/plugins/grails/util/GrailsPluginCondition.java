// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.util;

import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.config.GrailsStructure;

public class GrailsPluginCondition implements Condition<PsiElement> {

  private final String myPluginName;

  public GrailsPluginCondition(@NotNull String pluginName) {
    myPluginName = pluginName;
  }

  @Override
  public boolean value(PsiElement element) {
    GrailsStructure structure = GrailsStructure.getInstance(element);
    if (structure == null) return false;

    return structure.isPluginInstalled(myPluginName);
  }
}
