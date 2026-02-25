// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.controller;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.util.GrailsArtifact;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrClassDefinition;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.literals.GrLiteralImpl;

import java.util.HashSet;
import java.util.Set;

public final class GrailsNamespaceReference extends PsiReferenceBase<PsiElement> {
  private PsiClass myIgnoredController;

  public GrailsNamespaceReference(PsiElement psiElement, boolean soft) {
    super(psiElement, soft);
  }

  public GrailsNamespaceReference(PsiElement element, TextRange range, boolean soft) {
    super(element, range, soft);
  }

  @Override
  public @Nullable PsiElement resolve() {
    return null;
  }

  public void setIgnoredController(@NotNull PsiClass ignoredController) {
    myIgnoredController = ignoredController;
  }

  @Override
  public Object @NotNull [] getVariants() {
    Module module = ModuleUtilCore.findModuleForPsiElement(getElement());
    if (module == null) return ResolveResult.EMPTY_ARRAY;

    MultiMap<String,GrClassDefinition> instances = GrailsArtifact.CONTROLLER.getInstances(module);

    Set<String> res = new HashSet<>();

    for (GrClassDefinition controllerDefinition : instances.values()) {
      if (controllerDefinition == myIgnoredController) continue;

      PsiField namespaceField = controllerDefinition.findCodeFieldByName("namespace", true);
      if (namespaceField instanceof GrField && namespaceField.hasModifierProperty(PsiModifier.STATIC)) {
        GrExpression initializerGroovy = ((GrField)namespaceField).getInitializerGroovy();
        if (initializerGroovy instanceof GrLiteralImpl) {
          Object value = ((GrLiteralImpl)initializerGroovy).getValue();
          if (value instanceof String str) {
            str = str.trim();

            if (!str.isEmpty()) {
              res.add(str);
            }
          }
        }
      }
    }

    return res.toArray();
  }
}
