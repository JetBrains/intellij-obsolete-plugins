// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.controller;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.rename.RenamePsiElementProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.util.GrailsArtifact;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrClassDefinition;

import java.util.Map;

public final class ControllerRenameProcessor extends RenamePsiElementProcessor {
  @Override
  public boolean canProcessElement(@NotNull PsiElement element) {
    if (!(element instanceof GrClassDefinition)) return false;

    return GrailsArtifact.CONTROLLER.isInstance((GrClassDefinition)element);
  }

  @Override
  public void prepareRenaming(@NotNull PsiElement element, @NotNull String newName, @NotNull Map<PsiElement, String> allRenames) {
    String suffix = GrailsArtifact.CONTROLLER.suffix;
    if (!newName.endsWith(suffix) || newName.length() <= suffix.length()) return;

    String newControllerName = GrailsArtifact.CONTROLLER.getArtifactName(newName);

    VirtualFile oldViewsDir = GrailsUtils.getControllerGspDir((PsiClass)element);
    if (oldViewsDir != null) {
      PsiDirectory directory = element.getManager().findDirectory(oldViewsDir);
      if (directory != null) {
        allRenames.put(directory, newControllerName);
      }
    }
  }
}
