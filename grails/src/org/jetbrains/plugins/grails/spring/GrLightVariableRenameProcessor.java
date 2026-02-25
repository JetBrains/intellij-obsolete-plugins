// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.grails.spring;

import com.intellij.pom.PomTarget;
import com.intellij.pom.PomTargetPsiElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.targets.AliasingPsiTarget;
import com.intellij.refactoring.rename.RenameAliasingPomTargetProcessor;
import com.intellij.refactoring.rename.RenamePsiElementProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightVariable;

import java.util.Map;

public final class GrLightVariableRenameProcessor extends RenamePsiElementProcessor {
  @Override
  public boolean canProcessElement(@NotNull PsiElement element) {
    return element instanceof GrLightVariable;
  }

  @Override
  public void prepareRenaming(@NotNull PsiElement element, @NotNull String newName, @NotNull Map<PsiElement, String> allRenames) {
    if (element instanceof GrLightVariable) {
      for (PsiElement target : ((GrLightVariable)element).getDeclarations()) {
      if (target instanceof AliasingPsiTarget) {
        RenameAliasingPomTargetProcessor.prepareAliasingPsiTargetRenaming((AliasingPsiTarget)target, newName, allRenames);
      } else if (target instanceof PomTargetPsiElement) {
        final PomTarget pomTarget = ((PomTargetPsiElement)target).getTarget();
        if (pomTarget instanceof AliasingPsiTarget aliasingPsiTarget) {
          allRenames.put(target, newName);
          RenameAliasingPomTargetProcessor.prepareAliasingPsiTargetRenaming(aliasingPsiTarget, newName, allRenames);
        }
      }
    }}
  }
}
